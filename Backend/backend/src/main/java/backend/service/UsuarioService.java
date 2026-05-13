package backend.service;

import backend.entity.Usuario;
import backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrarUsuario(
            String nombre,
            String apellido,
            String email,
            String password
    ) {

        usuarioRepository.registrarUsuario(
                nombre,
                apellido,
                email,
                password
        );
    }

    public Usuario login(String email, String password) {

    Usuario usuario = usuarioRepository.login(email, password);

    if (usuario == null) {
        throw new RuntimeException("Correo o contraseña incorrectos");
    }

    return usuario;
}
}