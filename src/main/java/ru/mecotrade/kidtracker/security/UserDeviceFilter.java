package ru.mecotrade.kidtracker.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UriTemplate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class UserDeviceFilter extends GenericFilterBean {

    private final static UriTemplate URI_TEMPLATE = new UriTemplate("/api/device/{deviceId}/{:.*}");

    private final AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String uri = httpServletRequest.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            userPrincipal.getUserInfo().setPassword(null);

            Map<String, String> uriParams = URI_TEMPLATE.match(uri);
            String deviceId = uriParams.get("deviceId");
            if (deviceId != null && userPrincipal.getUserInfo().getKids().stream().noneMatch(k -> k.getDevice().getId().equals(deviceId))) {
                log.warn("User {} attempts to access unauthorized device {} in request {}", userPrincipal.getUserInfo().getUsername(), deviceId, uri);
                throw new InsufficientAuthenticationException(deviceId);
            }
        }

        chain.doFilter(request, response);
    }
}
