package backend.controller;

import backend.entity.Club;
import backend.service.ClubService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
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
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage() != null ? e.getMessage() : "Error al actualizar club");
            return ResponseEntity.badRequest().body(error);
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
                    peso,
                    estatura,
                    experiencia,
                    especialidad);

            return ResponseEntity.ok("Solicitud enviada correctamente");

        } catch (Exception e) {

            e.printStackTrace(); // ✅ DEBUG CLAVE

            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage() != null ? e.getMessage() : "Error al crear solicitud");

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

    @GetMapping("/solicitudes/rol")
    public ResponseEntity<?> getSolicitudesPorRol(
            @RequestParam String rol,
            HttpServletRequest request) {

        try {

            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            return ResponseEntity.ok(
                    clubService.getSolicitudesPorRol(email, rol));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error filtrando solicitudes");
        }
    }

    @GetMapping("/entrenadores")
    public ResponseEntity<?> getEntrenadoresClub(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            return ResponseEntity.ok(clubService.getEntrenadoresClub(email));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/deportistas")
    public ResponseEntity<?> getDeportistasClub(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            return ResponseEntity.ok(clubService.getDeportistasClub(email));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/entrenadores/{id}")
    public ResponseEntity<?> eliminarEntrenador(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            clubService.eliminarEntrenador(id, email);
            return ResponseEntity.ok(Map.of("message", "Entrenador eliminado"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/deportistas/{id}")
    public ResponseEntity<?> eliminarDeportista(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            clubService.eliminarDeportista(id, email);
            return ResponseEntity.ok(Map.of("message", "Deportista eliminado"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/horarios")
    public ResponseEntity<?> getHorariosClub(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            return ResponseEntity.ok(clubService.getHorariosClub(email));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error obteniendo horarios");
        }
    }

    @GetMapping("/horarios/slug/{slug}")
    public ResponseEntity<?> getHorariosBySlug(@PathVariable String slug) {
        try {
            return ResponseEntity.ok(clubService.getHorariosClubBySlug(slug));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error obteniendo horarios");
        }
    }

    @PostMapping("/horarios")
    public ResponseEntity<?> crearHorario(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            String dia = body.get("dia");
            String horaInicio = body.get("horaInicio");
            String horaFin = body.get("horaFin");
            String ubicacion = body.get("ubicacion");
            String descripcion = body.get("descripcion");
            // nuevo: destinatario
            Long grupoId = body.get("grupoId") != null ? Long.valueOf(body.get("grupoId")) : null;
            String categoria = body.get("categoria");

            if (dia == null || horaInicio == null || horaFin == null) {
                return ResponseEntity.badRequest().body("Datos de horario incompletos");
            }

            Long horarioId = clubService.crearHorario(
                    email,
                    dia,
                    horaInicio,
                    horaFin,
                    descripcion,
                    ubicacion,
                    grupoId,
                    categoria);

            return ResponseEntity.ok(Map.of("id", horarioId));
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/horarios/{id}")
    public ResponseEntity<?> actualizarHorario(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            String dia = body.get("dia");
            String horaInicio = body.get("horaInicio");
            String horaFin = body.get("horaFin");
            String ubicacion = body.get("ubicacion");
            String descripcion = body.get("descripcion");
            Long grupoId = body.get("grupoId") != null ? Long.valueOf(body.get("grupoId")) : null;
            String categoria = body.get("categoria");

            if (dia == null || horaInicio == null || horaFin == null) {
                return ResponseEntity.badRequest().body("Datos de horario incompletos");
            }

            clubService.actualizarHorario(
                    id,
                    email,
                    dia,
                    horaInicio,
                    horaFin,
                    descripcion,
                    ubicacion,
                    grupoId,
                    categoria);

            return ResponseEntity.ok("Horario actualizado");
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/horarios/{id}")
    public ResponseEntity<?> eliminarHorario(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            clubService.eliminarHorario(id, email);
            return ResponseEntity.ok("Horario eliminado");
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(
            @RequestParam(required = false) String search,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            return ResponseEntity.ok(clubService.getCategories(email, search));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            var category = clubService.getCategoryById(id);
            if (category == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            String nombre = (String) body.get("nombre");
            String descripcion = (String) body.get("descripcion");
            List<?> entrenadorIds = (List<?>) body.get("entrenadorIds");
            var ids = entrenadorIds == null ? List.<Long>of()
                    : entrenadorIds.stream()
                            .filter(it -> it != null)
                            .map(it -> it instanceof Number ? ((Number) it).longValue() : Long.valueOf(it.toString()))
                            .toList();
            clubService.createCategory(email, nombre, descripcion, ids);
            return ResponseEntity.ok(Map.of("message", "Categoría creada"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            String nombre = (String) body.get("nombre");
            String descripcion = (String) body.get("descripcion");
            List<?> entrenadorIds = (List<?>) body.get("entrenadorIds");
            var ids = entrenadorIds == null ? List.<Long>of()
                    : entrenadorIds.stream()
                            .filter(it -> it != null)
                            .map(it -> it instanceof Number ? ((Number) it).longValue() : Long.valueOf(it.toString()))
                            .toList();
            clubService.updateCategory(id, email, nombre, descripcion, ids);
            return ResponseEntity.ok(Map.of("message", "Categoría actualizada"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/categories/{id}/entrenadores")
    public ResponseEntity<?> assignCategoryEntrenadores(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            List<?> entrenadorIds = (List<?>) body.get("entrenadorIds");
            var ids = entrenadorIds == null ? List.<Long>of()
                    : entrenadorIds.stream()
                            .filter(it -> it != null)
                            .map(it -> it instanceof Number ? ((Number) it).longValue() : Long.valueOf(it.toString()))
                            .toList();
            clubService.assignEntrenadoresToCategory(id, email, ids);
            return ResponseEntity.ok(Map.of("message", "Entrenadores asignados a la categoría"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            clubService.deleteCategory(id, email);
            return ResponseEntity.ok(Map.of("message", "Categoría eliminada"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/groups")
    public ResponseEntity<?> getGroups(
            @RequestParam(required = false) String search,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            return ResponseEntity.ok(clubService.getGroups(email, search));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<?> getGroupById(@PathVariable Long id) {
        try {
            var group = clubService.getGroupById(id);
            if (group == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            String nombre = (String) body.get("nombre");
            String descripcion = (String) body.get("descripcion");
            List<?> entrenadorIds = (List<?>) body.get("entrenadorIds");
            var ids = entrenadorIds == null ? List.<Long>of()
                    : entrenadorIds.stream()
                            .filter(it -> it != null)
                            .map(it -> it instanceof Number ? ((Number) it).longValue() : Long.valueOf(it.toString()))
                            .toList();
            clubService.createGroup(email, nombre, descripcion, ids);
            return ResponseEntity.ok(Map.of("message", "Grupo creado"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<?> updateGroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            String nombre = (String) body.get("nombre");
            String descripcion = (String) body.get("descripcion");
            List<?> entrenadorIds = (List<?>) body.get("entrenadorIds");
            var ids = entrenadorIds == null ? List.<Long>of()
                    : entrenadorIds.stream()
                            .filter(it -> it != null)
                            .map(it -> it instanceof Number ? ((Number) it).longValue() : Long.valueOf(it.toString()))
                            .toList();
            clubService.updateGroup(id, email, nombre, descripcion, ids);
            return ResponseEntity.ok(Map.of("message", "Grupo actualizado"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/groups/{id}/entrenadores")
    public ResponseEntity<?> assignGroupEntrenadores(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            List<?> entrenadorIds = (List<?>) body.get("entrenadorIds");
            var ids = entrenadorIds == null ? List.<Long>of()
                    : entrenadorIds.stream()
                            .filter(it -> it != null)
                            .map(it -> it instanceof Number ? ((Number) it).longValue() : Long.valueOf(it.toString()))
                            .toList();
            clubService.assignEntrenadoresToGroup(id, email, ids);
            return ResponseEntity.ok(Map.of("message", "Entrenadores asignados al grupo"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<?> deleteGroup(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }
            clubService.deleteGroup(id, email);
            return ResponseEntity.ok(Map.of("message", "Grupo eliminado"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
