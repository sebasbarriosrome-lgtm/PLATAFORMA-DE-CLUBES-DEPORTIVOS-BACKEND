package backend.service;

import backend.entity.Invitacion;
import backend.entity.Usuario;
import backend.entity.UsuarioClub;
import backend.repository.ClubRepository;
import backend.repository.InvitacionRepository;
import backend.repository.UsuarioRepository;
import backend.repository.UsuarioClubRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UsuarioService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioClubRepository usuarioClubRepository;
    private final InvitacionRepository invitacionRepository;
    private final ClubRepository clubRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
            UsuarioClubRepository usuarioClubRepository,
            InvitacionRepository invitacionRepository,
            ClubRepository clubRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioClubRepository = usuarioClubRepository;
        this.invitacionRepository = invitacionRepository;
        this.clubRepository = clubRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // REGISTER
    public void registrarUsuario(String nombre,
            String apellido,
            String email,
            String password) {

        String passwordEncriptada = passwordEncoder.encode(password);

        usuarioRepository.registrarUsuario(
                nombre,
                apellido,
                email,
                passwordEncriptada);
    }

    // LOGIN
    public Usuario login(String email, String password) {

        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        return usuario;
    }

    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // EDITAR PERFIL

    @Transactional
    public Usuario actualizarPerfil(
            String emailActual,
            String nombre,
            String apellido,
            String email,
            String telefono,
            String birthDate,
            String photoUrl) {

        return usuarioRepository.actualizarPerfil(
                emailActual,
                nombre,
                apellido,
                email,
                telefono,
                birthDate,
                photoUrl);
    }

    // VALIDAR ROL DESDE USUARIO_CLUB
    public java.util.Map<String, Object> validarRol(String email) {
        Optional<UsuarioClub> usuarioClub = usuarioClubRepository.findFirstByUsuarioEmail(email);

        java.util.Map<String, Object> resp = new java.util.HashMap<>();

        if (usuarioClub.isPresent()) {
            UsuarioClub uc = usuarioClub.get();
            resp.put("rol", uc.getRol()); // rol: "entrenador", "deportista", "admin"
            resp.put("tienePanel", true);
            resp.put("clubId", uc.getClubId());
            return resp;
        }

        resp.put("rol", null);
        resp.put("tienePanel", false);
        resp.put("clubId", null);
        return resp;
    }

    public List<Map<String, Object>> obtenerInvitaciones(String email) {
        List<Object[]> rows = invitacionRepository.findInvitacionesByUsuarioEmail(email);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0] == null ? null : ((Number) row[0]).longValue());
            item.put("clubName", row[1]);
            item.put("rol", row[2]);
            item.put("estado", row[3]);
            item.put("createdAt", row[4]);
            item.put("mensaje", "Has sido invitado por el club " + row[1]);
            response.add(item);
        }

        return response;
    }

    @Transactional
    public void resolverInvitacion(
            String email,
            Long invitacionId,
            String accion,
            Double peso,
            Long estatura,
            String experiencia,
            String especialidad) {

        if (invitacionId == null) {
            throw new IllegalArgumentException("ID de invitación inválido");
        }

        if (!"aceptado".equals(accion) && !"rechazado".equals(accion)) {
            throw new IllegalArgumentException("Acción inválida");
        }

        Invitacion invitacion = invitacionRepository
                .findByIdAndUsuarioEmail(invitacionId, email)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró la invitación o no tiene permisos para modificarla"));

        if ("rechazado".equals(accion)) {
            int deleted = invitacionRepository.deleteByIdAndUsuarioEmail(invitacionId, email);
            if (deleted == 0) {
                throw new RuntimeException("No se encontró la invitación o no tiene permisos para modificarla");
            }
            return;
        }

        String rol = invitacion.getRol();
        Long usuarioId = invitacion.getUsuarioId();
        Long clubId = invitacion.getClubId();

        // Validar datos obligatorios según rol
        if ("deportista".equals(rol)) {
            if (peso == null || estatura == null) {
                throw new IllegalArgumentException("Peso y estatura son requeridos para aceptar como deportista");
            }
        } else if ("entrenador".equals(rol)) {
            if (experiencia == null || experiencia.trim().isEmpty() || especialidad == null
                    || especialidad.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Experiencia y especialidad son requeridos para aceptar como entrenador");
            }
        }

        // Reutilizar la creación de usuario_club existente
        clubRepository.crearUsuarioClub(usuarioId, clubId, rol);

        if ("deportista".equals(rol)) {
            entityManager.createNativeQuery(""
                    + "INSERT INTO deportista (usuario_club_id, peso, estatura, created_at) "
                    + "VALUES ("
                    + "(SELECT id FROM usuario_club WHERE usuario_id = ? AND club_id = ?), "
                    + "?, ?, CURRENT_TIMESTAMP)"

            )
                    .setParameter(1, usuarioId)
                    .setParameter(2, clubId)
                    .setParameter(3, peso)
                    .setParameter(4, estatura)
                    .executeUpdate();
        } else if ("entrenador".equals(rol)) {
            entityManager.createNativeQuery(""
                    + "INSERT INTO entrenador (usuario_club_id, experiencia, especialidad, created_at) "
                    + "VALUES ("
                    + "(SELECT id FROM usuario_club WHERE usuario_id = ? AND club_id = ?), "
                    + "?, ?, CURRENT_TIMESTAMP)"

            )
                    .setParameter(1, usuarioId)
                    .setParameter(2, clubId)
                    .setParameter(3, experiencia)
                    .setParameter(4, especialidad)
                    .executeUpdate();
        }

        int deleted = invitacionRepository.deleteByIdAndUsuarioEmail(invitacionId, email);
        if (deleted == 0) {
            throw new RuntimeException("No se encontró la invitación o no tiene permisos para modificarla");
        }
    }
}
