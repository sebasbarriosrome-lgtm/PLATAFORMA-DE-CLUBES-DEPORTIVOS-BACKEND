package backend.service;

import backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EntrenadorService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UsuarioRepository usuarioRepository;

    public EntrenadorService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // ─────────────────────────────────────────────
    // Helper: obtener entrenador_id desde el email
    // ─────────────────────────────────────────────
    private Long getEntrenadorId(String email) {
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Una sola columna → JPA devuelve List<Object>, NO List<Object[]>
        List<?> rows = entityManager
                .createNativeQuery(
                        "SELECT e.id FROM entrenador e " +
                        "JOIN usuario_club uc ON uc.id = e.usuario_club_id " +
                        "WHERE uc.usuario_id = ? AND uc.estado = 'activo' LIMIT 1")
                .setParameter(1, usuario.getId())
                .getResultList();

        if (rows.isEmpty()) throw new RuntimeException("No se encontró el entrenador");
        return ((Number) rows.get(0)).longValue();
    }

    // ─────────────────────────────────────────────
    // PANEL (existente, sin cambios)
    // ─────────────────────────────────────────────
    public Map<String, Object> getPanelEntrenador(String email) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Long usuarioId = usuario.getId();

        List<Object[]> grupos = entityManager
                .createNativeQuery(
                        "SELECT g.id, g.nombre, g.club_id FROM grupo_entrenador ge JOIN grupo_deportivo g ON ge.grupo_id = g.id WHERE ge.entrenador_id IN (SELECT e.id FROM entrenador e WHERE e.usuario_club_id IN (SELECT uc.id FROM usuario_club uc WHERE uc.usuario_id = ?))")
                .setParameter(1, usuarioId)
                .getResultList();

        List<Map<String, Object>> gruposResponse = new ArrayList<>();

        for (Object[] g : grupos) {
            Long grupoId = ((Number) g[0]).longValue();
            String nombre = (String) g[1];

            Map<String, Object> grupoMap = new HashMap<>();
            grupoMap.put("id", grupoId);
            grupoMap.put("nombre", nombre);

            List<Object[]> atletas = entityManager
                    .createNativeQuery(
                            "SELECT u.id, u.nombre, u.apellido FROM deportista d JOIN usuario_club uc ON uc.id = d.usuario_club_id JOIN usuario u ON u.id = uc.usuario_id WHERE d.grupo_id = ?")
                    .setParameter(1, grupoId)
                    .getResultList();

            List<Map<String, Object>> atletasResp = new ArrayList<>();
            for (Object[] a : atletas) {
                Map<String, Object> atleta = new HashMap<>();
                atleta.put("id", ((Number) a[0]).longValue());
                atleta.put("nombre", a[1] + " " + a[2]);
                atletasResp.add(atleta);
            }
            grupoMap.put("deportistas", atletasResp);

            List<Object[]> horarios = entityManager
                    .createNativeQuery(
                            "SELECT he.id, he.dia_semana, TIME_FORMAT(he.hora_inicio, '%H:%i') AS hora_inicio, " +
                            "TIME_FORMAT(he.hora_fin, '%H:%i') AS hora_fin, he.descripcion, he.ubicacion, he.activo " +
                            "FROM horario_entrenamiento he " +
                            "WHERE he.grupo_id = ? AND he.activo = TRUE AND he.deleted_at IS NULL " +
                            "ORDER BY FIELD(he.dia_semana, 'lunes','martes','miercoles','jueves','viernes','sabado','domingo'), he.hora_inicio")
                    .setParameter(1, grupoId)
                    .getResultList();

            List<Map<String, Object>> horariosResp = new ArrayList<>();
            for (Object[] h : horarios) {
                Map<String, Object> horario = new HashMap<>();
                horario.put("id", h[0]);
                horario.put("dia", h[1]);
                horario.put("horaInicio", h[2]);
                horario.put("horaFin", h[3]);
                horario.put("descripcion", h[4]);
                horario.put("ubicacion", h[5]);
                horario.put("estado", h[6]);
                horariosResp.add(horario);
            }
            grupoMap.put("horarios", horariosResp);

            gruposResponse.add(grupoMap);
        }

        List<Object[]> categorias = entityManager
                .createNativeQuery(
                        "SELECT DISTINCT c.id, c.nombre, c.descripcion " +
                        "FROM categoria c " +
                        "JOIN entrenador_categoria ec ON ec.categoria_id = c.id " +
                        "JOIN entrenador e ON ec.entrenador_id = e.id " +
                        "JOIN usuario_club uc ON uc.id = e.usuario_club_id " +
                        "WHERE uc.usuario_id = ?")
                .setParameter(1, usuarioId)
                .getResultList();

        List<Map<String, Object>> categoriasResponse = new ArrayList<>();
        for (Object[] c : categorias) {
            Map<String, Object> categoriaMap = new HashMap<>();
            categoriaMap.put("id", ((Number) c[0]).longValue());
            categoriaMap.put("nombre", c[1]);
            categoriaMap.put("descripcion", c[2]);
            categoriasResponse.add(categoriaMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("grupos", gruposResponse);
        response.put("categorias", categoriasResponse);
        response.put("coachEmail", email);

        return response;
    }

    // ─────────────────────────────────────────────
    // SESIONES
    // ─────────────────────────────────────────────

    public List<Map<String, Object>> getSesiones(String email) {
        Long entrenadorId = getEntrenadorId(email);

        List<Object[]> rows = entityManager
                .createNativeQuery(
                        "SELECT s.id, s.fecha, " +
                        "TIME_FORMAT(s.hora_inicio, '%H:%i') AS hora_inicio, " +
                        "TIME_FORMAT(s.hora_fin, '%H:%i') AS hora_fin, " +
                        "s.estado, s.descripcion, " +
                        "g.nombre AS grupo_nombre, g.id AS grupo_id " +
                        "FROM sesion_entrenamiento s " +
                        "JOIN grupo_deportivo g ON g.id = s.grupo_id " +
                        "WHERE s.entrenador_id = ? AND s.deleted_at IS NULL " +
                        "ORDER BY s.fecha DESC, s.hora_inicio DESC")
                .setParameter(1, entrenadorId)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> s = new HashMap<>();
            s.put("id",           ((Number) row[0]).longValue());
            s.put("fecha",        row[1] != null ? row[1].toString() : null);
            s.put("horaInicio",   row[2]);
            s.put("horaFin",      row[3]);
            s.put("estado",       row[4]);
            s.put("descripcion",  row[5]);
            s.put("grupoNombre",  row[6]);
            s.put("grupoId",      row[7] != null ? ((Number) row[7]).longValue() : null);
            result.add(s);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> crearSesion(String email, Map<String, Object> body) {
        Long entrenadorId = getEntrenadorId(email);

        Long grupoId       = Long.valueOf(body.get("grupoId").toString());
        String fecha       = body.get("fecha").toString();
        String horaInicio  = body.getOrDefault("horaInicio", "00:00").toString();
        String horaFin     = body.getOrDefault("horaFin", "00:00").toString();
        String descripcion = body.getOrDefault("descripcion", "").toString();

        // Insertar directamente sin CALL para evitar problemas con múltiples result sets
        entityManager.createNativeQuery(
                "INSERT INTO sesion_entrenamiento " +
                "(grupo_id, entrenador_id, fecha, hora_inicio, hora_fin, estado, descripcion, created_at) " +
                "VALUES (?, ?, ?, ?, ?, 'programada', ?, CURRENT_TIMESTAMP)")
                .setParameter(1, grupoId)
                .setParameter(2, entrenadorId)
                .setParameter(3, fecha)
                .setParameter(4, horaInicio.isEmpty() ? null : horaInicio)
                .setParameter(5, horaFin.isEmpty()    ? null : horaFin)
                .setParameter(6, descripcion)
                .executeUpdate();

        // Obtener el id generado
        List<?> idRows = entityManager
                .createNativeQuery("SELECT LAST_INSERT_ID()")
                .getResultList();
        Long newId = idRows.isEmpty() ? null : ((Number) idRows.get(0)).longValue();

        Map<String, Object> result = new HashMap<>();
        result.put("sesionId", newId);
        result.put("message", "Sesión creada correctamente");
        return result;
    }

    @Transactional
    public Map<String, Object> actualizarEstadoSesion(Long sesionId, String estado) {
        entityManager
                .createNativeQuery(
                        "UPDATE sesion_entrenamiento SET estado = ? WHERE id = ? AND deleted_at IS NULL")
                .setParameter(1, estado)
                .setParameter(2, sesionId)
                .executeUpdate();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Estado actualizado");
        return result;
    }

    @Transactional
    public Map<String, Object> eliminarSesion(Long sesionId) {
        entityManager
                .createNativeQuery(
                        "UPDATE sesion_entrenamiento SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
                .setParameter(1, sesionId)
                .executeUpdate();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Sesión eliminada");
        return result;
    }

    // ─────────────────────────────────────────────
    // ACTIVIDADES (catálogo global)
    // ─────────────────────────────────────────────

    public List<Map<String, Object>> getActividades() {
        List<Object[]> rows = entityManager
                .createNativeQuery(
                        "SELECT id, nombre, descripcion FROM actividad " +
                        "WHERE deleted_at IS NULL ORDER BY nombre")
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> a = new HashMap<>();
            a.put("id",          ((Number) row[0]).longValue());
            a.put("nombre",      row[1]);
            a.put("descripcion", row[2]);
            result.add(a);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> crearActividad(Map<String, Object> body) {
        String nombre      = body.get("nombre").toString();
        String descripcion = body.getOrDefault("descripcion", "").toString();

        // Verificar duplicado
        List<?> existe = entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM actividad WHERE LOWER(nombre) = LOWER(?) AND deleted_at IS NULL")
                .setParameter(1, nombre)
                .getResultList();
        if (!existe.isEmpty() && ((Number) existe.get(0)).intValue() > 0) {
            throw new RuntimeException("Ya existe una actividad con ese nombre");
        }

        entityManager.createNativeQuery(
                "INSERT INTO actividad(nombre, descripcion, created_at) VALUES(?, ?, CURRENT_TIMESTAMP)")
                .setParameter(1, nombre)
                .setParameter(2, descripcion)
                .executeUpdate();

        List<?> idRows = entityManager
                .createNativeQuery("SELECT LAST_INSERT_ID()")
                .getResultList();
        Long newId = idRows.isEmpty() ? null : ((Number) idRows.get(0)).longValue();

        Map<String, Object> result = new HashMap<>();
        result.put("actividadId", newId);
        result.put("message", "Actividad creada correctamente");
        return result;
    }

    // ─────────────────────────────────────────────
    // ACTIVIDADES DE UNA SESIÓN
    // ─────────────────────────────────────────────

    public List<Map<String, Object>> getActividadesBySesion(Long sesionId) {
        List<Object[]> rows = entityManager
                .createNativeQuery(
                        "SELECT sa.id, sa.orden, sa.duracion_minutos, sa.descripcion, " +
                        "a.id, a.nombre, a.descripcion " +
                        "FROM sesion_actividad sa " +
                        "JOIN actividad a ON a.id = sa.actividad_id " +
                        "WHERE sa.sesion_id = ? ORDER BY sa.orden")
                .setParameter(1, sesionId)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> sa = new HashMap<>();
            sa.put("sesionActividadId", ((Number) row[0]).longValue());
            sa.put("orden",             row[1]);
            sa.put("duracionMinutos",   row[2]);
            sa.put("nota",              row[3]);
            sa.put("actividadId",       row[4] != null ? ((Number) row[4]).longValue() : null);
            sa.put("actividadNombre",   row[5]);
            sa.put("actividadDesc",     row[6]);
            result.add(sa);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> agregarActividadASesion(Long sesionId, Map<String, Object> body) {
        Long actividadId   = Long.valueOf(body.get("actividadId").toString());
        Integer duracion   = body.containsKey("duracionMinutos") && body.get("duracionMinutos") != null
                             ? Integer.valueOf(body.get("duracionMinutos").toString()) : null;
        String descripcion = body.getOrDefault("descripcion", "").toString();

        // Calcular orden automáticamente
        List<?> ordenRows = entityManager
                .createNativeQuery(
                        "SELECT COALESCE(MAX(orden), 0) + 1 FROM sesion_actividad WHERE sesion_id = ?")
                .setParameter(1, sesionId)
                .getResultList();
        Integer orden = ordenRows.isEmpty() ? 1 : ((Number) ordenRows.get(0)).intValue();

        entityManager.createNativeQuery(
                "INSERT INTO sesion_actividad(sesion_id, actividad_id, orden, duracion_minutos, descripcion) " +
                "VALUES(?, ?, ?, ?, ?)")
                .setParameter(1, sesionId)
                .setParameter(2, actividadId)
                .setParameter(3, orden)
                .setParameter(4, duracion)
                .setParameter(5, descripcion)
                .executeUpdate();

        List<?> idRows = entityManager
                .createNativeQuery("SELECT LAST_INSERT_ID()")
                .getResultList();
        Long newId = idRows.isEmpty() ? null : ((Number) idRows.get(0)).longValue();

        Map<String, Object> result = new HashMap<>();
        result.put("sesionActividadId", newId);
        result.put("message", "Actividad agregada a la sesión");
        return result;
    }

    @Transactional
    public Map<String, Object> quitarActividadDeSesion(Long sesionActividadId) {
        entityManager
                .createNativeQuery("DELETE FROM sesion_actividad WHERE id = ?")
                .setParameter(1, sesionActividadId)
                .executeUpdate();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Actividad eliminada de la sesión");
        return result;
    }

    // ─────────────────────────────────────────────
    // ASISTENCIA
    // ─────────────────────────────────────────────

    public List<Map<String, Object>> getAsistenciaBySesion(Long sesionId) {
        List<Object[]> rows = entityManager
                .createNativeQuery(
                        "SELECT d.id, u.nombre, u.apellido, " +
                        "COALESCE(a.estado, 'ausente') AS estado, a.fecha " +
                        "FROM deportista d " +
                        "JOIN usuario_club uc ON uc.id = d.usuario_club_id " +
                        "JOIN usuario u ON u.id = uc.usuario_id " +
                        "LEFT JOIN asistencia a ON a.deportista_id = d.id AND a.sesion_id = ? " +
                        "WHERE d.grupo_id = (SELECT grupo_id FROM sesion_entrenamiento WHERE id = ?) " +
                        "AND d.deleted_at IS NULL AND uc.estado = 'activo' " +
                        "ORDER BY u.apellido, u.nombre")
                .setParameter(1, sesionId)
                .setParameter(2, sesionId)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> a = new HashMap<>();
            a.put("deportistaId", ((Number) row[0]).longValue());
            a.put("nombre",       row[1] + " " + row[2]);
            a.put("estado",       row[3]);
            a.put("fecha",        row[4] != null ? row[4].toString() : null);
            result.add(a);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> registrarAsistencia(Long sesionId, Map<String, Object> body) {
        Long deportistaId = Long.valueOf(body.get("deportistaId").toString());
        String estado     = body.get("estado").toString();
        String fecha      = body.get("fecha").toString();

        upsertAsistencia(deportistaId, sesionId, estado, fecha);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Asistencia registrada");
        return result;
    }

    @Transactional
    public Map<String, Object> registrarAsistenciaLote(Long sesionId, List<Map<String, Object>> lista) {
        for (Map<String, Object> item : lista) {
            Long deportistaId = Long.valueOf(item.get("deportistaId").toString());
            String estado     = item.get("estado").toString();
            String fecha      = item.get("fecha").toString();
            upsertAsistencia(deportistaId, sesionId, estado, fecha);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Asistencia registrada para " + lista.size() + " deportistas");
        return result;
    }

    private void upsertAsistencia(Long deportistaId, Long sesionId, String estado, String fecha) {
        List<?> existe = entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM asistencia WHERE deportista_id = ? AND sesion_id = ?")
                .setParameter(1, deportistaId)
                .setParameter(2, sesionId)
                .getResultList();

        boolean hayRegistro = !existe.isEmpty() && ((Number) existe.get(0)).intValue() > 0;

        if (hayRegistro) {
            entityManager.createNativeQuery(
                    "UPDATE asistencia SET estado = ?, fecha = ?, created_at = CURRENT_TIMESTAMP " +
                    "WHERE deportista_id = ? AND sesion_id = ?")
                    .setParameter(1, estado)
                    .setParameter(2, fecha)
                    .setParameter(3, deportistaId)
                    .setParameter(4, sesionId)
                    .executeUpdate();
        } else {
            entityManager.createNativeQuery(
                    "INSERT INTO asistencia(deportista_id, sesion_id, estado, fecha, created_at) " +
                    "VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP)")
                    .setParameter(1, deportistaId)
                    .setParameter(2, sesionId)
                    .setParameter(3, estado)
                    .setParameter(4, fecha)
                    .executeUpdate();
        }
    }
}