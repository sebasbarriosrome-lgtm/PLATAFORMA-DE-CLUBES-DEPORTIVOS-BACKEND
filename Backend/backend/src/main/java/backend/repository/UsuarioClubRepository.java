package backend.repository;

import backend.entity.Usuario;
import backend.entity.UsuarioClub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioClubRepository extends JpaRepository<UsuarioClub, Long> {

    @Query("SELECT uc FROM UsuarioClub uc WHERE uc.usuarioId = (SELECT u.id FROM Usuario u WHERE u.email = :email)")
    List<UsuarioClub> findByUsuarioEmail(@Param("email") String email);

    @Query("SELECT uc FROM UsuarioClub uc WHERE uc.usuarioId = (SELECT u.id FROM Usuario u WHERE u.email = :email) LIMIT 1")
    Optional<UsuarioClub> findFirstByUsuarioEmail(@Param("email") String email);
}
