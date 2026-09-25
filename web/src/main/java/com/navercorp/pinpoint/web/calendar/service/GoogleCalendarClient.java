/*
 * Copyright 2026 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.web.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.pinpoint.web.calendar.vo.GoogleCalendarEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class GoogleCalendarClient {
    private static final int MAX_SUMMARY_LENGTH = 1000;
    private static final int MAX_LINK_LENGTH = 2000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String googleApiBaseUrl;
    private final String calendarId;
    private final String oauthAccessToken;

    public GoogleCalendarClient(RestTemplate restTemplate,
                                ObjectMapper objectMapper,
                                @Value("${pinpoint.calendar.google.base-url:https://www.googleapis.com}") String googleApiBaseUrl,
                                @Value("${pinpoint.calendar.google.calendar-id:primary}") String calendarId,
                                @Value("${pinpoint.calendar.google.oauth-access-token:}") String oauthAccessToken) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.googleApiBaseUrl = Objects.requireNonNull(googleApiBaseUrl, "googleApiBaseUrl");
        this.calendarId = Objects.requireNonNull(calendarId, "calendarId");
        this.oauthAccessToken = Objects.requireNonNull(oauthAccessToken, "oauthAccessToken");
    }

    public List<GoogleCalendarEvent> fetchWeeklyEvents(String userId, long weekStartMillis, long weekEndMillis) {
        if (oauthAccessToken.isBlank()) {
            throw new IllegalStateException("Google Calendar access token is not configured.");
        }

        final String timeMin = Instant.ofEpochMilli(weekStartMillis).toString();
        final String timeMax = Instant.ofEpochMilli(weekEndMillis).toString();
        final URI uri = UriComponentsBuilder.fromHttpUrl(googleApiBaseUrl)
                .pathSegment("calendar", "v3", "calendars", calendarId, "events")
                .queryParam("singleEvents", true)
                .queryParam("orderBy", "startTime")
                .queryParam("maxResults", 2500)
                .queryParam("timeMin", timeMin)
                .queryParam("timeMax", timeMax)
                .build()
                .encode()
                .toUri();

        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(oauthAccessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        final ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        final String body = response.getBody();
        if (body == null || body.isBlank()) {
            return List.of();
        }

        try {
            final JsonNode root = objectMapper.readTree(body);
            final JsonNode items = root.path("items");
            if (!items.isArray()) {
                return List.of();
            }

            final List<GoogleCalendarEvent> events = new ArrayList<>();
            for (JsonNode item : items) {
                final String eventId = item.path("id").asText("");
                if (eventId.isBlank()) {
                    continue;
                }
                final long startTimeMillis = parseEventTime(item.path("start"));
                if (startTimeMillis < weekStartMillis || startTimeMillis >= weekEndMillis) {
                    continue;
                }

                long endTimeMillis = parseEventTime(item.path("end"));
                if (endTimeMillis <= 0L) {
                    endTimeMillis = startTimeMillis;
                }

                final GoogleCalendarEvent event = new GoogleCalendarEvent();
                event.setUserId(userId);
                event.setEventId(eventId);
                event.setSummary(truncate(item.path("summary").asText(""), MAX_SUMMARY_LENGTH));
                event.setHtmlLink(truncate(item.path("htmlLink").asText(""), MAX_LINK_LENGTH));
                event.setStartTimeMillis(startTimeMillis);
                event.setEndTimeMillis(endTimeMillis);
                event.setUpdatedTimeMillis(parseUpdatedTime(item.path("updated").asText("")));
                events.add(event);
            }
            return events;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Google Calendar response.", e);
        }
    }

    private long parseEventTime(JsonNode eventTimeNode) {
        final String dateTime = eventTimeNode.path("dateTime").asText("");
        if (!dateTime.isBlank()) {
            return OffsetDateTime.parse(dateTime).toInstant().toEpochMilli();
        }
        final String date = eventTimeNode.path("date").asText("");
        if (!date.isBlank()) {
            return LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        return -1L;
    }

    private long parseUpdatedTime(String updated) {
        if (updated == null || updated.isBlank()) {
            return 0L;
        }
        try {
            return OffsetDateTime.parse(updated).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
