package backend.repository;

import backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long>, UsuarioRepositoryCustom {
}