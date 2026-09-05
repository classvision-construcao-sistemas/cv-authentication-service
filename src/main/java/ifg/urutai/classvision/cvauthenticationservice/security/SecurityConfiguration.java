package ifg.urutai.classvision.cvauthenticationservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(authz -> authz
                        // Login fica público para obtenção do token.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Operações administrativas: apenas ADMINISTRADOR.
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/{id}").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/{id}/desativar").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/{id}/ativar").hasRole("ADMINISTRADOR")

                        // Leituras e atualização exigem autenticação com perfil autorizado.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios", "/api/usuarios/{id}", "/api/usuarios/email/**")
                        .hasAnyRole("ADMINISTRADOR", "COORDENADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/{id}")
                        .hasAnyRole("ADMINISTRADOR", "COORDENADOR")

                        // Qualquer outra rota só para autenticado (nunca pública por engano).
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
