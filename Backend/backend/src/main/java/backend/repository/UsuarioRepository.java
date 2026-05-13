package backend.repository;

import backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Procedure(procedureName = "sp_register_usuario")
    void registrarUsuario(
            @Param("p_nombre") String nombre,
            @Param("p_apellido") String apellido,
            @Param("p_email") String email,
            @Param("p_password") String password
    );

    @Query("""
        SELECT u
        FROM Usuario u
        WHERE u.email = :email
        AND u.password = :password
    """)
    Usuario login(
            @Param("email") String email,
            @Param("password") String password
    );
}