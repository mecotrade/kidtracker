/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
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
package ru.mecotrade.kidtracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.mecotrade.kidtracker.exception.KidTrackerInvalidOperationException;
import ru.mecotrade.kidtracker.model.User;
import ru.mecotrade.kidtracker.processor.UserProcessor;
import ru.mecotrade.kidtracker.security.UserPrincipal;
import ru.mecotrade.kidtracker.util.ValidationUtils;

@Controller
@Slf4j
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserProcessor userProcessor;

    @PostMapping("/user")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody void create(@RequestBody User user, Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            if (isValid(user)) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                try {
                    userProcessor.addUser(userPrincipal, user);
                } catch (KidTrackerInvalidOperationException ex) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
                }
            } else {
                log.warn("{} can't be created since it is not valid", user);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else {
            log.warn("Unauthorized request for user creation");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isValid(User user) {
        return ValidationUtils.isValidPhone(user.getPhone())
                && user.getCredentials() != null
                && StringUtils.isNotBlank(user.getCredentials().getUsername())
                && StringUtils.isNotBlank(user.getCredentials().getPassword())
                && StringUtils.isNotBlank(user.getName());
    }
}
