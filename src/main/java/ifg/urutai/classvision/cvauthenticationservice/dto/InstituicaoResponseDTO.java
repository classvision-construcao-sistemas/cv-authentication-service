package ifg.urutai.classvision.cvauthenticationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ifg.urutai.classvision.cvauthenticationservice.enums.SituacaoInstituicaoEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class InstituicaoResponseDTO {

    private Long id;
    private String nome;
    private String cnpj;
    private String telefone;
    private SituacaoInstituicaoEnum situacao;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Schema(type = "string", pattern = "dd/MM/yyyy HH:mm:ss", example = "12/11/2026 09:30:00")
    private LocalDateTime criadoEm;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @Schema(type = "string", pattern = "dd/MM/yyyy HH:mm:ss", example = "12/11/2026 09:30:00")
    private LocalDateTime atualizadoEm;
}
