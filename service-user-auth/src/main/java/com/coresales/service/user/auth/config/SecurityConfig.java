package com.coresales.service.user.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

 /**
 * Configuración de Spring Security:
 * - /api/auth/** público (login/roles de dominio Windows).
 * - /api/auth/sesion requiere token (registra el ingreso con el rol elegido).
 *   (usa la configuración CORS de WebConfig).
 */

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter){
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint((request, response, authException) -> {
                            // 401 con el mismo formato JSON que el resto de errores (antes el body salía vacío)
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"timestamp\":\"" + java.time.Instant.now() + "\","
                                            + "\"status\":401,\"error\":\"Unauthorized\","
                                            + "\"message\":\"Falta el token o es inválido/expirado. Use el token de /api/auth/login.\","
                                            + "\"path\":\"" + request.getRequestURI() + "\"}");
                        }))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // preflight CORS
                                .requestMatchers("/error").permitAll()                  // deja pasar el mensaje real del error
                                .requestMatchers("/api/auth/sesion").authenticated()   // va antes del permitAll
                                .requestMatchers(
                                        "/api/auth/**",
                                        "/swagger-ui/**",       // Permiten cargar la interfaz gráfica web
                                        "/swagger-ui.html",     // Permiten cargar la interfaz gráfica web
                                        "/v3/api-docs/**",      // Permiten a la interfaz consultar la estructura JSON/YAML de endpoints de la AP
                                        "/v3/api-docs.yaml",    // Permiten a la interfaz consultar la estructura JSON/YAML de endpoints de la AP
                                        "/webjars/**"           // Permite cargar las librerías estáticas (CSS, JS) que usa la página de Swagger.
                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
