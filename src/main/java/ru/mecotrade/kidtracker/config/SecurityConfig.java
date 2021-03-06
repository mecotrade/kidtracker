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
package ru.mecotrade.kidtracker.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import ru.mecotrade.kidtracker.security.Roles;
import ru.mecotrade.kidtracker.security.UserDeviceFilter;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${remember.me.token.validity.seconds}")
    private int rememberMeTokenValiditySeconds;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf()
                        .disable()
                .headers()
                        .frameOptions().disable()
                        .and()
                .authorizeRequests()
                        .antMatchers("/h2-console/**").permitAll()
                        .antMatchers("/api/admin/**").hasAnyAuthority(Roles.ADMIN.toString())
                        .anyRequest().hasAnyAuthority(Roles.USER.toString(), Roles.ADMIN.toString())
                        .and()
                .addFilterAfter(new UserDeviceFilter(), FilterSecurityInterceptor.class)
                .formLogin()
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                        .and()
                .logout()
                        .permitAll()
                        .and()
                .rememberMe()
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(rememberMeTokenValiditySeconds);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }
}
