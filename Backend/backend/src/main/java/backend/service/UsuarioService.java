package backend.service;

import backend.entity.Usuario;
import backend.entity.UsuarioClub;
import backend.repository.UsuarioRepository;
import backend.repository.UsuarioClubRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioClubRepository usuarioClubRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
            UsuarioClubRepository usuarioClubRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioClubRepository = usuarioClubRepository;
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
}
