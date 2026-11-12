package ifg.urutai.classvision.cvauthenticationservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI cvAuthenticationOpenApi() {
        // Esquema Bearer JWT: habilita o botão "Authorize" no Swagger UI
        // para enviar o token obtido em /auth/login nas demais requisições.
        return new OpenAPI()
                .info(new Info()
                        .title("CV Authentication Service")
                        .description("API de autenticação e gestão de usuários do ClassVision.")
                        .version("0.0.1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
