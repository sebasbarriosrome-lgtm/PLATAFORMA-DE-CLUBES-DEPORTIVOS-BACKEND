package backend.service;

import backend.entity.Club;
import backend.entity.Usuario;
import backend.repository.ClubRepository;
import backend.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class ClubService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ClubRepository clubRepository;
    private final UsuarioRepository usuarioRepository;

    public ClubService(
            ClubRepository clubRepository,
            UsuarioRepository usuarioRepository) {
        this.clubRepository = clubRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional // ✅ IMPORTANTE
    public void crearClub(
            String nombre,
            String ciudad,
            String descripcion,
            String logoUrl,
            String bannerUrl,
            String colorPrimario,
            String colorSecundario,
            String contacto,
            String email) {

        // ✅ 1. CREAR CLUB (SP)
        clubRepository.crearClub(
                nombre,
                ciudad,
                descripcion,
                logoUrl,
                bannerUrl,
                colorPrimario,
                colorSecundario,
                contacto);

        // ✅ 2. OBTENER USUARIO
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ 3. OBTENER ID DEL ÚLTIMO CLUB (IMPORTANTE)
        Long clubId = ((Number) entityManager
                .createNativeQuery("SELECT id FROM club ORDER BY id DESC LIMIT 1")
                .getSingleResult()).longValue();

        // ✅ 4. ASIGNAR COMO ADMIN
        clubRepository.crearUsuarioClub(
                usuario.getId(),
                clubId,
                "admin");
    }

    public List<Club> getAllClubs() {
        return clubRepository.findByDeletedAtIsNull();
    }

    public Map<String, Object> getPanelClub(String email) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<?> resultList = entityManager
                .createNativeQuery("CALL sp_get_panel_club(?)")
                .setParameter(1, usuario.getId())
                .getResultList();

        // ✅ CASO: NO TIENE CLUB
        if (resultList.isEmpty()) {
            return null;
        }

        Object[] result = (Object[]) resultList.get(0);

        Map<String, Object> response = new HashMap<>();

        response.put("clubId", result[0]);
        response.put("clubNombre", result[1]);
        response.put("clubLogo", result[2]);

        response.put("banner", result[3]);
        response.put("descripcion", result[4]);
        response.put("colorPrimario", result[5]);
        response.put("colorSecundario", result[6]);

        response.put("adminNombre", result[7]);
        response.put("adminFoto", result[8]);

        return response;
    }

    public void actualizarClub(
            String email,
            String descripcion,
            String logoUrl,
            String bannerUrl,
            String colorPrimario,
            String colorSecundario) {

        // ✅ obtener usuario
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ obtener club del usuario
        Map<String, Object> panel = (Map<String, Object>) clubRepository.getPanelClubData(usuario.getId());

        if (panel == null || panel.get("id") == null) {
            throw new RuntimeException("Club no encontrado para el usuario");
        }

        Long clubId = ((Number) panel.get("id")).longValue();

        // ✅ actualizar
        clubRepository.actualizarClub(
                clubId,
                descripcion,
                logoUrl,
                bannerUrl,
                colorPrimario,
                colorSecundario);
    }

    public Club getClubBySlug(String slug) {
        return clubRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Club no encontrado"));
    }

    @Transactional
    public void crearSolicitud(
            String email,
            Long clubId,
            String rol,
            String mensaje,
            Double peso,
            Long estatura,
            String experiencia,
            String especialidad) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ 1. crear solicitud
        Long solicitudId = clubRepository.crearSolicitud(
                usuario.getId(),
                clubId,
                rol,
                mensaje);

        // ✅ 2. separar por rol 🔥
        if ("deportista".equals(rol)) {

            clubRepository.crearSolicitudDeportiva(
                    solicitudId,
                    peso,
                    estatura,
                    null,
                    null);

        } else if ("entrenador".equals(rol)) {

            clubRepository.crearSolicitudDeportiva(
                    solicitudId,
                    null,
                    null,
                    experiencia,
                    especialidad);
        }
    }

    public List<Map<String, Object>> getSolicitudesClub(String email) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ obtener club del usuario
        Object[] panel = (Object[]) clubRepository.getPanelClubData(usuario.getId());

        Long clubId = ((Number) panel[0]).longValue();

        List<Object[]> results = clubRepository.getSolicitudesByClub(clubId);

        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] row : results) {

            Map<String, Object> item = new HashMap<>();

            item.put("id", row[0]);
            item.put("rol", row[1]);
            item.put("estado", row[2]);
            item.put("mensaje", row[3]);
            item.put("nombre", row[4]);
            item.put("edad", row[5]);
            item.put("peso", row[6]);
            item.put("estatura", row[7]);
            item.put("experiencia", row[8]);
            item.put("especialidad", row[9]);

            response.add(item);
        }

        return response;
    }

    @Transactional
    public void resolverSolicitud(Long solicitudId, String accion, String email) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ 🔒 VALIDAR QUE ES ADMIN DEL CLUB
        Number adminCount = (Number) entityManager.createNativeQuery("""
                    SELECT COUNT(*) FROM usuario_club
                    WHERE usuario_id = ? AND rol = 'admin'
                """)
                .setParameter(1, usuario.getId())
                .getSingleResult();

        boolean esAdmin = adminCount != null && adminCount.longValue() > 0;

        if (!esAdmin) {
            throw new RuntimeException("No tienes permisos para aceptar solicitudes");
        }

        // ✅ obtener datos de solicitud
        Object[] solicitud = (Object[]) entityManager
                .createNativeQuery("SELECT usuario_id, club_id, rol_solicitado FROM solicitud WHERE id = ?")
                .setParameter(1, solicitudId)
                .getSingleResult();

        Long usuarioId = ((Number) solicitud[0]).longValue();
        Long clubId = ((Number) solicitud[1]).longValue();
        String rol = (String) solicitud[2];

        // ✅ si rechaza - ELIMINAR solicitudes
        if ("rechazado".equals(accion)) {
            entityManager.createNativeQuery(
                    "DELETE FROM solicitud_deportiva WHERE solicitud_id = ?")
                    .setParameter(1, solicitudId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                    "DELETE FROM solicitud WHERE id = ?")
                    .setParameter(1, solicitudId)
                    .executeUpdate();
            return;
        }

        // ✅ aceptar
        clubRepository.crearUsuarioClub(usuarioId, clubId, rol);

        if ("deportista".equals(rol)) {

            entityManager.createNativeQuery("""
                        INSERT INTO deportista (usuario_club_id, peso, estatura, created_at)
                        VALUES (
                            (SELECT id FROM usuario_club WHERE usuario_id = ? AND club_id = ?),
                            (SELECT peso FROM solicitud_deportiva WHERE solicitud_id = ?),
                            (SELECT estatura FROM solicitud_deportiva WHERE solicitud_id = ?),
                            CURRENT_TIMESTAMP
                        )
                    """)
                    .setParameter(1, usuarioId)
                    .setParameter(2, clubId)
                    .setParameter(3, solicitudId)
                    .setParameter(4, solicitudId)
                    .executeUpdate();

        } else if ("entrenador".equals(rol)) {

            entityManager.createNativeQuery("""
                        INSERT INTO entrenador (usuario_club_id, experiencia, especialidad, created_at)
                        VALUES (
                            (SELECT id FROM usuario_club WHERE usuario_id = ? AND club_id = ?),
                            (SELECT experiencia FROM solicitud_deportiva WHERE solicitud_id = ?),
                            (SELECT especialidad FROM solicitud_deportiva WHERE solicitud_id = ?),
                            CURRENT_TIMESTAMP
                        )
                    """)
                    .setParameter(1, usuarioId)
                    .setParameter(2, clubId)
                    .setParameter(3, solicitudId)
                    .setParameter(4, solicitudId)
                    .executeUpdate();
        }

        // ✅ ELIMINAR solicitudes después de aceptar
        entityManager.createNativeQuery(
                "DELETE FROM solicitud_deportiva WHERE solicitud_id = ?")
                .setParameter(1, solicitudId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM solicitud WHERE id = ?")
                .setParameter(1, solicitudId)
                .executeUpdate();
    }

    public List<Map<String, Object>> getSolicitudesPorRol(String email, String rol) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ obtener club del admin
        Map<String, Object> panel = (Map<String, Object>) clubRepository.getPanelClubData(usuario.getId());

        if (panel == null) {
            throw new RuntimeException("No tiene club");
        }

        Long clubId = ((Number) panel.get("id")).longValue();

        // ✅ traer todas las solicitudes
        List<Object[]> results = clubRepository.getSolicitudesByClub(clubId);

        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] row : results) {

            // ✅ FILTRO 🔥
            if (!rol.equals(row[1])) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();

            item.put("id", row[0]);
            item.put("rol", row[1]);
            item.put("estado", row[2]);
            item.put("mensaje", row[3]);
            item.put("nombre", row[4]);
            item.put("edad", row[5]);
            item.put("peso", row[6]);
            item.put("estatura", row[7]);
            item.put("experiencia", row[8]);
            item.put("especialidad", row[9]);

            response.add(item);
        }

        return response;
    }

    public List<Map<String, Object>> getEntrenadoresClub(String email) {
        Long clubId = obtenerClubIdDelAdmin(email);

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager
                .createNativeQuery(
                        """
                                SELECT e.id, u.id, u.nombre, u.apellido, u.email, uc.id, e.experiencia, e.especialidad, uc.estado, e.created_at
                                FROM entrenador e
                                JOIN usuario_club uc ON uc.id = e.usuario_club_id
                                JOIN usuario u ON u.id = uc.usuario_id
                                WHERE uc.club_id = ? AND uc.rol = 'entrenador' AND e.deleted_at IS NULL
                                """)
                .setParameter(1, clubId)
                .getResultList();

        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("entrenadorId", ((Number) row[0]).longValue());
            item.put("usuarioId", ((Number) row[1]).longValue());
            item.put("nombre", row[2]);
            item.put("apellido", row[3]);
            item.put("email", row[4]);
            item.put("usuarioClubId", ((Number) row[5]).longValue());
            item.put("experiencia", row[6]);
            item.put("especialidad", row[7]);
            item.put("estado", row[8]);
            item.put("createdAt", row[9]);
            response.add(item);
        }

        return response;
    }

    @Transactional
    public void eliminarEntrenador(Long entrenadorId, String email) {
        Long clubId = obtenerClubIdDelAdmin(email);

        Object[] result = (Object[]) entityManager.createNativeQuery("""
                SELECT e.usuario_club_id, uc.club_id
                FROM entrenador e
                JOIN usuario_club uc ON uc.id = e.usuario_club_id
                WHERE e.id = ? AND e.deleted_at IS NULL
                """)
                .setParameter(1, entrenadorId)
                .getSingleResult();

        Long usuarioClubId = ((Number) result[0]).longValue();
        Long entrenadorClubId = ((Number) result[1]).longValue();

        if (!clubId.equals(entrenadorClubId)) {
            throw new RuntimeException("El entrenador no pertenece a tu club");
        }

        entityManager.createNativeQuery("DELETE FROM entrenador WHERE id = ?")
                .setParameter(1, entrenadorId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM usuario_club WHERE id = ?")
                .setParameter(1, usuarioClubId)
                .executeUpdate();
    }

    public List<Map<String, Object>> getDeportistasClub(String email) {
        Long clubId = obtenerClubIdDelAdmin(email);

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager
                .createNativeQuery(
                        """
                                SELECT d.id, u.id, u.nombre, u.apellido, u.email, uc.id, d.peso, d.estatura, d.categoria_id, d.grupo_id, uc.estado, d.created_at
                                FROM deportista d
                                JOIN usuario_club uc ON uc.id = d.usuario_club_id
                                JOIN usuario u ON u.id = uc.usuario_id
                                WHERE uc.club_id = ? AND uc.rol = 'deportista' AND d.deleted_at IS NULL
                                """)
                .setParameter(1, clubId)
                .getResultList();

        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("deportistaId", ((Number) row[0]).longValue());
            item.put("usuarioId", ((Number) row[1]).longValue());
            item.put("nombre", row[2]);
            item.put("apellido", row[3]);
            item.put("email", row[4]);
            item.put("usuarioClubId", ((Number) row[5]).longValue());
            item.put("peso", row[6]);
            item.put("estatura", row[7]);
            item.put("categoriaId", row[8]);
            item.put("grupoId", row[9]);
            item.put("estado", row[10]);
            item.put("createdAt", row[11]);
            response.add(item);
        }

        return response;
    }

    @Transactional
    public void eliminarDeportista(Long deportistaId, String email) {
        Long clubId = obtenerClubIdDelAdmin(email);

        Object[] result = (Object[]) entityManager.createNativeQuery("""
                SELECT d.usuario_club_id, uc.club_id
                FROM deportista d
                JOIN usuario_club uc ON uc.id = d.usuario_club_id
                WHERE d.id = ? AND d.deleted_at IS NULL
                """)
                .setParameter(1, deportistaId)
                .getSingleResult();

        Long usuarioClubId = ((Number) result[0]).longValue();
        Long deportistaClubId = ((Number) result[1]).longValue();

        if (!clubId.equals(deportistaClubId)) {
            throw new RuntimeException("El deportista no pertenece a tu club");
        }

        entityManager.createNativeQuery("DELETE FROM deportista WHERE id = ?")
                .setParameter(1, deportistaId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM usuario_club WHERE id = ?")
                .setParameter(1, usuarioClubId)
                .executeUpdate();
    }

    public List<Map<String, Object>> getHorariosClub(String email) {

        Long clubId = obtenerClubIdDelAdmin(email);
        List<Object[]> results = clubRepository.getHorariosByClubId(clubId);

        return mapHorarios(results);
    }

    public List<Map<String, Object>> getHorariosClubBySlug(String slug) {

        List<Object[]> results = clubRepository.getHorariosByClubSlug(slug);
        return mapHorarios(results);
    }

    @Transactional
    public Long crearHorario(
            String email,
            String dia,
            String horaInicio,
            String horaFin,
            String descripcion,
            String ubicacion) {

        Long clubId = obtenerClubIdDelAdmin(email);

        return clubRepository.crearHorarioEntrenamiento(
                clubId,
                dia,
                horaInicio,
                horaFin,
                descripcion,
                ubicacion);
    }

    @Transactional
    public void actualizarHorario(
            Long horarioId,
            String email,
            String dia,
            String horaInicio,
            String horaFin,
            String descripcion,
            String ubicacion) {

        Long clubId = obtenerClubIdDelAdmin(email);

        Long count = ((Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM horario_entrenamiento WHERE id = ? AND club_id = ? AND deleted_at IS NULL")
                .setParameter(1, horarioId)
                .setParameter(2, clubId)
                .getSingleResult()).longValue();

        if (count == 0) {
            throw new RuntimeException("Horario no encontrado o no pertenece al club");
        }

        clubRepository.actualizarHorarioEntrenamiento(
                horarioId,
                dia,
                horaInicio,
                horaFin,
                descripcion,
                ubicacion);
    }

    @Transactional
    public void eliminarHorario(Long horarioId, String email) {

        Long clubId = obtenerClubIdDelAdmin(email);

        Long count = ((Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM horario_entrenamiento WHERE id = ? AND club_id = ? AND deleted_at IS NULL")
                .setParameter(1, horarioId)
                .setParameter(2, clubId)
                .getSingleResult()).longValue();

        if (count == 0) {
            throw new RuntimeException("Horario no encontrado o no pertenece al club");
        }

        clubRepository.eliminarHorarioEntrenamiento(horarioId);
    }

    private Long obtenerClubIdDelAdmin(String email) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Map<String, Object> panel = (Map<String, Object>) clubRepository.getPanelClubData(usuario.getId());

        if (panel == null) {
            throw new RuntimeException("No tiene club");
        }

        // El repositorio devuelve el mapa con la clave 'id'
        return ((Number) panel.get("id")).longValue();
    }

    private List<Map<String, Object>> mapHorarios(List<Object[]> results) {

        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("dia", row[1]);
            item.put("horaInicio", row[2]);
            item.put("horaFin", row[3]);
            item.put("descripcion", row[4]);
            item.put("ubicacion", row[5]);
            item.put("estado", row[6]);
            response.add(item);
        }

        return response;
    }

    public Club getClubById(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Club no encontrado"));
    }
}
