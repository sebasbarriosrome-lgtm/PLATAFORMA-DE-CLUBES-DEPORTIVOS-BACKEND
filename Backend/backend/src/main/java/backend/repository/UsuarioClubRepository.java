package backend.repository;

import backend.entity.Usuario;
import backend.entity.UsuarioClub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UsuarioClubRepository extends JpaRepository<UsuarioClub, Long> {

    @Query("SELECT uc FROM UsuarioClub uc WHERE uc.usuarioId = (SELECT u.id FROM Usuario u WHERE u.email = :email)")
    List<UsuarioClub> findByUsuarioEmail(@Param("email") String email);

    @Query("SELECT uc FROM UsuarioClub uc WHERE uc.usuarioId = (SELECT u.id FROM Usuario u WHERE u.email = :email) LIMIT 1")
    Optional<UsuarioClub> findFirstByUsuarioEmail(@Param("email") String email);

    @Query(value = "SELECT uc.id, c.nombre AS clubName, uc.rol, uc.estado, uc.created_at " +
            "FROM usuario_club uc " +
            "JOIN club c ON c.id = uc.club_id " +
            "WHERE uc.usuario_id = (SELECT id FROM usuario WHERE email = :email)", nativeQuery = true)
    List<Object[]> findInvitacionesByUsuarioEmail(@Param("email") String email);

    @Modifying
    @Transactional
    @Query(value = "UPDATE usuario_club SET estado = :estado " +
            "WHERE id = :id AND usuario_id = (SELECT id FROM usuario WHERE email = :email) " +
            "AND estado = 'pendiente'", nativeQuery = true)
    int actualizarEstadoInvitacion(@Param("id") Long id,
            @Param("email") String email,
            @Param("estado") String estado);
}
