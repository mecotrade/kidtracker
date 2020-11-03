package ru.mecotrade.kidtracker.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import ru.mecotrade.kidtracker.dao.model.UserInfo;
import ru.mecotrade.kidtracker.device.DeviceManager;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@Component
@Slf4j
public class UserSessionListener implements HttpSessionListener {

    @Autowired
    private DeviceManager deviceManager;

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        // do nothing
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        SecurityContext context = (SecurityContext) event.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        Authentication authentication = context.getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserInfo userInfo = userPrincipal.getUserInfo();
        deviceManager.unsubscribe(userInfo);
        log.info("User session {} destroyed for {}", event.getSession().getId(), userInfo);
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        UserPrincipal userPrincipal = (UserPrincipal) event.getAuthentication().getPrincipal();
        log.info("User successfully authenticated: {}", userPrincipal.getUserInfo());
    }
}