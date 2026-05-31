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

    // ─────────────────────────────────────────────
// MÉTRICAS
// ─────────────────────────────────────────────

public List<Map<String, Object>> getMetricasBySesion(Long sesionId) {
    List<Object[]> rows = entityManager
            .createNativeQuery(
                    "SELECT ms.id, ms.deportista_id, " +
                    "u.nombre, u.apellido, " +
                    "a.nombre AS actividad_nombre, " +
                    "ms.tiempo, ms.distancia, ms.velocidad, " +
                    "ms.tecnica, ms.rendimiento_fisico, ms.observaciones, " +
                    "ms.sesion_actividad_id, ms.created_at " +
                    "FROM metricas_sesion ms " +
                    "JOIN deportista d ON d.id = ms.deportista_id " +
                    "JOIN usuario_club uc ON uc.id = d.usuario_club_id " +
                    "JOIN usuario u ON u.id = uc.usuario_id " +
                    "JOIN sesion_actividad sa ON sa.id = ms.sesion_actividad_id " +
                    "JOIN actividad a ON a.id = sa.actividad_id " +
                    "WHERE ms.sesion_id = ? AND ms.deleted_at IS NULL " +
                    "ORDER BY ms.created_at DESC")
            .setParameter(1, sesionId)
            .getResultList();

    List<Map<String, Object>> result = new ArrayList<>();
    for (Object[] row : rows) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",               ((Number) row[0]).longValue());
        m.put("deportistaId",     ((Number) row[1]).longValue());
        m.put("deportistaNombre", row[2] + " " + row[3]);
        m.put("actividadNombre",  row[4]);
        m.put("tiempo",           row[5]);
        m.put("distancia",        row[6]);
        m.put("velocidad",        row[7]);
        m.put("tecnica",          row[8]);
        m.put("rendimientoFisico",row[9]);
        m.put("observaciones",    row[10]);
        m.put("sesionActividadId",row[11] != null ? ((Number) row[11]).longValue() : null);
        m.put("createdAt",        row[12] != null ? row[12].toString() : null);
        result.add(m);
    }
    return result;
}

public List<Map<String, Object>> getMetricasByDeportista(Long deportistaId) {
    List<Object[]> rows = entityManager
            .createNativeQuery(
                    "SELECT ms.id, s.fecha, g.nombre AS grupo_nombre, " +
                    "a.nombre AS actividad_nombre, " +
                    "ms.tiempo, ms.distancia, ms.velocidad, " +
                    "ms.tecnica, ms.rendimiento_fisico, ms.observaciones " +
                    "FROM metricas_sesion ms " +
                    "JOIN sesion_entrenamiento s ON s.id = ms.sesion_id " +
                    "JOIN grupo_deportivo g ON g.id = s.grupo_id " +
                    "JOIN sesion_actividad sa ON sa.id = ms.sesion_actividad_id " +
                    "JOIN actividad a ON a.id = sa.actividad_id " +
                    "WHERE ms.deportista_id = ? AND ms.deleted_at IS NULL " +
                    "ORDER BY s.fecha DESC, ms.created_at DESC")
            .setParameter(1, deportistaId)
            .getResultList();

    List<Map<String, Object>> result = new ArrayList<>();
    for (Object[] row : rows) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",               ((Number) row[0]).longValue());
        m.put("fecha",            row[1] != null ? row[1].toString() : null);
        m.put("grupoNombre",      row[2]);
        m.put("actividadNombre",  row[3]);
        m.put("tiempo",           row[4]);
        m.put("distancia",        row[5]);
        m.put("velocidad",        row[6]);
        m.put("tecnica",          row[7]);
        m.put("rendimientoFisico",row[8]);
        m.put("observaciones",    row[9]);
        result.add(m);
    }
    return result;
}

