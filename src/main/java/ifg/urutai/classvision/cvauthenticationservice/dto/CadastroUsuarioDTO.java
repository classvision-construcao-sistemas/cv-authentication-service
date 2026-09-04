package ifg.urutai.classvision.cvauthenticationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    private List<String> roles;
}
