package ifg.urutai.classvision.cvauthenticationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.br.CPF;

@Getter
@Setter
@RequiredArgsConstructor
public class CadastroUsuarioDTO {

    @NotNull(message = "O campo 'nome' é obrigatório.")
    @NotBlank(message = "O campo 'nome' não pode estar em branco.")
    private String nome;

    @NotNull(message = "O campo 'cpf' é obrigatório.")
    @NotBlank(message = "O campo 'cpf' não pode estar em branco.")
    @CPF
    private String cpf;

    @Email(message = "O campo 'email' deve ser um endereço de e-mail válido.")
    @NotNull(message = "O campo 'email' é obrigatório.")
    @NotBlank(message = "O campo 'email' não pode estar em branco.")
    private String email;

    @NotNull(message = "O campo 'senha' é obrigatório.")
    @NotBlank(message = "O campo 'senha' não pode estar em branco.")
    private String senha;

    @JsonFormat(pattern = "dd/MM/yyyy")
    @Schema(type = "string", pattern = "dd/MM/yyyy", example = "25/03/1990",
            description = "Data de nascimento no formato dd/MM/yyyy")
    private LocalDate dataNascimento;

    private String role;

    @NotNull(message = "O campo 'instituicaoId' é obrigatório.")
    @Schema(example = "1", description = "ID da instituição (ATIVA) à qual o usuário pertence")
    private Long instituicaoId;
}
