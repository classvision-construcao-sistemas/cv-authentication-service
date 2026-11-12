package ifg.urutai.classvision.cvauthenticationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CNPJ;

@Getter
@Setter
@NoArgsConstructor
public class InstituicaoDTO {

    @NotBlank(message = "O campo 'nome' não pode estar em branco.")
    @Schema(example = "Instituto Federal Goiano - Campus Urutaí")
    private String nome;

    @NotBlank(message = "O campo 'cnpj' não pode estar em branco.")
    @CNPJ(format = CNPJ.Format.ALPHANUMERIC, message = "O campo 'cnpj' deve ser um CNPJ válido.")
    @Schema(example = "10651417000500",
            description = "CNPJ numérico ou alfanumérico, com ou sem pontuação. É armazenado sem pontuação.")
    private String cnpj;

    @NotBlank(message = "O campo 'telefone' não pode estar em branco.")
    @Pattern(regexp = "^\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}$",
            message = "O campo 'telefone' deve conter DDD + número, ex.: (64) 3465-1900 ou 64999998888.")
    @Schema(example = "(64) 3465-1900", description = "DDD + número, com ou sem máscara. É armazenado só com dígitos.")
    private String telefone;
}
