package ru.mecotrade.kidtracker.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.mecotrade.kidtracker.dao.UserService;
import ru.mecotrade.kidtracker.dao.model.UserInfo;

import java.util.Collections;
import java.util.Optional;

@Service
public class DaoUserDetailsService implements UserDetailsService  {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Optional<UserInfo> userInfoOptional = userService.getByUsername(username);
        if (userInfoOptional.isPresent()) {
            // TODO add more roles
            UserInfo userInfo = userInfoOptional.get();
            return new UserPrincipal(userInfo, Collections.singleton(new SimpleGrantedAuthority(Roles.USER.toString())));
        } else {
            throw new UsernameNotFoundException(username);
        }
    }
}
