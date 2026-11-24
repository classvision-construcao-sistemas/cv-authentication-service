package ifg.urutai.classvision.cvauthenticationservice.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS para o app Flutter rodando no navegador (Flutter Web). Sem isto o
 * navegador bloqueia as chamadas ("Failed to fetch"). Apps nativos (Android,
 * iOS, Windows) não passam por CORS.
 *
 * As origens aceitas vêm de CORS_ALLOWED_ORIGINS (separadas por vírgula, com
 * curingas de porta como http://localhost:[*]); o padrão libera só o
 * desenvolvimento local. Para publicar o app web, inclua o domínio dele.
 */
@Configuration
public class CorsConfiguracao {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowed-origins}") String origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(origens.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
