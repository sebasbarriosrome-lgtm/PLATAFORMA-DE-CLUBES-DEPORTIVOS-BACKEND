package backend.repository;

import backend.entity.Invitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface InvitacionRepository extends JpaRepository<Invitacion, Long> {

    @Query(value = "SELECT i.id, c.nombre AS clubName, i.rol, i.estado, i.created_at " +
            "FROM invitacion i " +
            "JOIN club c ON c.id = i.club_id " +
            "JOIN usuario u ON u.id = i.usuario_id " +
            "WHERE u.email = :email", nativeQuery = true)
    List<Object[]> findInvitacionesByUsuarioEmail(@Param("email") String email);

    @Query("SELECT i FROM Invitacion i WHERE i.id = :id AND i.usuarioId = (SELECT u.id FROM Usuario u WHERE u.email = :email)")
    Optional<Invitacion> findByIdAndUsuarioEmail(@Param("id") Long id, @Param("email") String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM Invitacion i WHERE i.id = :id AND i.usuarioId = (SELECT u.id FROM Usuario u WHERE u.email = :email)")
    int deleteByIdAndUsuarioEmail(@Param("id") Long id, @Param("email") String email);
}