@Transactional
public Map<String, Object> registrarMetrica(String email, Long sesionId, Map<String, Object> body) {
    Long entrenadorId    = getEntrenadorId(email);
    Long deportistaId    = Long.valueOf(body.get("deportistaId").toString());
    Long sesionActividadId = Long.valueOf(body.get("sesionActividadId").toString());

    Object tiempoRaw     = body.get("tiempo");
    Object distanciaRaw  = body.get("distancia");
    Object velocidadRaw  = body.get("velocidad");
    Object tecnicaRaw    = body.get("tecnica");
    Object rendimientoRaw= body.get("rendimientoFisico");
    String observaciones = body.getOrDefault("observaciones", "").toString();

    Double tiempo     = tiempoRaw    != null && !tiempoRaw.toString().isEmpty()
                        ? Double.valueOf(tiempoRaw.toString()) : null;
    Double distancia  = distanciaRaw != null && !distanciaRaw.toString().isEmpty()
                        ? Double.valueOf(distanciaRaw.toString()) : null;
    Double velocidad  = velocidadRaw != null && !velocidadRaw.toString().isEmpty()
                        ? Double.valueOf(velocidadRaw.toString()) : null;
    Integer tecnica   = tecnicaRaw   != null && !tecnicaRaw.toString().isEmpty()
                        ? Integer.valueOf(tecnicaRaw.toString()) : null;
    Integer rendimiento = rendimientoRaw != null && !rendimientoRaw.toString().isEmpty()
                        ? Integer.valueOf(rendimientoRaw.toString()) : null;

    entityManager.createNativeQuery(
            "INSERT INTO metricas_sesion " +
            "(deportista_id, entrenador_id, sesion_id, sesion_actividad_id, " +
            "tiempo, distancia, velocidad, tecnica, rendimiento_fisico, observaciones, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)")
            .setParameter(1, deportistaId)
            .setParameter(2, entrenadorId)
            .setParameter(3, sesionId)
            .setParameter(4, sesionActividadId)
            .setParameter(5, tiempo)
            .setParameter(6, distancia)
            .setParameter(7, velocidad)
            .setParameter(8, tecnica)
            .setParameter(9, rendimiento)
            .setParameter(10, observaciones)
            .executeUpdate();

    List<?> idRows = entityManager
            .createNativeQuery("SELECT LAST_INSERT_ID()")
            .getResultList();
    Long newId = idRows.isEmpty() ? null : ((Number) idRows.get(0)).longValue();

    Map<String, Object> result = new HashMap<>();
    result.put("metricaId", newId);
    result.put("message", "Métrica registrada correctamente");
    return result;
}

@Transactional
public Map<String, Object> eliminarMetrica(Long metricaId) {
    entityManager
            .createNativeQuery(
                    "UPDATE metricas_sesion SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
            .setParameter(1, metricaId)
            .executeUpdate();

    Map<String, Object> result = new HashMap<>();
    result.put("message", "Métrica eliminada");
    return result;
}

// ─────────────────────────────────────────────
// RENDIMIENTO
// ─────────────────────────────────────────────

/** Gráfica 1: Evolución técnica + rendimiento físico por deportista (línea) */
public List<Map<String, Object>> getEvolucionDeportista(Long deportistaId, Long actividadId) {
    String sql =
        "SELECT s.fecha, a.nombre AS actividad, " +
        "AVG(ms.tecnica) AS tecnica, AVG(ms.rendimiento_fisico) AS rendimiento " +
        "FROM metricas_sesion ms " +
        "JOIN sesion_entrenamiento s ON s.id = ms.sesion_id " +
        "JOIN sesion_actividad sa ON sa.id = ms.sesion_actividad_id " +
        "JOIN actividad a ON a.id = sa.actividad_id " +
        "WHERE ms.deportista_id = ? AND ms.deleted_at IS NULL " +
        (actividadId != null ? "AND a.id = ? " : "") +
        "GROUP BY s.fecha, a.nombre ORDER BY s.fecha ASC";

    var q = entityManager.createNativeQuery(sql).setParameter(1, deportistaId);
    if (actividadId != null) q.setParameter(2, actividadId);

    List<Object[]> rows = q.getResultList();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object[] row : rows) {
        Map<String, Object> m = new HashMap<>();
        m.put("fecha",       row[0] != null ? row[0].toString() : null);
        m.put("actividad",   row[1]);
        m.put("tecnica",     row[2] != null ? ((Number) row[2]).doubleValue() : null);
        m.put("rendimiento", row[3] != null ? ((Number) row[3]).doubleValue() : null);
        result.add(m);
    }
    return result;
}

