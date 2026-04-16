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

package com.navercorp.pinpoint.web.authorization.controller;

import com.navercorp.pinpoint.web.calendar.service.UserCalendarEventService;
import com.navercorp.pinpoint.web.calendar.vo.GoogleCalendarEvent;
import com.navercorp.pinpoint.web.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/user/calendar")
@Validated
public class UserCalendarController {
    private final UserService userService;
    private final UserCalendarEventService userCalendarEventService;

    public UserCalendarController(UserService userService, UserCalendarEventService userCalendarEventService) {
        this.userService = Objects.requireNonNull(userService, "userService");
        this.userCalendarEventService = Objects.requireNonNull(userCalendarEventService, "userCalendarEventService");
    }

    @GetMapping("/weekly")
    public List<GoogleCalendarEvent> getWeeklyEvents(
            @RequestParam(value = "weekStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        final String userId = getCurrentUserId();
        return userCalendarEventService.getWeeklyEvents(userId, normalizeWeekStart(weekStart));
    }

    @PostMapping("/sync")
    public List<GoogleCalendarEvent> syncWeeklyEvents(
            @RequestParam(value = "weekStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        final String userId = getCurrentUserId();
        try {
            return userCalendarEventService.syncWeeklyEvents(userId, normalizeWeekStart(weekStart));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private String getCurrentUserId() {
        final String userId = userService.getUserIdFromSecurity();
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return userId;
    }

    private LocalDate normalizeWeekStart(LocalDate weekStart) {
        final LocalDate base = weekStart == null ? LocalDate.now(ZoneOffset.UTC) : weekStart;
        return base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
