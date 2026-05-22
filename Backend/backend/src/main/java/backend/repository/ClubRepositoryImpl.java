package backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ClubRepositoryImpl implements ClubRepositoryCustom {

        @PersistenceContext
        private EntityManager entityManager;

        @Override
        public void crearClub(
                        String nombre,
                        String ciudad,
                        String descripcion,
                        String logoUrl,
                        String bannerUrl,
                        String colorPrimario,
                        String colorSecundario,
                        String contacto) {

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_create_club");

                query.registerStoredProcedureParameter("p_nombre", String.class, jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_ciudad", String.class, jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_descripcion", String.class,
                                jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_logo_url", String.class,
                                jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_banner_url", String.class,
                                jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_color_primario", String.class,
                                jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_color_secundario", String.class,
                                jakarta.persistence.ParameterMode.IN);
                query.registerStoredProcedureParameter("p_contacto", String.class,
                                jakarta.persistence.ParameterMode.IN);

                query.setParameter("p_nombre", nombre);
                query.setParameter("p_ciudad", ciudad);
                query.setParameter("p_descripcion", descripcion);
                query.setParameter("p_logo_url", logoUrl);
                query.setParameter("p_banner_url", bannerUrl);
                query.setParameter("p_color_primario", colorPrimario);
                query.setParameter("p_color_secundario", colorSecundario);
                query.setParameter("p_contacto", contacto);

                query.execute();
        }

        @Override
        public void crearUsuarioClub(
                        Long usuarioId,
                        Long clubId,
                        String rol) {

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_create_usuario_club");

                query.registerStoredProcedureParameter("p_usuario_id", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_club_id", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_rol", String.class, ParameterMode.IN);

                query.setParameter("p_usuario_id", usuarioId);
                query.setParameter("p_club_id", clubId);
                query.setParameter("p_rol", rol);

                query.execute();
        }

        @Override
        public Object getPanelClubData(Long usuarioId) {

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_get_panel_club");

                query.registerStoredProcedureParameter(
                                "p_usuario_id",
                                Long.class,
                                ParameterMode.IN);

                query.setParameter("p_usuario_id", usuarioId);

                List<?> result = query.getResultList();
                if (result.isEmpty()) {
                        return null;
                }

                Object[] row = (Object[]) result.get(0);

                Map<String, Object> response = new HashMap<>();
                response.put("id", row[0]);
                response.put("nombre", row[1]);
                response.put("logo", row[2]);
                response.put("banner", row[3]);
                response.put("descripcion", row[4]);
                response.put("colorPrimario", row[5]);
                response.put("colorSecundario", row[6]);
                response.put("adminNombre", row[7]);
                response.put("adminFoto", row[8]);

                return response;

        }

        @Override
        public void actualizarClub(
                        Long clubId,
                        String descripcion,
                        String logoUrl,
                        String bannerUrl,
                        String colorPrimario,
                        String colorSecundario) {

                StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_update_club");

                query.registerStoredProcedureParameter("p_id", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_descripcion", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_logo_url", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_banner_url", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_color_primario", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("p_color_secundario", String.class, ParameterMode.IN);

                query.setParameter("p_id", clubId);
                query.setParameter("p_descripcion", descripcion);
                query.setParameter("p_logo_url", logoUrl);
                query.setParameter("p_banner_url", bannerUrl);
                query.setParameter("p_color_primario", colorPrimario);
                query.setParameter("p_color_secundario", colorSecundario);

                query.execute();
        }

        @Override
        public Long crearSolicitud(
                        Long usuarioId,
                        Long clubId,
                        String rol,
                        String mensaje) {

                Object result = entityManager
                                .createNativeQuery("CALL sp_create_solicitud(?,?,?,?)")
                                .setParameter(1, usuarioId)
                                .setParameter(2, clubId)
                                .setParameter(3, rol)
                                .setParameter(4, mensaje)
                                .getSingleResult();

                return ((Number) result).longValue();
        }

        @Override
        public void crearSolicitudDeportiva(
                        Long solicitudId,
                        Double peso,
                        Long estatura,
                        String experiencia,
                        String especialidad) {

                entityManager
                                .createNativeQuery("CALL sp_create_solicitud_deportiva(?,?,?,?,?)")
                                .setParameter(1, solicitudId)
                                .setParameter(2, peso)
                                .setParameter(3, estatura)
                                .setParameter(4, experiencia)
                                .setParameter(5, especialidad)
                                .executeUpdate();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Object[]> getSolicitudesByClub(Long clubId) {

                return (List<Object[]>) entityManager
                                .createNativeQuery("CALL sp_get_solicitudes_by_club(?)")
                                .setParameter(1, clubId)
                                .getResultList();
        }

        @Override
        public void actualizarEstadoSolicitud(Long solicitudId, String estado) {

                entityManager
                                .createNativeQuery("CALL sp_update_solicitud_estado(?,?)")
                                .setParameter(1, solicitudId)
                                .setParameter(2, estado)
                                .executeUpdate();
        }

}