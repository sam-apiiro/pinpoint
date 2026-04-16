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

package com.navercorp.pinpoint.web.dao.mysql;

import com.navercorp.pinpoint.web.calendar.dao.GoogleCalendarEventDao;
import com.navercorp.pinpoint.web.calendar.vo.GoogleCalendarEvent;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class MysqlGoogleCalendarEventDao implements GoogleCalendarEventDao {
    private static final String NAMESPACE = GoogleCalendarEventDao.class.getName() + ".";
    private final SqlSessionTemplate sqlSessionTemplate;

    public MysqlGoogleCalendarEventDao(@Qualifier("sqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = Objects.requireNonNull(sqlSessionTemplate, "sqlSessionTemplate");
    }

    @Override
    public void deleteEventsByUserAndRange(String userId, long weekStartMillis, long weekEndMillis) {
        final Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("weekStartMillis", weekStartMillis);
        params.put("weekEndMillis", weekEndMillis);
        sqlSessionTemplate.delete(NAMESPACE + "deleteEventsByUserAndRange", params);
    }

    @Override
    public void insertEvents(String userId, List<GoogleCalendarEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        final Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("events", events);
        sqlSessionTemplate.insert(NAMESPACE + "insertEvents", params);
    }

    @Override
    public List<GoogleCalendarEvent> selectEventsByUserAndRange(String userId, long weekStartMillis, long weekEndMillis) {
        final Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("weekStartMillis", weekStartMillis);
        params.put("weekEndMillis", weekEndMillis);
        return sqlSessionTemplate.selectList(NAMESPACE + "selectEventsByUserAndRange", params);
    }
}
