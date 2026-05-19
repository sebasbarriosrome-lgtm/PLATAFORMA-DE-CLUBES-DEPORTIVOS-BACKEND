package backend.controller;

import backend.entity.Club;
import backend.service.ClubService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clubs")
@CrossOrigin("*")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    // CREAR CLUB
    @PostMapping
    public ResponseEntity<?> crearClub(
            @RequestBody Club club,
            HttpServletRequest request) {

        try {

            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            clubService.crearClub(
                    club.getNombre(),
                    club.getCiudad(),
                    club.getDescripcion(),
                    club.getLogoUrl(),
                    club.getBannerUrl(),
                    club.getColorPrimario(),
                    club.getColorSecundario(),
                    club.getContacto(),
                    email);

            return ResponseEntity.ok("Club creado correctamente");

        } catch (Exception e) {

            Map<String, String> error = new HashMap<>();

            error.put("message", "Error al crear club");

            return ResponseEntity.badRequest().body(error);
        }
    }

    // LISTAR CLUBES
    @GetMapping
    public ResponseEntity<?> obtenerClubs() {

        return ResponseEntity.ok(clubService.getAllClubs());
    }

    // PANEL CLUB
    @GetMapping("/panel-club")
    public ResponseEntity<?> getPanelClub(HttpServletRequest request) {

        try {

            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            var result = clubService.getPanelClub(email);

            if (result == null || result.isEmpty()) {
                return ResponseEntity.status(403)
                        .body("No tienes acceso al panel del club");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            Map<String, String> error = new HashMap<>();
            error.put("message", "Error cargando panel");

            return ResponseEntity.badRequest().body(error);
        }
    }

    // ACTUALIZAR PERSONALIZACIÓN
    @PutMapping("/personalizacion")
    public ResponseEntity<?> actualizarClub(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        try {

            String email = (String) request.getAttribute("email");

            clubService.actualizarClub(
                    email,
                    body.get("descripcion"),
                    body.get("logoUrl"),
                    body.get("bannerUrl"),
                    body.get("colorPrimario"),
                    body.get("colorSecundario"));

            return ResponseEntity.ok("Actualizado");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error");
        }
    }

    // OBTENER CLUB POR iD
    @GetMapping("/{id}")
    public ResponseEntity<?> getClubById(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getClubBySlug(
            @PathVariable String slug) {

        return ResponseEntity.ok(
                clubService.getClubBySlug(slug));
    }

    // 1. CREAR SOLICITUD
    // ✅ ✅ ✅ 🔥 CREAR SOLICITUD
    @PostMapping("/solicitud")
    public ResponseEntity<?> crearSolicitud(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        try {

            String email = (String) request.getAttribute("email");

            // ✅ Validación de autenticación
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            // ✅ Parseo seguro de datos (MUY IMPORTANTE 🔥)
            Integer edad = body.get("edad") != null
                    ? Integer.valueOf(body.get("edad").toString())
                    : null;

            Double peso = body.get("peso") != null
                    ? Double.valueOf(body.get("peso").toString())
                    : null;

            Long estatura = body.get("estatura") != null
                    ? Long.valueOf(body.get("estatura").toString())
                    : null;

            String experiencia = body.get("experiencia") != null
                    ? body.get("experiencia").toString()
                    : null;

            String especialidad = body.get("especialidad") != null
                    ? body.get("especialidad").toString()
                    : null;

            // ✅ Crear solicitud
            clubService.crearSolicitud(
                    email,
                    Long.valueOf(body.get("clubId").toString()),
                    body.get("rol").toString(),
                    body.get("mensaje").toString(),
                    edad,
                    peso,
                    estatura,
                    experiencia,
                    especialidad);

            return ResponseEntity.ok("Solicitud enviada correctamente");

        } catch (Exception e) {

            e.printStackTrace(); // ✅ DEBUG CLAVE

            Map<String, String> error = new HashMap<>();
            error.put("message", "Error al crear solicitud");

            return ResponseEntity.badRequest().body(error);
        }
    }

    // 2. OBTENER SOLICITUDES DEL CLUB
    @GetMapping("/solicitudes")
    public ResponseEntity<?> getSolicitudes(HttpServletRequest request) {

        try {

            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            return ResponseEntity.ok(
                    clubService.getSolicitudesClub(email));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.badRequest().body("Error obteniendo solicitudes");
        }
    }

    @PutMapping("/solicitud/{id}")
    public ResponseEntity<?> resolverSolicitud(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {

            // ✅ 1. obtener email del JWT
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            // ✅ 2. validar acción
            String accion = body.get("accion");

            if (accion == null ||
                    (!accion.equals("aceptado") && !accion.equals("rechazado"))) {
                return ResponseEntity.badRequest().body("Acción inválida");
            }

            // ✅ 3. enviar al service (IMPORTANTE ✅)
            clubService.resolverSolicitud(id, accion, email);

            return ResponseEntity.ok("Solicitud actualizada");

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

}
