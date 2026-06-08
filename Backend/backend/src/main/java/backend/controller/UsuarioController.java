package backend.controller;

import backend.entity.Usuario;
import backend.repository.PerfilUsuarioRepository;
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

    // Servicio
    private final UsuarioService usuarioService;
    private final PerfilUsuarioRepository perfilUsuarioRepository;

    public UsuarioController(UsuarioService usuarioService, PerfilUsuarioRepository perfilUsuarioRepository) {
        this.usuarioService = usuarioService;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {

        try {

            usuarioService.registrarUsuario(
                    usuario.getNombre(),
                    usuario.getApellido(),
                    usuario.getEmail(),
                    usuario.getPassword());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Usuario registrado correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace(); // AQUÍ VA

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
                    usuario.getPassword());
            // GENERAR TOKEN
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

    @GetMapping("/validar-rol")
    public ResponseEntity<?> validarRol(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            java.util.Map<String, Object> validacion = usuarioService.validarRol(email);
            return ResponseEntity.ok(validacion);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Error validando rol");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerPerfil(HttpServletRequest request) {

        try {

            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body(Map.of("message", "No autorizado"));
            }

            Usuario usuario = usuarioService.obtenerPorEmail(email);

            Map<String, Object> response = new HashMap<>();

            response.put(
                    "name",
                    usuario.getNombre() + " " + usuario.getApellido());

            response.put("email", usuario.getEmail());
            response.put("rol", usuario.getRolGlobal());

            // DATOS PERFIL
            if (usuario.getPerfilUsuario() != null) {

                response.put(
                        "telefono",
                        usuario.getPerfilUsuario().getTelefono());

                response.put(
                        "birthDate",
                        usuario.getPerfilUsuario().getFechaNacimiento());

                response.put(
                        "photoUrl",
                        usuario.getPerfilUsuario().getFotoUrl());

            } else {

                response.put("telefono", null);
                response.put("birthDate", null);
                response.put("photoUrl", null);
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Usuario no encontrado")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Usuario no encontrado");
                return ResponseEntity.status(401).body(error);
            }
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error al obtener perfil"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error al obtener perfil"));
        }
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        try {
            String emailActual = (String) request.getAttribute("email");

            if (emailActual == null) {
                return ResponseEntity.status(401).body(Map.of("message", "No autorizado"));
            }

            String nombre = body.getOrDefault("name", body.get("nombre"));
            String apellido = body.getOrDefault("apellido", body.get("apellidos"));
            String email = body.get("email");
            String telefono = body.get("telefono");
            String birthDate = body.get("birthDate");
            String photoUrl = body.get("photoUrl");

            // Normalizar campos vacíos a null
            nombre = (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : null;
            apellido = (apellido != null && !apellido.trim().isEmpty()) ? apellido.trim() : null;
            email = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
            telefono = (telefono != null && !telefono.trim().isEmpty()) ? telefono.trim() : null;
            birthDate = (birthDate != null && !birthDate.trim().isEmpty()) ? birthDate.trim() : null;
            photoUrl = (photoUrl != null && !photoUrl.trim().isEmpty()) ? photoUrl.trim() : null;

            // Si no se proporciona email nuevo, usar el actual
            if (email == null) {
                email = emailActual;
            }

            // Validar que nombre y apellido no sean null al mismo tiempo
            if (nombre == null && apellido == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "El nombre es requerido"));
            }

            if ((apellido == null || apellido.isBlank()) && nombre != null && nombre.contains(" ")) {
                int spaceIndex = nombre.indexOf(' ');
                apellido = nombre.substring(spaceIndex + 1).trim();
                nombre = nombre.substring(0, spaceIndex).trim();
            } else if (nombre != null && nombre.contains(" ") && apellido != null && !apellido.isBlank()) {
                nombre = nombre.split(" ")[0];
            }

            Usuario usuarioActualizado = usuarioService.actualizarPerfil(
                    emailActual,
                    nombre,
                    apellido,
                    email,
                    telefono,
                    birthDate,
                    photoUrl);

            String nuevoToken = JwtUtil.generarToken(usuarioActualizado);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Perfil actualizado");
            response.put("token", nuevoToken);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            String message = e.getMessage() != null ? e.getMessage() : "Datos inválidos";
            error.put("message", message);
            return ResponseEntity.badRequest().body(error);

        } catch (RuntimeException e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            String message = e.getMessage() != null ? e.getMessage() : "Error al actualizar perfil";

            if (message.contains("correo") || message.contains("email")) {
                error.put("message", "El correo ya está registrado");
            } else if (message.contains("teléfono") || message.contains("telefono")) {
                error.put("message", "El teléfono ya está registrado");
            } else if (message.contains("fecha") || message.contains("date")) {
                error.put("message", "Fecha de nacimiento inválida");
            } else {
                error.put("message", message);
            }

            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Error inesperado: " + (e.getMessage() != null ? e.getMessage() : "Desconocido"));
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/invitaciones")
    public ResponseEntity<?> obtenerInvitaciones(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body(Map.of("message", "No autorizado"));
            }
            return ResponseEntity.ok(usuarioService.obtenerInvitaciones(email));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Error al obtener invitaciones"));
        }
    }

    @PutMapping("/invitaciones/{id}")
    public ResponseEntity<?> resolverInvitacion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body(Map.of("message", "No autorizado"));
            }

            String accion = body.get("accion") != null ? body.get("accion").toString() : null;
            if (accion == null || (!accion.equals("aceptado") && !accion.equals("rechazado"))) {
                return ResponseEntity.badRequest().body(Map.of("message", "Acción inválida"));
            }

            Double peso = body.get("peso") != null ? Double.valueOf(body.get("peso").toString()) : null;
            Long estatura = body.get("estatura") != null ? Long.valueOf(body.get("estatura").toString()) : null;
            String experiencia = body.get("experiencia") != null ? body.get("experiencia").toString() : null;
            String especialidad = body.get("especialidad") != null ? body.get("especialidad").toString() : null;

            usuarioService.resolverInvitacion(email, id, accion, peso, estatura, experiencia, especialidad);
            return ResponseEntity.ok(Map.of("message", "Invitación actualizada"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Error al actualizar invitación"));
        }
    }

}