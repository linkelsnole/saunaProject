package com.example.modbus_api.security.config;

import com.example.modbus_api.security.service.JwtUtils;
import com.example.modbus_api.security.service.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/assets/") || 
               path.startsWith("/static/") || 
               path.equals("/") || 
               path.equals("/index.html") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.endsWith(".js") || 
               path.endsWith(".css") || 
               path.endsWith(".svg") || 
               path.endsWith(".jpeg") || 
               path.endsWith(".jpg") || 
               path.endsWith(".png") || 
               path.endsWith(".ico") ||
               path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7); 
            
            try {
                username = jwtUtils.extractUsername(jwtToken);
                logger.debug("Извлечен username из токена: {}", username);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT токен истек: {}", e.getMessage());
            } catch (MalformedJwtException e) {
                logger.warn("Неверный формат JWT токена: {}", e.getMessage());
            } catch (SignatureException e) {
                logger.warn("Неверная подпись JWT токена: {}", e.getMessage());
            } catch (UnsupportedJwtException e) {
                logger.warn("Неподдерживаемый JWT токен: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                logger.warn("JWT токен пустой или null: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Ошибка при извлечении username из токена", e);
            }
        } else {
            logger.debug("JWT токен не найден в заголовке Authorization");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtUtils.validateToken(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    logger.debug("Пользователь {} успешно аутентифицирован", username);
                } else {
                    logger.warn("Токен невалиден для пользователя: {}", username);
                }
            } catch (Exception e) {
                logger.error("Ошибка при загрузке пользователя {}: {}", username, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}