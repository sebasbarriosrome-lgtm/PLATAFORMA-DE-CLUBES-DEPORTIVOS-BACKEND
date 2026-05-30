package backend.repository;

import backend.entity.PerfilUsuario;
import backend.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.Optional;
import java.util.List;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepositoryCustom {

        @PersistenceContext
        private EntityManager entityManager;

        @Autowired
        private PerfilUsuarioRepository perfilUsuarioRepository;

        @Override
        public void registrarUsuario(String nombre,
                        String apellido,
                        String email,
                        String password) {

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_register_usuario");

                query.registerStoredProcedureParameter("p_nombre", String.class, jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_apellido", String.class,
                                jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_email", String.class, jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_password", String.class,
                                jakarta.persistence.ParameterMode.IN);

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

        @Transactional
        @Override
        public Usuario actualizarPerfil(
                        String emailActual,
                        String nombre,
                        String apellido,
                        String email,
                        String telefono,
                        String birthDate,
                        String photoUrl) {

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery(
                                "sp_update_usuario");

                query.registerStoredProcedureParameter(
                                "p_email_actual",
                                String.class,
                                ParameterMode.IN);

                query.registerStoredProcedureParameter(
                                "p_nombre",
                                String.class,
                                ParameterMode.IN);

                query.registerStoredProcedureParameter(
                                "p_apellido",
                                String.class,
                                ParameterMode.IN);

                query.registerStoredProcedureParameter(
                                "p_email",
                                String.class,
                                ParameterMode.IN);

                query.registerStoredProcedureParameter(
                                "p_telefono",
                                String.class,
                                ParameterMode.IN);

                query.registerStoredProcedureParameter(
                                "p_birthDate",
                                Date.class,
                                ParameterMode.IN);

                query.registerStoredProcedureParameter(
                                "p_photoUrl",
                                String.class,
                                ParameterMode.IN);

                query.setParameter("p_email_actual", emailActual);
                query.setParameter("p_nombre", nombre);
                query.setParameter("p_apellido", apellido);
                query.setParameter("p_email", email);
                query.setParameter("p_telefono", telefono);
                Date birthDateValue = birthDate == null || birthDate.isBlank()
                                ? null
                                : Date.valueOf(birthDate);
                query.setParameter("p_birthDate", birthDateValue);
                query.setParameter("p_photoUrl", photoUrl);

                query.execute();
                entityManager.flush();

                // Buscar por el email final (nuevo email si cambió, o email actual)
                String emailFinal = (email != null && !email.isBlank()) ? email : emailActual;

                return buscarPorEmail(emailFinal)
                                .orElseThrow(() -> new RuntimeException(
                                                "Usuario no encontrado después de actualizar perfil"));
        }
}