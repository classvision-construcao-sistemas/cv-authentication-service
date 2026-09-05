package ifg.urutai.classvision.cvauthenticationservice.controller;

import ifg.urutai.classvision.cvauthenticationservice.dto.LoginDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.JwtTokenDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.CadastroUsuarioDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.UsuarioResponseDTO;
import ifg.urutai.classvision.cvauthenticationservice.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<JwtTokenDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        JwtTokenDTO token = usuarioService.autenticar(loginDTO);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponseDTO> registrarUsuario(@Valid @RequestBody CadastroUsuarioDTO cadastroDTO) {
        UsuarioResponseDTO novoUsuario = usuarioService.registrarUsuario(cadastroDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
    }
}

