package ifg.urutai.classvision.cvauthenticationservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "CV01_USUARIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"role"})
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(nullable = false, name = "NOME")
    @NotBlank(message = "O campo 'nome' não pode estar em branco.")
    private String nome;

    @Column(name = "CPF", nullable = false, unique = true)
    @NotBlank(message = "O campo 'cpf' não pode estar em branco.")
    private String cpf;

    @Column(name = "EMAIL", nullable = false, unique = true)
    @Email(message = "O campo 'email' deve ser um endereço de e-mail válido.")
    @NotBlank(message = "O campo 'email' não pode estar em branco.")
    private String email;

    @Column(name = "SENHA", nullable = false)
    private String senha;

    @Column(name = "DATA_NASCIMENTO")
    private LocalDate dataNascimento;

    // Relacionamento N:1 para manter somente duas tabelas fisicas no banco.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "BIGINT_ROLE_ID", nullable = false)
    private Role role;

    @Column(name = "CRIADO_EM", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "ATUALIZADO_EM")
    private LocalDateTime atualizadoEm;

    @Column(name = "ATIVO", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @PrePersist
    protected void onCreate() {
        // Garante timestamps iniciais consistentes no momento da criação.
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        // Prefixo ROLE_ é exigido pelo Spring Security para reconhecer permissões por papel.
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.getNome()));
    }

    @Override
    public @NonNull String getPassword() {
        return this.senha;
    }

    @Override
    public @NonNull String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.ativo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.ativo;
    }
}
