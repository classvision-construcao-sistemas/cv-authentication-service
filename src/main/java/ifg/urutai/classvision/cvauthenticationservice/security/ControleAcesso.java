package ifg.urutai.classvision.cvauthenticationservice.security;

import ifg.urutai.classvision.cvauthenticationservice.enums.RolesEnum;
import ifg.urutai.classvision.cvauthenticationservice.model.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Regras de autorização por instituição e por nível de role.
 *
 * - ADMINISTRADOR (geral) enxerga e gerencia tudo.
 * - Os demais só enxergam dados da própria instituição.
 * - Ninguém gerencia usuários com nível acima do seu nem concede role acima do seu nível.
 * - Ninguém altera a própria role, se desativa ou se exclui.
 */
@Component
public class ControleAcesso {

    public Usuario usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario usuario)) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        return usuario;
    }

    public boolean isAdminGeral(Usuario usuario) {
        return RolesEnum.ADMINISTRADOR.name().equals(nomeRole(usuario));
    }

    public int nivel(Usuario usuario) {
        return RolesEnum.nivelDe(nomeRole(usuario));
    }

    /** O usuário logado pode ver dados desta instituição? */
    public boolean podeVerInstituicao(Long instituicaoId) {
        Usuario logado = usuarioLogado();
        return isAdminGeral(logado) || Objects.equals(instituicaoId(logado), instituicaoId);
    }

    /** O usuário logado pode ver este usuário? (mesma instituição ou admin geral) */
    public boolean podeVerUsuario(Usuario alvo) {
        return podeVerInstituicao(instituicaoId(alvo));
    }

    /**
     * Valida se o usuário logado pode gerenciar (editar/ativar/desativar/excluir) o alvo.
     * Chamar somente depois de garantir que o alvo é visível (senão responda 404).
     */
    public void validarGerenciamento(Usuario alvo) {
        Usuario logado = usuarioLogado();
        if (nivel(alvo) > nivel(logado)) {
            throw new AccessDeniedException("Você não pode gerenciar um usuário com nível de acesso acima do seu.");
        }
    }

    /**
     * Ninguém executa sobre si mesmo ações que possam tirar o próprio acesso.
     * @param acao complemento da frase "Você não pode ...", ex.: "desativar o seu próprio usuário"
     */
    public void validarNaoEhOProprio(Usuario alvo, String acao) {
        if (Objects.equals(usuarioLogado().getId(), alvo.getId())) {
            throw new AccessDeniedException("Você não pode " + acao + ".");
        }
    }

    /** Valida se o usuário logado pode conceder esta role. */
    public void validarConcessaoRole(String nomeRole) {
        Usuario logado = usuarioLogado();
        if (RolesEnum.nivelDe(nomeRole) > nivel(logado)) {
            throw new AccessDeniedException("Você não pode conceder uma role acima do seu nível de acesso ("
                    + nomeRole(logado) + ").");
        }
    }

    public Long instituicaoId(Usuario usuario) {
        return usuario.getInstituicao() != null ? usuario.getInstituicao().getId() : null;
    }

    private String nomeRole(Usuario usuario) {
        return usuario.getRole() != null ? usuario.getRole().getNome() : null;
    }
}
