package com.example.modbus_api.service;

import com.example.modbus_api.config.JwtProperties;
import com.example.modbus_api.dto.auth.AuthResponse;
import com.example.modbus_api.dto.auth.UserInfoResponse;
import com.example.modbus_api.exception.AuthException;
import com.example.modbus_api.logging.ActionType;
import com.example.modbus_api.security.CookieUtils;
import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.RefreshToken;
import com.example.modbus_api.security.repository.UserRepository;
import com.example.modbus_api.security.service.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final UserActionLogService userActionLogService;

    public AuthService(
            AuthenticationManager authenticationManager,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserRepository userRepository,
            UserActionLogService userActionLogService
    ) {
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.userActionLogService = userActionLogService;
    }

    public AuthResponse login(String username, String password, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (AuthenticationException e) {
            userActionLogService.logAs(username, ActionType.AUTH, "Вход в систему", false, "Неверный логин или пароль");
            throw e;
        }
        userActionLogService.logAs(username, ActionType.AUTH, "Вход в систему", true, null);

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("User not found: " + username));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        CookieUtils.addHttpOnlyCookie(
                response,
                jwtProperties.getRefreshCookieName(),
                refreshToken.getToken(),
                jwtProperties.getRefreshTokenExpirationMs(),
                jwtProperties.isSecureCookie(),
                jwtProperties.getRefreshCookiePath(),
                jwtProperties.getRefreshCookieSameSite()
        );

        return buildResponse(user);
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = CookieUtils.getCookieValue(request, jwtProperties.getRefreshCookieName())
                .orElseThrow(() -> new AuthException("Refresh token is missing"));

        RefreshToken refreshToken = refreshTokenService.rotateRefreshToken(refreshTokenValue);
        CookieUtils.addHttpOnlyCookie(
                response,
                jwtProperties.getRefreshCookieName(),
                refreshToken.getToken(),
                jwtProperties.getRefreshTokenExpirationMs(),
                jwtProperties.isSecureCookie(),
                jwtProperties.getRefreshCookiePath(),
                jwtProperties.getRefreshCookieSameSite()
        );

        return buildResponse(refreshToken.getUser());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.getName() != null && !"anonymousUser".equals(current.getName())) {
            userActionLogService.logAs(current.getName(), ActionType.AUTH, "Выход из системы", true, null);
        }
        String refreshTokenValue = CookieUtils.getCookieValue(request, jwtProperties.getRefreshCookieName()).orElse(null);
        if (refreshTokenValue != null && !refreshTokenValue.trim().isEmpty()) {
            refreshTokenService.deleteByToken(refreshTokenValue);
        }

        CookieUtils.clearCookie(
                response,
                jwtProperties.getRefreshCookieName(),
                jwtProperties.isSecureCookie(),
                jwtProperties.getRefreshCookiePath(),
                jwtProperties.getRefreshCookieSameSite()
        );
    }

    public UserInfoResponse currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new AuthException("User is not authenticated");
        }

        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AuthException("User is not authenticated"));
        return UserInfoResponse.from(user);
    }

    private AuthResponse buildResponse(AppUser user) {
        return new AuthResponse(
                jwtService.generateAccessToken(toUserDetails(user)),
                "Bearer",
                jwtProperties.getAccessTokenExpirationMs(),
                UserInfoResponse.from(user)
        );
    }

    // AppUser в этом проекте не реализует UserDetails, поэтому собираем принципала вручную
    private UserDetails toUserDetails(AppUser user) {
        return new User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
