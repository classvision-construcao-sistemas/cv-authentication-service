package ifg.urutai.classvision.cvauthenticationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    @Email(message = "O campo 'email' deve ser um endereço de e-mail válido.")
    @NotBlank(message = "O campo 'email' não pode estar em branco.")
    private String email;

    @NotBlank(message = "O campo 'senha' não pode estar em branco.")
    private String senha;
}

