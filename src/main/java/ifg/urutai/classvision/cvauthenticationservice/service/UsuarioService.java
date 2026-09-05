package ifg.urutai.classvision.cvauthenticationservice.service;

import ifg.urutai.classvision.cvauthenticationservice.dto.CadastroUsuarioDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.LoginDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.JwtTokenDTO;
import ifg.urutai.classvision.cvauthenticationservice.dto.UsuarioResponseDTO;
import ifg.urutai.classvision.cvauthenticationservice.model.Role;
import ifg.urutai.classvision.cvauthenticationservice.model.Usuario;
import ifg.urutai.classvision.cvauthenticationservice.repository.RoleRepository;
import ifg.urutai.classvision.cvauthenticationservice.repository.UsuarioRepository;
import ifg.urutai.classvision.cvauthenticationservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registra um novo usuário (apenas administrador pode fazer isso)
     */
    public UsuarioResponseDTO registrarUsuario(CadastroUsuarioDTO dto) {
        log.info("Registrando novo usuário: {}", dto.getEmail());

        // Validar se email e CPF já existem
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            log.warn("Tentativa de cadastro com email já existente: {}", dto.getEmail());
            throw new IllegalArgumentException("Email já existente na base de dados.");
        }

        if (usuarioRepository.existsByCpf(dto.getCpf())) {
            log.warn("Tentativa de cadastro com CPF já existente: {}", dto.getCpf());
            throw new IllegalArgumentException("CPF já existente na base de dados.");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .cpf(dto.getCpf())
                .email(dto.getEmail())
                .senha(passwordEncoder.encode(dto.getSenha()))
                .dataNascimento(dto.getDataNascimento())
                .ativo(true)
                .build();

        usuario.setRole(resolveRole(dto.getRole()));

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuário registrado com sucesso: {}", usuarioSalvo.getId());
        return convertToResponseDTO(usuarioSalvo);
    }

    /**
     * Autentica um usuário e retorna um token JWT
     */
    public JwtTokenDTO autenticar(LoginDTO loginDTO) {
        log.info("Autenticando usuário: {}", loginDTO.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getEmail(),
                            loginDTO.getSenha()
                    )
            );

            Usuario usuario = (Usuario) authentication.getPrincipal();
            String token = jwtTokenProvider.generateTokenWithRoles(usuario);

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
     * Obter todos os usuários
     */
    public List<UsuarioResponseDTO> obterTodosUsuarios() {
        log.debug("Obtendo todos os usuários");
        return usuarioRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obter usuário por ID
     */
    public UsuarioResponseDTO obterUsuarioPorId(Long id) {
        log.debug("Obtendo usuário por ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado com ID: " + id);
                });
        return convertToResponseDTO(usuario);
    }

    /**
     * Obter usuário por Email
     */
    public UsuarioResponseDTO obterUsuarioPorEmail(String email) {
        log.debug("Obtendo usuário por email: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado por email: {}", email);
                    return new IllegalArgumentException("Usuário não encontrado com email: " + email);
                });
        return convertToResponseDTO(usuario);
    }

    /**
     * Atualizar usuário
     */
    public UsuarioResponseDTO atualizarUsuario(Long id, CadastroUsuarioDTO dto) {
        log.info("Atualizando usuário: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para atualizar: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado com ID: " + id);
                });

        // Validar se o novo email já existe em outro usuário
        if (!usuario.getEmail().equals(dto.getEmail()) && usuarioRepository.existsByEmail(dto.getEmail())) {
            log.warn("Email já existe: {}", dto.getEmail());
            throw new IllegalArgumentException("Email já existente na base de dados.");
        }

        // Validar se o novo CPF já existe em outro usuário
        if (!usuario.getCpf().equals(dto.getCpf()) && usuarioRepository.existsByCpf(dto.getCpf())) {
            log.warn("CPF já existe: {}", dto.getCpf());
            throw new IllegalArgumentException("CPF já existente na base de dados.");
        }

        usuario.setNome(dto.getNome());
        usuario.setCpf(dto.getCpf());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setDataNascimento(dto.getDataNascimento());

        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            usuario.setRole(resolveRole(dto.getRole()));
        }

        Usuario usuarioAtualizado = usuarioRepository.save(usuario);
        log.info("Usuário atualizado com sucesso: {}", usuarioAtualizado.getId());
        return convertToResponseDTO(usuarioAtualizado);
    }

    /**
     * Deletar usuário
     */
    public void deletarUsuario(Long id) {
        log.info("Deletando usuário: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para deletar: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado com ID: " + id);
                });
        usuarioRepository.delete(usuario);
        log.info("Usuário deletado com sucesso: {}", id);
    }

    /**
     * Desativar usuário (soft delete)
     */
    public UsuarioResponseDTO desativarUsuario(Long id) {
        log.info("Desativando usuário: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para desativar: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado com ID: " + id);
                });
        usuario.setAtivo(false);
        Usuario usuarioDesativado = usuarioRepository.save(usuario);
        log.info("Usuário desativado com sucesso: {}", usuarioDesativado.getId());
        return convertToResponseDTO(usuarioDesativado);
    }

    /**
     * Ativar usuário
     */
    public UsuarioResponseDTO ativarUsuario(Long id) {
        log.info("Ativando usuário: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para ativar: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado com ID: " + id);
                });
        usuario.setAtivo(true);
        Usuario usuarioAtivado = usuarioRepository.save(usuario);
        log.info("Usuário ativado com sucesso: {}", usuarioAtivado.getId());
        return convertToResponseDTO(usuarioAtivado);
    }

    /**
     * Converter entidade Usuario para DTO de resposta
     */
    private UsuarioResponseDTO convertToResponseDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .cpf(usuario.getCpf())
                .email(usuario.getEmail())
                .dataNascimento(usuario.getDataNascimento())
                .role(usuario.getRole() != null ? usuario.getRole().getNome() : null)
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .ativo(usuario.getAtivo())
                .build();
    }

    private Role resolveRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return roleRepository.findByNome("PROFESSOR")
                    .orElseThrow(() -> new IllegalArgumentException("Role padrão não encontrada"));
        }

        return roleRepository.findByNome(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role não encontrada: " + roleName));
    }
}

