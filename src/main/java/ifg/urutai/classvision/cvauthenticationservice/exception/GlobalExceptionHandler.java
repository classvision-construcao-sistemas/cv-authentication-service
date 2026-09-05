package ifg.urutai.classvision.cvauthenticationservice.exception;

import ifg.urutai.classvision.cvauthenticationservice.dto.ApiErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Locale;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        HttpStatus status = inferStatus(ex.getMessage());
        return ResponseEntity.status(status).body(buildError(status, ex.getMessage(), inferSolution(status, ex.getMessage())));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleUsernameNotFound(UsernameNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status)
                .body(buildError(status, ex.getMessage(), "Verifique o email informado ou realize o cadastro."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String descricao;
        if (ex.getBindingResult().hasFieldErrors()) {
            descricao = ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        } else {
            descricao = "Requisicao invalida.";
        }

        if (descricao == null || descricao.isBlank()) {
            descricao = "Requisicao invalida.";
        }

        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(buildError(status, descricao, "Revise os campos obrigatorios e o formato dos dados enviados."));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleValidationExceptions() {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(buildError(status, "Parametro invalido na requisicao.", "Corrija os parametros e tente novamente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDTO> handleGeneric(Exception ex) {
        log.error("Erro inesperado na API", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(buildError(status, "Erro interno no servidor.", "Tente novamente em instantes. Se persistir, contate o suporte."));
    }

    private ApiErrorResponseDTO buildError(HttpStatus status, String descricao, String solucao) {
        return ApiErrorResponseDTO.builder()
                .descricao(descricao)
                .statusCode(status.value())
                .solucao(solucao)
                .dataHora(LocalDateTime.now())
                .build();
    }

    private HttpStatus inferStatus(String descricao) {
        if (descricao == null) {
            return HttpStatus.BAD_REQUEST;
        }

        String normalized = descricao.toLowerCase(Locale.ROOT);
        if (normalized.contains("nao encontrado") || normalized.contains("não encontrado")) {
            return HttpStatus.NOT_FOUND;
        }
        if (normalized.contains("ja existente") || normalized.contains("já existente")) {
            return HttpStatus.CONFLICT;
        }
        if (normalized.contains("senha invalida") || normalized.contains("senha inválida")
                || normalized.contains("email ou senha")) {
            return HttpStatus.UNAUTHORIZED;
        }

        return HttpStatus.BAD_REQUEST;
    }

    private String inferSolution(HttpStatus status, String descricao) {
        if (status == HttpStatus.NOT_FOUND) {
            return "Confirme o identificador informado e tente novamente.";
        }
        if (status == HttpStatus.CONFLICT) {
            return "Use outro valor para os dados unicos (como email ou CPF).";
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            return "Valide suas credenciais e gere um novo token, se necessario.";
        }
        if (descricao != null && descricao.toLowerCase(Locale.ROOT).contains("role")) {
            return "Confira os perfis permitidos e envie um nome de role valido.";
        }

        return "Revise os dados enviados e tente novamente.";
    }
}
