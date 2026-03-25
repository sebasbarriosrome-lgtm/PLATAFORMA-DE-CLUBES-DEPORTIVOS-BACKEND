package backend.service;

import backend.dto.LoginRequestDto;
import backend.dto.RegisterRequestDto;
import backend.model.Usuario;
import backend.model.Perfil;
import backend.repository.UsuarioRepository;
import backend.security.JwtUtil;
import backend.repository.PerfilRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public Usuario registerUser(RegisterRequestDto dto) {

        if (usuarioRepository.findByCorreo(dto.getCorreo()).isPresent()) {
            throw new RuntimeException("El correo ya está registrado");
        }

        // 👤 1. Crear usuario (SOLO login)
        Usuario usuario = new Usuario();
        usuario.setCorreo(dto.getCorreo());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));

        usuario = usuarioRepository.save(usuario);

        // 🧍 2. Crear perfil (datos personales)
        Perfil perfil = new Perfil();
        perfil.setTipo(dto.getTipo());
        perfil.setPrimerNombre(dto.getPrimerNombre());
        perfil.setSegundoNombre(dto.getSegundoNombre());
        perfil.setPrimerApellido(dto.getPrimerApellido());
        perfil.setSegundoApellido(dto.getSegundoApellido());
        perfil.setTelefono(dto.getTelefono());
        perfil.setDeporte(dto.getDeporte());
        perfil.setFechaNacimiento(dto.getFechaNacimiento());

        perfil.setUsuario(usuario);

        perfilRepository.save(perfil);

        return usuario;
    }

    public String login(LoginRequestDto dto) {

        Usuario usuario = usuarioRepository.findByCorreo(dto.getCorreo())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        // 🔐 generar token
        return jwtUtil.generateToken(usuario.getCorreo());
    }
}