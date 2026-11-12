package ifg.urutai.classvision.cvauthenticationservice.service;

import ifg.urutai.classvision.cvauthenticationservice.dto.CadastroUsuarioDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.JwtTokenDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.LoginDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.UsuarioResponseDTO;
import ifg.urutai.classvision.cvauthenticationservice.enums.RolesEnum;
import ifg.urutai.classvision.cvauthenticationservice.model.Instituicao;
import ifg.urutai.classvision.cvauthenticationservice.model.Role;
import ifg.urutai.classvision.cvauthenticationservice.model.Usuario;
import ifg.urutai.classvision.cvauthenticationservice.repository.RoleRepository;
import ifg.urutai.classvision.cvauthenticationservice.repository.UsuarioRepository;
import ifg.urutai.classvision.cvauthenticationservice.security.ControleAcesso;
import ifg.urutai.classvision.cvauthenticationservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private static final String ROLE_PADRAO = RolesEnum.PROFESSOR.name();

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final InstituicaoService instituicaoService;
    private final ControleAcesso controleAcesso;

    /**
     * Cadastra um usuário.
     * - ADMINISTRADOR: em qualquer instituição, com qualquer role.
     * - ADMIN_INSTITUICAO: somente na própria instituição e com role até ADMIN_INSTITUICAO.
     */
    public UsuarioResponseDTO registrarUsuario(CadastroUsuarioDTO dto) {
        log.info("Registrando novo usuário: {}", dto.getEmail());
        Usuario logado = controleAcesso.usuarioLogado();

        Instituicao instituicao = resolverInstituicaoDoCadastro(logado, dto.getInstituicaoId());
        Role role = resolveRole(dto.getRole());
        controleAcesso.validarConcessaoRole(role.getNome());

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email já existente na base de dados.");
        }
        if (usuarioRepository.existsByCpf(dto.getCpf())) {
            throw new IllegalArgumentException("CPF já existente na base de dados.");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .cpf(dto.getCpf())
                .email(dto.getEmail())
                .senha(passwordEncoder.encode(dto.getSenha()))
                .dataNascimento(dto.getDataNascimento())
                .ativo(true)
                .role(role)
                .instituicao(instituicao)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuário {} registrado por {} na instituição {}", usuarioSalvo.getId(), logado.getId(), instituicao.getId());
        return convertToResponseDTO(usuarioSalvo);
    }

    /**
     * Autentica um usuário e retorna um token JWT
     */
    @Transactional(readOnly = true)
    public JwtTokenDTO autenticar(LoginDTO loginDTO) {
        log.info("Autenticando usuário: {}", loginDTO.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getSenha()));

            Usuario usuario = (Usuario) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(usuario);
            log.info("Usuário autenticado com sucesso: {}", usuario.getId());

            return JwtTokenDTO.builder()
                    .token(token)
                    .tipo("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationTime())
                    .build();
        } catch (AuthenticationException e) {
            log.error("Falha na autenticação: {}", loginDTO.getEmail());
            throw new IllegalArgumentException("Email ou senha inválidos.");
        }
    }

    /**
     * Lista usuários: o ADMINISTRADOR vê todos; os demais, apenas os da própria instituição.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> obterTodosUsuarios() {
        Usuario logado = controleAcesso.usuarioLogado();
        List<Usuario> usuarios = controleAcesso.isAdminGeral(logado)
                ? usuarioRepository.findAll()
                : usuarioRepository.findAllByInstituicaoId(controleAcesso.instituicaoId(logado));
        return usuarios.stream().map(this::convertToResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obterUsuarioPorId(Long id) {
        return convertToResponseDTO(buscarVisivelPorId(id));
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obterUsuarioPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(controleAcesso::podeVerUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com email: " + email));
        return convertToResponseDTO(usuario);
    }

    /**
     * Atualiza um usuário (ADMINISTRADOR ou ADMIN_INSTITUICAO).
     * - Só gerencia usuários com nível igual ou inferior ao seu.
     * - Só concede role até o próprio nível e nunca altera a própria role.
     * - Somente o ADMINISTRADOR transfere o usuário de instituição.
     */
    public UsuarioResponseDTO atualizarUsuario(Long id, CadastroUsuarioDTO dto) {
        log.info("Atualizando usuário: {}", id);
        Usuario logado = controleAcesso.usuarioLogado();
        Usuario usuario = buscarVisivelPorId(id);
        controleAcesso.validarGerenciamento(usuario);

        if (dto.getRole() != null && !dto.getRole().isBlank() && !dto.getRole().equals(nomeRole(usuario))) {
            controleAcesso.validarNaoEhOProprio(usuario, "alterar a sua própria role");
            Role novaRole = resolveRole(dto.getRole());
            controleAcesso.validarConcessaoRole(novaRole.getNome());
            usuario.setRole(novaRole);
        }

        if (dto.getInstituicaoId() != null && !dto.getInstituicaoId().equals(controleAcesso.instituicaoId(usuario))) {
            if (!controleAcesso.isAdminGeral(logado)) {
                throw new AccessDeniedException("Somente o administrador geral pode transferir usuários entre instituições.");
            }
            usuario.setInstituicao(instituicaoService.buscarAtivaParaVinculo(dto.getInstituicaoId()));
        }

        if (!usuario.getEmail().equals(dto.getEmail()) && usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email já existente na base de dados.");
        }
        if (!usuario.getCpf().equals(dto.getCpf()) && usuarioRepository.existsByCpf(dto.getCpf())) {
            throw new IllegalArgumentException("CPF já existente na base de dados.");
        }

        usuario.setNome(dto.getNome());
        usuario.setCpf(dto.getCpf());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setDataNascimento(dto.getDataNascimento());

        Usuario usuarioAtualizado = usuarioRepository.save(usuario);
        log.info("Usuário {} atualizado por {}", usuarioAtualizado.getId(), logado.getId());
        return convertToResponseDTO(usuarioAtualizado);
    }

    public void deletarUsuario(Long id) {
        Usuario usuario = buscarGerenciavel(id, "excluir");
        usuarioRepository.delete(usuario);
        log.info("Usuário {} excluído por {}", id, controleAcesso.usuarioLogado().getId());
    }

    public UsuarioResponseDTO desativarUsuario(Long id) {
        Usuario usuario = buscarGerenciavel(id, "desativar");
        usuario.setAtivo(false);
        log.info("Usuário {} desativado por {}", id, controleAcesso.usuarioLogado().getId());
        return convertToResponseDTO(usuarioRepository.save(usuario));
    }

    public UsuarioResponseDTO ativarUsuario(Long id) {
        Usuario usuario = buscarGerenciavel(id, "ativar");
        usuario.setAtivo(true);
        log.info("Usuário {} ativado por {}", id, controleAcesso.usuarioLogado().getId());
        return convertToResponseDTO(usuarioRepository.save(usuario));
    }

    /**
     * Instituição do novo usuário: o ADMINISTRADOR escolhe qualquer uma (obrigatório informar);
     * o ADMIN_INSTITUICAO sempre cadastra na própria (o campo pode ser omitido).
     */
    private Instituicao resolverInstituicaoDoCadastro(Usuario logado, Long instituicaoIdInformada) {
        if (controleAcesso.isAdminGeral(logado)) {
            if (instituicaoIdInformada == null) {
                throw new IllegalArgumentException("O campo 'instituicaoId' é obrigatório.");
            }
            return instituicaoService.buscarAtivaParaVinculo(instituicaoIdInformada);
        }

        Long instituicaoDoLogado = controleAcesso.instituicaoId(logado);
        if (instituicaoIdInformada != null && !Objects.equals(instituicaoIdInformada, instituicaoDoLogado)) {
            throw new AccessDeniedException("Você só pode cadastrar usuários na sua própria instituição.");
        }
        return instituicaoService.buscarAtivaParaVinculo(instituicaoDoLogado);
    }

    /** Busca um usuário visível para o logado; de outra instituição responde como "não encontrado". */
    private Usuario buscarVisivelPorId(Long id) {
        return usuarioRepository.findById(id)
                .filter(controleAcesso::podeVerUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + id));
    }

    private Usuario buscarGerenciavel(Long id, String acao) {
        Usuario usuario = buscarVisivelPorId(id);
        controleAcesso.validarNaoEhOProprio(usuario, acao + " o seu próprio usuário");
        controleAcesso.validarGerenciamento(usuario);
        return usuario;
    }

    private String nomeRole(Usuario usuario) {
        return usuario.getRole() != null ? usuario.getRole().getNome() : null;
    }

    private UsuarioResponseDTO convertToResponseDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .cpf(usuario.getCpf())
                .email(usuario.getEmail())
                .dataNascimento(usuario.getDataNascimento())
                .role(nomeRole(usuario))
                .instituicaoId(controleAcesso.instituicaoId(usuario))
                .instituicaoNome(usuario.getInstituicao() != null ? usuario.getInstituicao().getNome() : null)
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .ativo(usuario.getAtivo())
                .build();
    }

    private Role resolveRole(String roleName) {
        String nome = (roleName == null || roleName.isBlank()) ? ROLE_PADRAO : roleName;
        return roleRepository.findByNome(nome)
                .orElseThrow(() -> new IllegalArgumentException("Role não encontrada: " + nome));
    }
}
