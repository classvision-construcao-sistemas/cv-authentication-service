package ifg.urutai.classvision.cvauthenticationservice.security;

import ifg.urutai.classvision.cvauthenticationservice.dto.ApiErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@NullMarked
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        ApiErrorResponseDTO body = ApiErrorResponseDTO.builder()
                .descricao("Acesso negado para este recurso.")
                .statusCode(status.value())
                .solucao("Solicite permissao adequada para o seu perfil de usuario.")
                .dataHora(LocalDateTime.now())
                .build();

        writeResponse(response, status, body);
    }

    private void writeResponse(HttpServletResponse response, HttpStatus status, ApiErrorResponseDTO body) {
        try {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(toJson(body));
        } catch (Exception ignored) {
            response.setStatus(status.value());
        }
    }

    private String toJson(ApiErrorResponseDTO body) {
        return "{"
                + "\"descricao\":\"" + escape(body.getDescricao()) + "\","
                + "\"statusCode\":" + body.getStatusCode() + ","
                + "\"solucao\":\"" + escape(body.getSolucao()) + "\","
                + "\"dataHora\":\"" + body.getDataHora() + "\""
                + "}";
    }

    private String escape(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
