package ifg.urutai.classvision.cvauthenticationservice.config;

import ifg.urutai.classvision.cvauthenticationservice.model.Role;
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

    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Criar roles padrão se não existirem
        if (roleRepository.count() == 0) {
            log.info("Inicializando roles padrão...");

            Role adminRole = Role.builder()
                    .nome("ADMINISTRADOR")
                    .descricao("Administrador do sistema")
                    .build();

            Role professorRole = Role.builder()
                    .nome("PROFESSOR")
                    .descricao("Professor da instituição")
                    .build();

            Role coordenadorRole = Role.builder()
                    .nome("COORDENADOR")
                    .descricao("Coordenador de curso")
                    .build();

            Role secretarioRole = Role.builder()
                    .nome("SECRETARIO")
                    .descricao("Secretário da instituição")
                    .build();

            Role assistenteRole = Role.builder()
                    .nome("ASSISTENTE")
                    .descricao("Assistente de coordenação")
                    .build();

            roleRepository.save(adminRole);
            roleRepository.save(professorRole);
            roleRepository.save(coordenadorRole);
            roleRepository.save(secretarioRole);
            roleRepository.save(assistenteRole);

            log.info("Roles inicializadas com sucesso!");
        }

        // Criar usuário administrador padrão se não existir
        if (!usuarioRepository.existsByEmail("admin@classvision.com")) {
            log.info("Criando usuário administrador padrão...");

            Role adminRole = roleRepository.findByNome("ADMINISTRADOR")
                    .orElseThrow(() -> new RuntimeException("Role ADMINISTRADOR não encontrada"));

            Usuario adminUser = Usuario.builder()
                    .nome("Administrador")
                    .email("admin@classvision.com")
                    .cpf("00000000000")
                    .senha(passwordEncoder.encode("admin123456"))
                    .ativo(true)
                    .role(adminRole)
                    .build();

            usuarioRepository.save(adminUser);
            log.info("Usuário administrador criado com sucesso!");
            log.info("Email: admin@classvision.com");
            log.info("Senha: admin123456");
        }
    }
}

