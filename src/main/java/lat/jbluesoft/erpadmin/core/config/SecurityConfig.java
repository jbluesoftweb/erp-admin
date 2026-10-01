package lat.jbluesoft.erpadmin.core.config;

import lat.jbluesoft.erpadmin.core.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig
 * Configuración de Spring Security 7.x (Spring Boot 4.x)
 * - Autenticación con BD (CustomUserDetailsService)
 * - BCrypt para passwords
 * - Login form personalizado
 * - Control de acceso por roles
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager
     * Spring Security 7.x: Usar ProviderManager directamente
     */
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    /**
     * SecurityFilterChain - Configuración de seguridad HTTP
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Recursos públicos
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                        .requestMatchers("/login", "/error").permitAll()

                        // Dashboard - Accesible por todos los roles autenticados
                        .requestMatchers("/dashboard").authenticated()

                                // Rutas protegidas por rol
                                .requestMatchers("/admin/**").hasAnyRole("ADMIN_SYS")


                                // mi-password: accesible por cualquier usuario autenticado
                                .requestMatchers("/asistencia/usuarios/mi-password").authenticated()

                                // USUARIOS ERP Admin — solo ADMIN_SYS
                                .requestMatchers("/asistencia/usuarios/**").hasAnyRole("ADMIN_SYS", "ADMIN_APP")


                                // Regla general — cubre todo lo demás de asistencia
                                .requestMatchers("/asistencia/**").hasAnyRole("ADMIN_DPTO", "ADMIN_APP", "ADMIN_SYS")

                                .requestMatchers("/almuerzo/**").hasAnyRole("ADMIN_DPTO", "ADMIN_SYS")

                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .usernameParameter("codigo")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(false)
                        )
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}