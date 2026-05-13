package backend.controller;

import backend.entity.Usuario;
import backend.service.UsuarioService;
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

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Login exitoso");
        response.put("usuario", usuarioLogueado);

        return ResponseEntity.ok(response);

    } catch (Exception e) {

        Map<String, String> error = new HashMap<>();

        error.put("message", e.getMessage());

        return ResponseEntity.badRequest().body(error);
    }
}
}