package ifg.urutai.classvision.cvauthenticationservice.config;

import ifg.urutai.classvision.cvauthenticationservice.enums.RolesEnum;
import ifg.urutai.classvision.cvauthenticationservice.enums.SituacaoInstituicaoEnum;
import ifg.urutai.classvision.cvauthenticationservice.model.Instituicao;
import ifg.urutai.classvision.cvauthenticationservice.model.Role;
import ifg.urutai.classvision.cvauthenticationservice.repository.InstituicaoRepository;
import ifg.urutai.classvision.cvauthenticationservice.model.Usuario;
import ifg.urutai.classvision.cvauthenticationservice.repository.RoleRepository;
import ifg.urutai.classvision.cvauthenticationservice.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class DataInitializer implements CommandLineRunner {

    // CNPJ reservado para a instituição padrão da plataforma (não é um CNPJ real).
    public static final String CNPJ_INSTITUICAO_PADRAO = "00000000000000";

    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Garante que todas as roles do RolesEnum existam (cria as que faltarem, ex.: ADMIN_INSTITUICAO).
        for (RolesEnum roleEnum : RolesEnum.values()) {
            if (roleRepository.findByNome(roleEnum.name()).isEmpty()) {
                roleRepository.save(Role.builder()
                        .nome(roleEnum.name())
                        .descricao(roleEnum.getDescricao())
                        .build());
                log.info("Role criada: {}", roleEnum.name());
            }
        }

        // Criar usuário administrador padrão se não existir
        if (!usuarioRepository.existsByEmail("admin@classvision.com")) {
            log.info("Criando usuário administrador padrão...");

            // Instituição padrão (dona da plataforma) do administrador. Só é criada junto com o
            // admin: depois disso o CNPJ pode ser trocado pelo real sem gerar uma nova a cada subida.
            Instituicao instituicaoPadrao = instituicaoRepository.findByCnpj(CNPJ_INSTITUICAO_PADRAO)
                    .orElseGet(() -> {
                        log.info("Criando instituição padrão...");
                        return instituicaoRepository.save(Instituicao.builder()
                                .nome("ClassVision")
                                .cnpj(CNPJ_INSTITUICAO_PADRAO)
                                .telefone("0000000000")
                                .situacao(SituacaoInstituicaoEnum.ATIVA)
                                .build());
                    });

            Role adminRole = roleRepository.findByNome("ADMINISTRADOR")
                    .orElseThrow(() -> new RuntimeException("Role ADMINISTRADOR não encontrada"));

            Usuario adminUser = Usuario.builder()
                    .nome("Administrador")
                    .email("admin@classvision.com")
                    .cpf("00000000000")
                    .senha(passwordEncoder.encode("admin123456"))
                    .ativo(true)
                    .role(adminRole)
                    .instituicao(instituicaoPadrao)
                    .build();

            usuarioRepository.save(adminUser);
            log.info("Usuário administrador criado com sucesso!");
            log.info("Email: admin@classvision.com");
            log.info("Senha: admin123456");
        }
    }
}

