package backend.service;

import backend.entity.Usuario;
import backend.entity.UsuarioClub;
import backend.repository.UsuarioRepository;
import backend.repository.UsuarioClubRepository;
import backend.dto.RolValidacionDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public RolValidacionDTO validarRol(String email) {
        Optional<UsuarioClub> usuarioClub = usuarioClubRepository.findFirstByUsuarioEmail(email);

        if (usuarioClub.isPresent()) {
            UsuarioClub uc = usuarioClub.get();
            return new RolValidacionDTO(
                    uc.getRol(), // rol: "entrenador", "deportista", "admin"
                    true, // tiene_panel
                    uc.getClubId());
        }

        return new RolValidacionDTO(null, false, null);
    }
}
