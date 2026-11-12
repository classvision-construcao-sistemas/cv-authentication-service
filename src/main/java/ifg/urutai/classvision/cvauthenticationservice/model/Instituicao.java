package ifg.urutai.classvision.cvauthenticationservice.model;

import ifg.urutai.classvision.cvauthenticationservice.enums.SituacaoInstituicaoEnum;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "CV03_INSTITUICOES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instituicao implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false)
    private String nome;

    // Armazenado sem pontuação (14 caracteres; aceita o CNPJ alfanumérico).
    @Column(name = "CNPJ", nullable = false, unique = true, length = 14)
    private String cnpj;

    // Armazenado somente com dígitos (DDD + número).
    @Column(name = "TELEFONE", nullable = false, length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "SITUACAO", nullable = false, length = 20)
    @Builder.Default
    private SituacaoInstituicaoEnum situacao = SituacaoInstituicaoEnum.ATIVA;

    @Column(name = "CRIADO_EM", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "ATUALIZADO_EM")
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
