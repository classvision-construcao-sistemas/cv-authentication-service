package ifg.urutai.classvision.cvauthenticationservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiErrorResponseDTO {

    private final String descricao;
    private final int statusCode;
    private final String solucao;
    private final LocalDateTime dataHora;
}