/** Gráfica 2: Comparación de deportistas en una sesión (barras) */
public List<Map<String, Object>> getComparacionSesion(Long sesionId, Long actividadId) {
    String sql =
        "SELECT u.nombre, u.apellido, a.nombre AS actividad, " +
        "AVG(ms.tecnica) AS tecnica, AVG(ms.rendimiento_fisico) AS rendimiento, " +
        "AVG(ms.tiempo) AS tiempo, AVG(ms.distancia) AS distancia, AVG(ms.velocidad) AS velocidad " +
        "FROM metricas_sesion ms " +
        "JOIN deportista d ON d.id = ms.deportista_id " +
        "JOIN usuario_club uc ON uc.id = d.usuario_club_id " +
        "JOIN usuario u ON u.id = uc.usuario_id " +
        "JOIN sesion_actividad sa ON sa.id = ms.sesion_actividad_id " +
        "JOIN actividad a ON a.id = sa.actividad_id " +
        "WHERE ms.sesion_id = ? AND ms.deleted_at IS NULL " +
        (actividadId != null ? "AND a.id = ? " : "") +
        "GROUP BY u.nombre, u.apellido, a.nombre " +
        "ORDER BY u.apellido, u.nombre";

    var q = entityManager.createNativeQuery(sql).setParameter(1, sesionId);
    if (actividadId != null) q.setParameter(2, actividadId);

    List<Object[]> rows = q.getResultList();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object[] row : rows) {
        Map<String, Object> m = new HashMap<>();
        m.put("deportista",  row[0] + " " + row[1]);
        m.put("actividad",   row[2]);
        m.put("tecnica",     row[3] != null ? ((Number) row[3]).doubleValue() : null);
        m.put("rendimiento", row[4] != null ? ((Number) row[4]).doubleValue() : null);
        m.put("tiempo",      row[5] != null ? ((Number) row[5]).doubleValue() : null);
        m.put("distancia",   row[6] != null ? ((Number) row[6]).doubleValue() : null);
        m.put("velocidad",   row[7] != null ? ((Number) row[7]).doubleValue() : null);
        result.add(m);
    }
    return result;
}

/** Gráfica 3: Asistencia por deportista en el tiempo (barras) */
public List<Map<String, Object>> getAsistenciaDeportistas(Long grupoId) {
    String sql =
        "SELECT u.nombre, u.apellido, " +
        "SUM(CASE WHEN a.estado = 'presente' THEN 1 ELSE 0 END) AS presentes, " +
        "SUM(CASE WHEN a.estado = 'ausente'  THEN 1 ELSE 0 END) AS ausentes, " +
        "COUNT(a.id) AS total " +
        "FROM deportista d " +
        "JOIN usuario_club uc ON uc.id = d.usuario_club_id " +
        "JOIN usuario u ON u.id = uc.usuario_id " +
        "LEFT JOIN asistencia a ON a.deportista_id = d.id " +
        "WHERE d.grupo_id = ? AND d.deleted_at IS NULL AND uc.estado = 'activo' " +
        "GROUP BY u.nombre, u.apellido ORDER BY u.apellido, u.nombre";

    List<Object[]> rows = entityManager.createNativeQuery(sql)
            .setParameter(1, grupoId).getResultList();

    List<Map<String, Object>> result = new ArrayList<>();
    for (Object[] row : rows) {
        Map<String, Object> m = new HashMap<>();
        m.put("deportista", row[0] + " " + row[1]);
        m.put("presentes",  row[2] != null ? ((Number) row[2]).intValue() : 0);
        m.put("ausentes",   row[3] != null ? ((Number) row[3]).intValue() : 0);
        m.put("total",      row[4] != null ? ((Number) row[4]).intValue() : 0);
        result.add(m);
    }
    return result;
}

/** Gráfica 4: Promedio de distancia / velocidad / tiempo por sesión de un grupo */
public List<Map<String, Object>> getPromediosSesiones(Long grupoId) {
    String sql =
        "SELECT s.fecha, s.id AS sesion_id, " +
        "AVG(ms.tiempo) AS tiempo, AVG(ms.distancia) AS distancia, AVG(ms.velocidad) AS velocidad " +
        "FROM sesion_entrenamiento s " +
        "LEFT JOIN metricas_sesion ms ON ms.sesion_id = s.id AND ms.deleted_at IS NULL " +
        "WHERE s.grupo_id = ? AND s.deleted_at IS NULL " +
        "GROUP BY s.id, s.fecha ORDER BY s.fecha ASC";

    List<Object[]> rows = entityManager.createNativeQuery(sql)
            .setParameter(1, grupoId).getResultList();

    List<Map<String, Object>> result = new ArrayList<>();
    for (Object[] row : rows) {
        Map<String, Object> m = new HashMap<>();
        m.put("fecha",     row[0] != null ? row[0].toString() : null);
        m.put("sesionId",  ((Number) row[1]).longValue());
        m.put("tiempo",    row[2] != null ? ((Number) row[2]).doubleValue() : null);
        m.put("distancia", row[3] != null ? ((Number) row[3]).doubleValue() : null);
        m.put("velocidad", row[4] != null ? ((Number) row[4]).doubleValue() : null);
        result.add(m);
    }
    return result;
}

}