package ifg.urutai.classvision.cvauthenticationservice.enums;

/**
 * Roles do sistema com o nível de privilégio de cada uma.
 * Quanto maior o nível, maior o privilégio. Um usuário nunca concede uma role
 * acima do próprio nível nem gerencia usuários com nível maior que o seu.
 */
public enum RolesEnum {

    ADMINISTRADOR(100, "Administrador geral da plataforma"),
    ADMIN_INSTITUICAO(80, "Administrador da instituição"),
    COORDENADOR(60, "Coordenador de curso"),
    PROFESSOR(40, "Professor da instituição"),
    SECRETARIO(40, "Secretário da instituição"),
    ASSISTENTE(20, "Assistente de coordenação");

    private final int nivel;
    private final String descricao;

    RolesEnum(int nivel, String descricao) {
        this.nivel = nivel;
        this.descricao = descricao;
    }

    public int getNivel() {
        return nivel;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Nível da role pelo nome gravado no banco (0 se desconhecida). */
    public static int nivelDe(String nomeRole) {
        if (nomeRole == null) {
            return 0;
        }
        try {
            return RolesEnum.valueOf(nomeRole).getNivel();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
}
