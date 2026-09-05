package ifg.urutai.classvision.cvauthenticationservice.controller;

import ifg.urutai.classvision.cvauthenticationservice.dto.CadastroUsuarioDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.UsuarioResponseDTO;
import ifg.urutai.classvision.cvauthenticationservice.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        List<UsuarioResponseDTO> usuarios = usuarioService.obterTodosUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obterPorId(@PathVariable Long id) {
        UsuarioResponseDTO usuario = usuarioService.obterUsuarioPorId(id);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioResponseDTO> obterPorEmail(@PathVariable String email) {
        UsuarioResponseDTO usuario = usuarioService.obterUsuarioPorEmail(email);
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CadastroUsuarioDTO cadastroDTO) {
        UsuarioResponseDTO usuarioAtualizado = usuarioService.atualizarUsuario(id, cadastroDTO);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<UsuarioResponseDTO> desativar(@PathVariable Long id) {
        UsuarioResponseDTO usuarioDesativado = usuarioService.desativarUsuario(id);
        return ResponseEntity.ok(usuarioDesativado);
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<UsuarioResponseDTO> ativar(@PathVariable Long id) {
        UsuarioResponseDTO usuarioAtivado = usuarioService.ativarUsuario(id);
        return ResponseEntity.ok(usuarioAtivado);
    }
}

