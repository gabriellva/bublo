package com.gabriel.bublo.config;

import com.gabriel.bublo.repository.UsuarioRepository;
import com.gabriel.bublo.security.AuthAuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança do Bublo.
 * - Acesso restrito ao administrador autenticado.
 * - Senhas armazenadas com BCrypt.
 * - Proteção CSRF habilitada.
 * - Sessão por cookie, sem "lembrar-me".
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final int USERNAME_AUDIT_MAX_LENGTH = 100;

    private final UsuarioRepository usuarioRepository;
    private final AuthAuditService authAuditService;

    public SecurityConfig(UsuarioRepository usuarioRepository, AuthAuditService authAuditService) {
        this.usuarioRepository = usuarioRepository;
        this.authAuditService = authAuditService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/lib/**", "/images/**", "/error", "/favicon.ico").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    authAuditService.registrarSucesso(authentication.getName());
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                })
                .failureHandler((request, response, exception) -> {
                    authAuditService.registrarFalha(nomeParaAuditoria(request.getParameter("username")));
                    response.sendRedirect(request.getContextPath() + "/login?error=true");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("BUBLO_SESSION", "JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expirada=true")
            );

        return http.build();
    }

    private String nomeParaAuditoria(String username) {
        if (username == null || username.isBlank()) return "desconhecido";
        return username.length() > USERNAME_AUDIT_MAX_LENGTH
            ? username.substring(0, USERNAME_AUDIT_MAX_LENGTH)
            : username;
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return username -> usuarioRepository.buscarPorUsername(username)
            .map(u -> User.withUsername(u.getUsername())
                .password(u.getSenhaHash())
                .roles("ADMIN")
                .accountLocked(!u.getAtivo())
                .build())
            .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Usuário não encontrado"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
