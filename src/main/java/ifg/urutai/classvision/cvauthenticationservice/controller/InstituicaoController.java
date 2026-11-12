package ifg.urutai.classvision.cvauthenticationservice.controller;

import ifg.urutai.classvision.cvauthenticationservice.dto.InstituicaoDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.InstituicaoResponseDTO;
import ifg.urutai.classvision.cvauthenticationservice.enums.SituacaoInstituicaoEnum;
import ifg.urutai.classvision.cvauthenticationservice.service.InstituicaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instituicoes")
@RequiredArgsConstructor
public class InstituicaoController {

    private final InstituicaoService instituicaoService;

    @PostMapping("/cadastrar-instituicao")
    public ResponseEntity<InstituicaoResponseDTO> cadastrar(@Valid @RequestBody InstituicaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instituicaoService.cadastrar(dto));
    }

    @GetMapping("/listar-instituicoes")
    public ResponseEntity<List<InstituicaoResponseDTO>> listarTodas() {
        return ResponseEntity.ok(instituicaoService.listarTodas());
    }

    @GetMapping("/buscar-instituicao/{id}")
    public ResponseEntity<InstituicaoResponseDTO> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(instituicaoService.obterPorId(id));
    }

    @PutMapping("/atualizar-instituicao/{id}")
    public ResponseEntity<InstituicaoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody InstituicaoDTO dto) {
        return ResponseEntity.ok(instituicaoService.atualizar(id, dto));
    }

    @PatchMapping("/ativar-instituicao/{id}")
    public ResponseEntity<InstituicaoResponseDTO> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(instituicaoService.alterarSituacao(id, SituacaoInstituicaoEnum.ATIVA));
    }

    @PatchMapping("/desativar-instituicao/{id}")
    public ResponseEntity<InstituicaoResponseDTO> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(instituicaoService.alterarSituacao(id, SituacaoInstituicaoEnum.INATIVA));
    }
}
