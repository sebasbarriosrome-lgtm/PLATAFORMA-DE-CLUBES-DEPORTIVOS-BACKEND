package backend.repository;

import backend.entity.PerfilUsuario;
import backend.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilUsuarioRepository
        extends JpaRepository<PerfilUsuario, Long> {

    Optional<PerfilUsuario> findByUsuario(Usuario usuario);
}