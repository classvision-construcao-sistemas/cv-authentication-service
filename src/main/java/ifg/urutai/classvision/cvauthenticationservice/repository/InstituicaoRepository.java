package ifg.urutai.classvision.cvauthenticationservice.repository;

import ifg.urutai.classvision.cvauthenticationservice.model.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> {
    Optional<Instituicao> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
}
