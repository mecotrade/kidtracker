package ru.mecotrade.kidtracker.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.UserInfo;

import java.util.Collections;
import java.util.Optional;

@Service
@Slf4j
public class DaoUserDetailsService implements UserDetailsService  {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Optional<UserInfo> userInfoOptional = userService.getByUsername(username);
        if (userInfoOptional.isPresent()) {
            UserInfo userInfo = userInfoOptional.get();
            return new UserPrincipal(userInfo,
                    Collections.singleton(userInfo.isAdmin() ? Roles.ADMIN.toAuthority() : Roles.USER.toAuthority()));
        } else {
            throw new UsernameNotFoundException(username);
        }
    }
}
