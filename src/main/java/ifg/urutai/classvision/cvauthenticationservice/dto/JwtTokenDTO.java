package ifg.urutai.classvision.cvauthenticationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class JwtTokenDTO {

    private String token;
    private String tipo;
    private Long expiresIn;
}

