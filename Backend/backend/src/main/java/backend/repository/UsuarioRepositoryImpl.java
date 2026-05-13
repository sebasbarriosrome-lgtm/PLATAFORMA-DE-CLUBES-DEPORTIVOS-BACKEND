package backend.repository;

import backend.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void registrarUsuario(String nombre,
                                 String apellido,
                                 String email,
                                 String password) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("sp_register_usuario");

        query.registerStoredProcedureParameter("p_nombre", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_apellido", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_email", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_password", String.class, jakarta.persistence.ParameterMode.IN);

        query.setParameter("p_nombre", nombre);
        query.setParameter("p_apellido", apellido);
        query.setParameter("p_email", email);
        query.setParameter("p_password", password);

        query.execute();
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {

        String jpql = "SELECT u FROM Usuario u WHERE u.email = :email";

        try {
            Usuario usuario = entityManager.createQuery(jpql, Usuario.class)
                    .setParameter("email", email)
                    .getSingleResult();

            return Optional.of(usuario);

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}