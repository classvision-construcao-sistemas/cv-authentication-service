package ifg.urutai.classvision.cvauthenticationservice.security;

import ifg.urutai.classvision.cvauthenticationservice.dto.ApiErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@NullMarked
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponseDTO body = ApiErrorResponseDTO.builder()
                .descricao("Acesso nao autorizado.")
                .statusCode(status.value())
                .solucao("Envie um token JWT valido no cabecalho Authorization: Bearer <token>.")
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
