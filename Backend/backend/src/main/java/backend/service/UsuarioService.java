package backend.service;

import backend.entity.Usuario;
import backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
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
                passwordEncriptada
        );
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


}