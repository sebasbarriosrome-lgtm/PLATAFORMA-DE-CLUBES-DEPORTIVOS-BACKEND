package backend.controller;

import backend.entity.Usuario;
import backend.security.JwtUtil;
import backend.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin("*")
public class UsuarioController {

    //Servicio
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {

        try {

            usuarioService.registrarUsuario(
                    usuario.getNombre(),
                    usuario.getApellido(),
                    usuario.getEmail(),
                    usuario.getPassword()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Usuario registrado correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace(); // 👈 AQUÍ VA

            Map<String, String> error = new HashMap<>();

            if (e.getMessage().contains("El correo ya está registrado")) {
                error.put("message", "El correo ya está registrado");
            } else {
                error.put("message", "Error en el registro");
            }

    return ResponseEntity.badRequest().body(error);
}
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario usuario) {


    try {

        Usuario usuarioLogueado = usuarioService.login(
                usuario.getEmail(),
                usuario.getPassword()
        );
            // 🔐 GENERAR TOKEN
            String token = JwtUtil.generarToken(usuarioLogueado);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("email", usuarioLogueado.getEmail());
            response.put("rol", usuarioLogueado.getRolGlobal());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }

    }

    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerPerfil(HttpServletRequest request) {

    try {
        // 👇 sacamos el email del token (lo puso el JwtFilter)
        String email = (String) request.getAttribute("email");

        if (email == null) {
            return ResponseEntity.status(401).body("No autorizado");
        }

        Usuario usuario = usuarioService.obtenerPorEmail(email);

        // 👇 nunca envíes password
        Map<String, Object> response = new HashMap<>();
        response.put("name", usuario.getNombre() + " " + usuario.getApellido());
        response.put("email", usuario.getEmail());
        response.put("rol", usuario.getRolGlobal());

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error al obtener perfil");
    }
}

}