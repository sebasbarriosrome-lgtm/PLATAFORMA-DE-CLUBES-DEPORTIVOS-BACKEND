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
            UsuarioRepository usuarioRepository
    ) {
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
            String email
    ) {

        // ✅ 1. CREAR CLUB (SP)
        clubRepository.crearClub(
                nombre,
                ciudad,
                descripcion,
                logoUrl,
                bannerUrl,
                colorPrimario,
                colorSecundario,
                contacto
        );

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
                "admin"
        );
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
        String colorSecundario
    ) {

        // ✅ obtener usuario
        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ obtener club del usuario
        Object[] result = (Object[]) clubRepository.getPanelClubData(usuario.getId());

        Long clubId = ((Number) result[0]).longValue();

        // ✅ actualizar
        clubRepository.actualizarClub(
                clubId,
                descripcion,
                logoUrl,
                bannerUrl,
                colorPrimario,
                colorSecundario
        );
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
        Integer edad,
        Double peso,
        Long estatura,
        String experiencia,
        String especialidad
    ) {

        var usuario = usuarioRepository.buscarPorEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ 1. crear solicitud
        Long solicitudId = clubRepository.crearSolicitud(
            usuario.getId(),
            clubId,
            rol,
            mensaje
        );

        // ✅ 2. si es deportista o entrenador (IMPORTANTE 🔥)
        if ("deportista".equals(rol) || "entrenador".equals(rol)) {

            clubRepository.crearSolicitudDeportiva(
                solicitudId,
                "deportista".equals(rol) ? edad : null,        // ✅ solo deportista
                "deportista".equals(rol) ? peso : null,
                "deportista".equals(rol) ? estatura : null,
                experiencia,                                  // ✅ ambos
                especialidad                                  // ✅ ambos
            );
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
        boolean esAdmin = entityManager.createNativeQuery("""
            SELECT COUNT(*) FROM usuario_club
            WHERE usuario_id = ? AND rol = 'admin'
        """)
        .setParameter(1, usuario.getId())
        .getSingleResult() != null;

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

        // ✅ si rechaza
        if ("rechazado".equals(accion)) {
            clubRepository.actualizarEstadoSolicitud(solicitudId, "rechazado");
            return;
        }

        // ✅ aceptar
        clubRepository.actualizarEstadoSolicitud(solicitudId, "aceptado");

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
    }

    public Club getClubById(Long id) {
        return clubRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Club no encontrado"));
    }


}