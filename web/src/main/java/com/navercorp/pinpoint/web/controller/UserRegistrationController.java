/*
 * Copyright 2014 NAVER Corp.
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
package com.navercorp.pinpoint.web.controller;

import com.navercorp.pinpoint.common.server.response.Response;
import com.navercorp.pinpoint.common.server.response.SimpleResponse;
import com.navercorp.pinpoint.web.service.UserService;
import com.navercorp.pinpoint.web.util.ValueValidator;
import com.navercorp.pinpoint.web.vo.User;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
@RequestMapping("/api-public/user")
@Validated
public class UserRegistrationController {

    private final UserService userService;

    public UserRegistrationController(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    @PostMapping("/register")
    public Response registerUser(@RequestBody User user) {
        user.setNumber(null);

        if (!ValueValidator.validateUser(user)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User information validation failed."
            );
        }

        if (userService.isExistUserId(user.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Unable to complete registration."
            );
        }

        userService.insertUser(user);
        return SimpleResponse.ok();
    }
}
