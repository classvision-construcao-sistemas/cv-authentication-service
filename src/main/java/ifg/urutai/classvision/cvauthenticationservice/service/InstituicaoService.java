package ifg.urutai.classvision.cvauthenticationservice.service;

import ifg.urutai.classvision.cvauthenticationservice.dto.InstituicaoDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.InstituicaoResponseDTO;
import ifg.urutai.classvision.cvauthenticationservice.enums.SituacaoInstituicaoEnum;
import ifg.urutai.classvision.cvauthenticationservice.model.Instituicao;
import ifg.urutai.classvision.cvauthenticationservice.model.Usuario;
import ifg.urutai.classvision.cvauthenticationservice.repository.InstituicaoRepository;
import ifg.urutai.classvision.cvauthenticationservice.security.ControleAcesso;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InstituicaoService {

    private final InstituicaoRepository instituicaoRepository;
    private final ControleAcesso controleAcesso;

    public InstituicaoResponseDTO cadastrar(InstituicaoDTO dto) {
        String cnpj = normalizarCnpj(dto.getCnpj());
        log.info("Cadastrando instituição: {}", cnpj);

        if (instituicaoRepository.existsByCnpj(cnpj)) {
            throw new IllegalArgumentException("CNPJ já existente na base de dados.");
        }

        Instituicao instituicao = Instituicao.builder()
                .nome(dto.getNome().trim())
                .cnpj(cnpj)
                .telefone(normalizarTelefone(dto.getTelefone()))
                .situacao(SituacaoInstituicaoEnum.ATIVA)
                .build();

        return toResponseDTO(instituicaoRepository.save(instituicao));
    }

    /** O ADMINISTRADOR vê todas; os demais, apenas a própria instituição. */
    @Transactional(readOnly = true)
    public List<InstituicaoResponseDTO> listarTodas() {
        Usuario logado = controleAcesso.usuarioLogado();
        List<Instituicao> instituicoes = controleAcesso.isAdminGeral(logado)
                ? instituicaoRepository.findAll()
                : instituicaoRepository.findAllById(List.of(controleAcesso.instituicaoId(logado)));
        return instituicoes.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public InstituicaoResponseDTO obterPorId(Long id) {
        return toResponseDTO(buscarVisivel(id));
    }

    /**
     * ADMINISTRADOR atualiza qualquer instituição; ADMIN_INSTITUICAO só a própria e sem trocar o CNPJ.
     */
    public InstituicaoResponseDTO atualizar(Long id, InstituicaoDTO dto) {
        log.info("Atualizando instituição: {}", id);
        Instituicao instituicao = buscarVisivel(id);
        String cnpj = normalizarCnpj(dto.getCnpj());

        if (!instituicao.getCnpj().equals(cnpj)) {
            if (!controleAcesso.isAdminGeral(controleAcesso.usuarioLogado())) {
                throw new AccessDeniedException("Somente o administrador geral pode alterar o CNPJ da instituição.");
            }
            if (instituicaoRepository.existsByCnpj(cnpj)) {
                throw new IllegalArgumentException("CNPJ já existente na base de dados.");
            }
        }

        instituicao.setNome(dto.getNome().trim());
        instituicao.setCnpj(cnpj);
        instituicao.setTelefone(normalizarTelefone(dto.getTelefone()));
        return toResponseDTO(instituicaoRepository.save(instituicao));
    }

    public InstituicaoResponseDTO alterarSituacao(Long id, SituacaoInstituicaoEnum situacao) {
        log.info("Alterando situação da instituição {} para {}", id, situacao);
        Instituicao instituicao = buscarEntidade(id);
        instituicao.setSituacao(situacao);
        return toResponseDTO(instituicaoRepository.save(instituicao));
    }

    /**
     * Busca a instituição para vínculo com usuário: precisa existir e estar ATIVA.
     */
    @Transactional(readOnly = true)
    public Instituicao buscarAtivaParaVinculo(Long id) {
        Instituicao instituicao = instituicaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instituição não encontrada com ID: " + id));
        if (instituicao.getSituacao() != SituacaoInstituicaoEnum.ATIVA) {
            throw new IllegalArgumentException("A instituição informada está inativa e não pode receber usuários.");
        }
        return instituicao;
    }

    /** Instituição de outro usuário (não admin geral) responde como "não encontrada". */
    private Instituicao buscarVisivel(Long id) {
        if (!controleAcesso.podeVerInstituicao(id)) {
            throw new IllegalArgumentException("Instituição não encontrada com ID: " + id);
        }
        return buscarEntidade(id);
    }

    private Instituicao buscarEntidade(Long id) {
        return instituicaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instituição não encontrada com ID: " + id));
    }

    private String normalizarCnpj(String cnpj) {
        return cnpj.replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);
    }

    private String normalizarTelefone(String telefone) {
        return telefone.replaceAll("\\D", "");
    }

    private InstituicaoResponseDTO toResponseDTO(Instituicao instituicao) {
        return InstituicaoResponseDTO.builder()
                .id(instituicao.getId())
                .nome(instituicao.getNome())
                .cnpj(instituicao.getCnpj())
                .telefone(instituicao.getTelefone())
                .situacao(instituicao.getSituacao())
                .criadoEm(instituicao.getCriadoEm())
                .atualizadoEm(instituicao.getAtualizadoEm())
                .build();
    }
}
