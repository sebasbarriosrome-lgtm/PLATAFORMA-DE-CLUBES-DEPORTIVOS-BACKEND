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

        // Primero eliminar asociaciones en tablas intermedias para evitar FK violations
        entityManager.createNativeQuery("DELETE FROM entrenador_categoria WHERE entrenador_id = ?")
                .setParameter(1, entrenadorId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM grupo_entrenador WHERE entrenador_id = ?")
                .setParameter(1, entrenadorId)
                .executeUpdate();

        // Luego eliminar el entrenador y el usuario_club
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
        System.out.println("🔍 DEBUG getHorariosClub - Email: " + email + ", ClubId: " + clubId);

        List<Object[]> results = clubRepository.getHorariosByClubId(clubId);
        System.out.println("🔍 DEBUG getHorariosClub - Resultados: " + results.size() + " horarios encontrados");

        if (!results.isEmpty()) {
            Object[] firstRow = results.get(0);
            System.out.println("🔍 DEBUG getHorariosClub - Primer horario: id=" + firstRow[0] + ", dia=" + firstRow[1]);
        }

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
            String ubicacion,
            Long grupoId,
            String categoria) {

        Long clubId = obtenerClubIdDelAdmin(email);

        return clubRepository.crearHorarioEntrenamiento(
                clubId,
                grupoId,
                dia,
                horaInicio,
                horaFin,
                descripcion,
                ubicacion,
                categoria);
    }

    @Transactional
    public void actualizarHorario(
            Long horarioId,
            String email,
            String dia,
            String horaInicio,
            String horaFin,
            String descripcion,
            String ubicacion,
            Long grupoId,
            String categoria) {

        Long clubId = obtenerClubIdDelAdmin(email);

        Long count = ((Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM horario_entrenamiento WHERE id = ? AND grupo_id IN (SELECT id FROM grupo_deportivo WHERE club_id = ?) AND deleted_at IS NULL")
                .setParameter(1, horarioId)
                .setParameter(2, clubId)
                .getSingleResult()).longValue();

        if (count == 0) {
            throw new RuntimeException("Horario no encontrado o no pertenece al club");
        }

        clubRepository.actualizarHorarioEntrenamiento(
                horarioId,
                grupoId,
                dia,
                horaInicio,
                horaFin,
                descripcion,
                ubicacion,
                categoria);
    }

    @Transactional
    public void eliminarHorario(Long horarioId, String email) {

        Long clubId = obtenerClubIdDelAdmin(email);

        Long count = ((Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM horario_entrenamiento WHERE id = ? AND grupo_id IN (SELECT id FROM grupo_deportivo WHERE club_id = ?) AND deleted_at IS NULL")
                .setParameter(1, horarioId)
                .setParameter(2, clubId)
                .getSingleResult()).longValue();

        if (count == 0) {
            throw new RuntimeException("Horario no encontrado o no pertenece al club");
        }

        clubRepository.eliminarHorarioEntrenamiento(horarioId);
    }

    public List<Map<String, Object>> getCategories(String email, String search) {
        Long clubId = obtenerClubIdDelAdmin(email);
        List<Object[]> rows = clubRepository.getCategoriasByClub(clubId, search == null ? "" : search);

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Object[] row : rows) {
            Long categoriaId = ((Number) row[0]).longValue();
            Map<String, Object> category = new HashMap<>();
            category.put("id", categoriaId);
            category.put("clubId", ((Number) row[1]).longValue());
            category.put("nombre", row[2]);
            category.put("descripcion", row[3]);
            category.put("createdAt", row[4]);
            category.put("entrenadores", obtenerEntrenadoresPorCategoria(categoriaId));
            category.put("deportistas", obtenerDeportistasPorCategoria(categoriaId));
            categories.add(category);
        }

        return categories;
    }

    public Map<String, Object> getCategoryById(Long categoriaId) {
        Object[] row = clubRepository.getCategoriaById(categoriaId);
        if (row == null) {
            return null;
        }

        Long categoryClubId = ((Number) row[1]).longValue();
        Map<String, Object> category = new HashMap<>();
        category.put("id", ((Number) row[0]).longValue());
        category.put("clubId", categoryClubId);
        category.put("nombre", row[2]);
        category.put("descripcion", row[3]);
        category.put("createdAt", row[4]);
        category.put("entrenadores", obtenerEntrenadoresPorCategoria(((Number) row[0]).longValue()));
        category.put("deportistas", obtenerDeportistasPorCategoria(((Number) row[0]).longValue()));
        return category;
    }

    @Transactional
    public Long createCategory(String email, String nombre, String descripcion, List<Long> entrenadorIds) {
        Long clubId = obtenerClubIdDelAdmin(email);
        Long categoriaId = clubRepository.crearCategoria(clubId, nombre, descripcion);
        asignarEntrenadoresCategoria(categoriaId, entrenadorIds);
        return categoriaId;
    }

    @Transactional
    public void updateCategory(Long categoriaId, String email, String nombre, String descripcion,
            List<Long> entrenadorIds) {
        Long clubId = obtenerClubIdDelAdmin(email);
        Object[] row = clubRepository.getCategoriaById(categoriaId);
        if (row == null || ((Number) row[1]).longValue() != clubId) {
            throw new RuntimeException("Categoría no encontrada o no pertenece al club");
        }

        clubRepository.actualizarCategoria(categoriaId, nombre, descripcion);
        asignarEntrenadoresCategoria(categoriaId, entrenadorIds);
    }

    @Transactional
    public void assignEntrenadoresToCategory(Long categoriaId, String email, List<Long> entrenadorIds) {
        Long clubId = obtenerClubIdDelAdmin(email);
        Object[] row = clubRepository.getCategoriaById(categoriaId);
        if (row == null || ((Number) row[1]).longValue() != clubId) {
            throw new RuntimeException("Categoría no encontrada o no pertenece al club");
        }
        asignarEntrenadoresCategoria(categoriaId, entrenadorIds);
    }

    @Transactional
    public void deleteCategory(Long categoriaId, String email) {
        Long clubId = obtenerClubIdDelAdmin(email);
        Object[] row = clubRepository.getCategoriaById(categoriaId);
        if (row == null || ((Number) row[1]).longValue() != clubId) {
            throw new RuntimeException("Categoría no encontrada o no pertenece al club");
        }

        // Limpiar asociaciones de entrenadores en la categoría antes de eliminar
        clubRepository.clearEntrenadoresCategoria(categoriaId);

        // Eliminar la categoría
        clubRepository.eliminarCategoria(categoriaId);
    }

    private void asignarEntrenadoresCategoria(Long categoriaId, List<Long> entrenadorIds) {
        clubRepository.clearEntrenadoresCategoria(categoriaId);
        if (entrenadorIds == null) {
            return;
        }
        for (Long entrenadorId : entrenadorIds) {
            clubRepository.insertEntrenadorCategoria(categoriaId, entrenadorId);
        }
    }

    public List<Map<String, Object>> getGroups(String email, String search) {
        Long clubId = obtenerClubIdDelAdmin(email);
        List<Object[]> rows = clubRepository.getGruposByClub(clubId, search == null ? "" : search);

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Object[] row : rows) {
            Long grupoId = ((Number) row[0]).longValue();
            Map<String, Object> group = new HashMap<>();
            group.put("id", grupoId);
            group.put("clubId", ((Number) row[1]).longValue());
            group.put("nombre", row[2]);
            group.put("descripcion", row[3]);
            group.put("createdAt", row[4]);
            group.put("entrenadores", obtenerEntrenadoresPorGrupo(grupoId));
            group.put("deportistas", obtenerDeportistasPorGrupo(grupoId));
            groups.add(group);
        }

        return groups;
    }

    public Map<String, Object> getGroupById(Long grupoId) {
        Object[] row = clubRepository.getGrupoById(grupoId);
        if (row == null) {
            return null;
        }

        Map<String, Object> group = new HashMap<>();
        group.put("id", ((Number) row[0]).longValue());
        group.put("clubId", ((Number) row[1]).longValue());
        group.put("nombre", row[2]);
        group.put("descripcion", row[3]);
        group.put("createdAt", row[4]);
        group.put("entrenadores", obtenerEntrenadoresPorGrupo(((Number) row[0]).longValue()));
        group.put("deportistas", obtenerDeportistasPorGrupo(((Number) row[0]).longValue()));
        return group;
    }

    @Transactional
    public Long createGroup(String email, String nombre, String descripcion, List<Long> entrenadorIds) {
        Long clubId = obtenerClubIdDelAdmin(email);
        Long grupoId = clubRepository.crearGrupoDeportivo(clubId, nombre, descripcion);
        asignarEntrenadoresGrupo(grupoId, entrenadorIds);
        return grupoId;
    }

    @Transactional
    public void updateGroup(Long grupoId, String email, String nombre, String descripcion, List<Long> entrenadorIds) {
        Long clubId = obtenerClubIdDelAdmin(email);
        Object[] row = clubRepository.getGrupoById(grupoId);
        if (row == null || ((Number) row[1]).longValue() != clubId) {
            throw new RuntimeException("Grupo no encontrado o no pertenece al club");
        }

        clubRepository.actualizarGrupoDeportivo(grupoId, nombre, descripcion);
        asignarEntrenadoresGrupo(grupoId, entrenadorIds);
    }

    @Transactional
    public void assignEntrenadoresToGroup(Long grupoId, String email, List<Long> entrenadorIds) {
        Long clubId = obtenerClubIdDelAdmin(email);
        Object[] row = clubRepository.getGrupoById(grupoId);
        if (row == null || ((Number) row[1]).longValue() != clubId) {
            throw new RuntimeException("Grupo no encontrado o no pertenece al club");
        }
        asignarEntrenadoresGrupo(grupoId, entrenadorIds);
    }

    @Transactional
    public void deleteGroup(Long grupoId, String email) {
        Long clubId = obtenerClubIdDelAdmin(email);
        Object[] row = clubRepository.getGrupoById(grupoId);
        if (row == null || ((Number) row[1]).longValue() != clubId) {
            throw new RuntimeException("Grupo no encontrado o no pertenece al club");
        }

        // Limpiar asociaciones de entrenadores en el grupo antes de eliminar
        clubRepository.clearEntrenadoresGrupo(grupoId);

        // Eliminar el grupo
        clubRepository.eliminarGrupoDeportivo(grupoId);
    }

    private void asignarEntrenadoresGrupo(Long grupoId, List<Long> entrenadorIds) {
        clubRepository.clearEntrenadoresGrupo(grupoId);
        if (entrenadorIds == null) {
            return;
        }
        for (Long entrenadorId : entrenadorIds) {
            clubRepository.insertGrupoEntrenador(grupoId, entrenadorId);
        }
    }

    private List<Map<String, Object>> obtenerEntrenadoresPorCategoria(Long categoriaId) {
        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(
                "SELECT e.id, u.id, u.nombre, u.apellido, u.email, e.experiencia, e.especialidad " +
                        "FROM entrenador_categoria ec " +
                        "JOIN entrenador e ON ec.entrenador_id = e.id " +
                        "JOIN usuario_club uc ON uc.id = e.usuario_club_id " +
                        "JOIN usuario u ON u.id = uc.usuario_id " +
                        "WHERE ec.categoria_id = ? " +
                        "AND e.deleted_at IS NULL " +
                        "AND uc.estado = 'activo'")
                .setParameter(1, categoriaId)
                .getResultList();

        List<Map<String, Object>> trainers = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("entrenadorId", ((Number) row[0]).longValue());
            item.put("usuarioId", ((Number) row[1]).longValue());
            item.put("nombre", row[2]);
            item.put("apellido", row[3]);
            item.put("email", row[4]);
            item.put("experiencia", row[5]);
            item.put("especialidad", row[6]);
            trainers.add(item);
        }
        return trainers;
    }

    private List<Map<String, Object>> obtenerDeportistasPorCategoria(Long categoriaId) {
        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(
                "SELECT d.id, u.id, u.nombre, u.apellido, u.email, d.peso, d.estatura " +
                        "FROM deportista d " +
                        "JOIN usuario_club uc ON uc.id = d.usuario_club_id " +
                        "JOIN usuario u ON u.id = uc.usuario_id " +
                        "WHERE d.categoria_id = ? " +
                        "AND d.deleted_at IS NULL " +
                        "AND uc.estado = 'activo'")
                .setParameter(1, categoriaId)
                .getResultList();

        List<Map<String, Object>> athletes = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("deportistaId", ((Number) row[0]).longValue());
            item.put("usuarioId", ((Number) row[1]).longValue());
            item.put("nombre", row[2]);
            item.put("apellido", row[3]);
            item.put("email", row[4]);
            item.put("peso", row[5]);
            item.put("estatura", row[6]);
            athletes.add(item);
        }
        return athletes;
    }

    private List<Map<String, Object>> obtenerEntrenadoresPorGrupo(Long grupoId) {
        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(
                "SELECT e.id, u.id, u.nombre, u.apellido, u.email, e.experiencia, e.especialidad " +
                        "FROM grupo_entrenador ge " +
                        "JOIN entrenador e ON ge.entrenador_id = e.id " +
                        "JOIN usuario_club uc ON uc.id = e.usuario_club_id " +
                        "JOIN usuario u ON u.id = uc.usuario_id " +
                        "WHERE ge.grupo_id = ? " +
                        "AND e.deleted_at IS NULL " +
                        "AND uc.estado = 'activo'")
                .setParameter(1, grupoId)
                .getResultList();

        List<Map<String, Object>> trainers = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("entrenadorId", ((Number) row[0]).longValue());
            item.put("usuarioId", ((Number) row[1]).longValue());
            item.put("nombre", row[2]);
            item.put("apellido", row[3]);
            item.put("email", row[4]);
            item.put("experiencia", row[5]);
            item.put("especialidad", row[6]);
            trainers.add(item);
        }
        return trainers;
    }

    private List<Map<String, Object>> obtenerDeportistasPorGrupo(Long grupoId) {
        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(
                "SELECT d.id, u.id, u.nombre, u.apellido, u.email, d.peso, d.estatura " +
                        "FROM deportista d " +
                        "JOIN usuario_club uc ON uc.id = d.usuario_club_id " +
                        "JOIN usuario u ON u.id = uc.usuario_id " +
                        "WHERE d.grupo_id = ? " +
                        "AND d.deleted_at IS NULL " +
                        "AND uc.estado = 'activo'")
                .setParameter(1, grupoId)
                .getResultList();

        List<Map<String, Object>> athletes = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("deportistaId", ((Number) row[0]).longValue());
            item.put("usuarioId", ((Number) row[1]).longValue());
            item.put("nombre", row[2]);
            item.put("apellido", row[3]);
            item.put("email", row[4]);
            item.put("peso", row[5]);
            item.put("estatura", row[6]);
            athletes.add(item);
        }
        return athletes;
    }

    private Long obtenerClubIdDelAdmin(String email) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        System.out.println("🔍 UsuarioId: " + usuario.getId());

        // ── DEBUG: ver todos los registros de usuario_club para este usuario ──
        @SuppressWarnings("unchecked")
        List<Object[]> raw = entityManager.createNativeQuery(
                "SELECT uc.id, uc.club_id, uc.rol, uc.estado, e.id as entrenador_id " +
                "FROM usuario_club uc " +
                "LEFT JOIN entrenador e ON e.usuario_club_id = uc.id " +
                "WHERE uc.usuario_id = ?")
                .setParameter(1, usuario.getId())
                .getResultList();

        if (raw.isEmpty()) {
            System.out.println("🔍 DEBUG - usuario_club VACÍO para userId=" + usuario.getId());
        } else {
            raw.forEach(r -> System.out.println(
                "🔍 DEBUG - uc.id=" + r[0] +
                " club_id=" + r[1] +
                " rol=" + r[2] +
                " estado=" + r[3] +
                " entrenador_id=" + r[4]
            ));
        }
        // ── FIN DEBUG ──

        Map<String, Object> panel = (Map<String, Object>) clubRepository.getPanelClubData(usuario.getId());

        if (panel != null && panel.get("id") != null) {
            Long clubId = ((Number) panel.get("id")).longValue();
            System.out.println("🔍 Es admin, ClubId: " + clubId);
            return clubId;
        }

        System.out.println("🔍 No es admin, buscando como entrenador...");

        @SuppressWarnings("unchecked")
        List<Object> trainerClubResults = entityManager.createNativeQuery(
                "SELECT uc.club_id " +
                "FROM usuario_club uc " +
                "JOIN entrenador e ON e.usuario_club_id = uc.id " +
                "WHERE uc.usuario_id = ? " +
                "AND uc.rol = 'entrenador' " +
                "LIMIT 1")
                .setParameter(1, usuario.getId())
                .getResultList();

        if (trainerClubResults.isEmpty()) {
            System.out.println("🔍 ERROR: No es admin y no es entrenador de ningún club");
            throw new RuntimeException("No tiene club asociado");
        }

        Long clubId = ((Number) trainerClubResults.get(0)).longValue();
        System.out.println("🔍 Es entrenador, ClubId: " + clubId);
        return clubId;
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

            // ✅ grupoId
            item.put("grupoId", row[7] == null ? null : ((Number) row[7]).longValue());

            // ✅ categoria
            item.put("categoria", row[8]);

            // ✅ NUEVO → nombre del grupo
            item.put("grupoNombre", row[9]);

            response.add(item);
        }

        return response;
    }


    public Club getClubById(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Club no encontrado"));
    }
}
