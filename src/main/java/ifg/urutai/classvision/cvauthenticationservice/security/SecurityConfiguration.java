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

                        // Documentação Swagger/OpenAPI pública para facilitar testes.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Usuários - operações administrativas: apenas ADMINISTRADOR.
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/cadastrar-usuario").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/excluir-usuario/{id}").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/usuarios/desativar-usuario/{id}",
                                "/api/usuarios/ativar-usuario/{id}").hasRole("ADMINISTRADOR")

                        // Usuários - leituras e atualização: ADMINISTRADOR e COORDENADOR.
                        .requestMatchers(HttpMethod.GET,
                                "/api/usuarios/listar-usuarios",
                                "/api/usuarios/buscar-usuario/{id}",
                                "/api/usuarios/buscar-usuario-por-email/{email}")
                        .hasAnyRole("ADMINISTRADOR", "COORDENADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/atualizar-usuario/{id}")
                        .hasAnyRole("ADMINISTRADOR", "COORDENADOR")

                        // Instituições: escrita apenas ADMINISTRADOR; leitura para qualquer autenticado.
                        .requestMatchers(HttpMethod.POST, "/api/instituicoes/cadastrar-instituicao").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/instituicoes/atualizar-instituicao/{id}").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/instituicoes/ativar-instituicao/{id}",
                                "/api/instituicoes/desativar-instituicao/{id}").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET,
                                "/api/instituicoes/listar-instituicoes",
                                "/api/instituicoes/buscar-instituicao/{id}").authenticated()

                        // Qualquer outra rota só para autenticado (nunca pública por engano).
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
