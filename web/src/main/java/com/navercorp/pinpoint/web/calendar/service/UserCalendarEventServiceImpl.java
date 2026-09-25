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

import com.navercorp.pinpoint.web.calendar.dao.GoogleCalendarEventDao;
import com.navercorp.pinpoint.web.calendar.vo.GoogleCalendarEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserCalendarEventServiceImpl implements UserCalendarEventService {
    private final GoogleCalendarEventDao googleCalendarEventDao;
    private final GoogleCalendarClient googleCalendarClient;

    public UserCalendarEventServiceImpl(GoogleCalendarEventDao googleCalendarEventDao, GoogleCalendarClient googleCalendarClient) {
        this.googleCalendarEventDao = Objects.requireNonNull(googleCalendarEventDao, "googleCalendarEventDao");
        this.googleCalendarClient = Objects.requireNonNull(googleCalendarClient, "googleCalendarClient");
    }

    @Override
    public List<GoogleCalendarEvent> syncWeeklyEvents(String userId, LocalDate weekStartDate) {
        final LocalDate alignedWeekStart = alignWeekStart(weekStartDate);
        final long weekStartMillis = toEpochMillis(alignedWeekStart);
        final long weekEndMillis = toEpochMillis(alignedWeekStart.plusDays(7));

        final List<GoogleCalendarEvent> events = googleCalendarClient.fetchWeeklyEvents(userId, weekStartMillis, weekEndMillis);
        googleCalendarEventDao.deleteEventsByUserAndRange(userId, weekStartMillis, weekEndMillis);
        googleCalendarEventDao.insertEvents(userId, events);
        return googleCalendarEventDao.selectEventsByUserAndRange(userId, weekStartMillis, weekEndMillis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoogleCalendarEvent> getWeeklyEvents(String userId, LocalDate weekStartDate) {
        final LocalDate alignedWeekStart = alignWeekStart(weekStartDate);
        final long weekStartMillis = toEpochMillis(alignedWeekStart);
        final long weekEndMillis = toEpochMillis(alignedWeekStart.plusDays(7));
        return googleCalendarEventDao.selectEventsByUserAndRange(userId, weekStartMillis, weekEndMillis);
    }

    private LocalDate alignWeekStart(LocalDate weekStartDate) {
        return weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private long toEpochMillis(LocalDate localDate) {
        return localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
