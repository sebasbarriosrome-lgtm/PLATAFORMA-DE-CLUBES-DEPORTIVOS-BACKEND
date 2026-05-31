package backend.controller;

import backend.service.EntrenadorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/entrenador")
@CrossOrigin("*")
public class EntrenadorController {

    private final EntrenadorService entrenadorService;

    public EntrenadorController(EntrenadorService entrenadorService) {
        this.entrenadorService = entrenadorService;
    }

    // ─────────────────────────────────────────────
    // PANEL (existente)
    // ─────────────────────────────────────────────

    @GetMapping("/panel")
    public ResponseEntity<?> getPanelEntrenador(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) return ResponseEntity.status(401).body("No autorizado");

            Map<String, Object> data = entrenadorService.getPanelEntrenador(email);
            if (data == null) return ResponseEntity.status(404).body("Panel no disponible");

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // SESIONES
    // ─────────────────────────────────────────────

    @GetMapping("/sesiones")
    public ResponseEntity<?> getSesiones(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) return ResponseEntity.status(401).body("No autorizado");

            return ResponseEntity.ok(entrenadorService.getSesiones(email));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/sesiones")
    public ResponseEntity<?> crearSesion(HttpServletRequest request,
                                          @RequestBody Map<String, Object> body) {
        try {
            String email = (String) request.getAttribute("email");
            if (email == null) return ResponseEntity.status(401).body("No autorizado");

            return ResponseEntity.ok(entrenadorService.crearSesion(email, body));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/sesiones/{sesionId}/estado")
    public ResponseEntity<?> actualizarEstadoSesion(@PathVariable Long sesionId,
                                                     @RequestBody Map<String, Object> body) {
        try {
            String estado = body.get("estado").toString();
            return ResponseEntity.ok(entrenadorService.actualizarEstadoSesion(sesionId, estado));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/sesiones/{sesionId}")
    public ResponseEntity<?> eliminarSesion(@PathVariable Long sesionId) {
        try {
            return ResponseEntity.ok(entrenadorService.eliminarSesion(sesionId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // ACTIVIDADES (catálogo global)
    // ─────────────────────────────────────────────

    @GetMapping("/actividades")
    public ResponseEntity<?> getActividades() {
        try {
            return ResponseEntity.ok(entrenadorService.getActividades());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/actividades")
    public ResponseEntity<?> crearActividad(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(entrenadorService.crearActividad(body));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // ACTIVIDADES DE UNA SESIÓN
    // ─────────────────────────────────────────────

    @GetMapping("/sesiones/{sesionId}/actividades")
    public ResponseEntity<?> getActividadesBySesion(@PathVariable Long sesionId) {
        try {
            return ResponseEntity.ok(entrenadorService.getActividadesBySesion(sesionId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/sesiones/{sesionId}/actividades")
    public ResponseEntity<?> agregarActividad(@PathVariable Long sesionId,
                                               @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(entrenadorService.agregarActividadASesion(sesionId, body));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/sesiones/actividades/{sesionActividadId}")
    public ResponseEntity<?> quitarActividad(@PathVariable Long sesionActividadId) {
        try {
            return ResponseEntity.ok(entrenadorService.quitarActividadDeSesion(sesionActividadId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // ASISTENCIA
    // ─────────────────────────────────────────────

    @GetMapping("/sesiones/{sesionId}/asistencia")
    public ResponseEntity<?> getAsistencia(@PathVariable Long sesionId) {
        try {
            return ResponseEntity.ok(entrenadorService.getAsistenciaBySesion(sesionId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/sesiones/{sesionId}/asistencia")
    public ResponseEntity<?> registrarAsistencia(@PathVariable Long sesionId,
                                                  @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(entrenadorService.registrarAsistencia(sesionId, body));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/sesiones/{sesionId}/asistencia/lote")
    public ResponseEntity<?> registrarAsistenciaLote(@PathVariable Long sesionId,
                                                      @RequestBody List<Map<String, Object>> lista) {
        try {
            return ResponseEntity.ok(entrenadorService.registrarAsistenciaLote(sesionId, lista));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
// MÉTRICAS
// ─────────────────────────────────────────────

@GetMapping("/sesiones/{sesionId}/metricas")
public ResponseEntity<?> getMetricasBySesion(@PathVariable Long sesionId) {
    try {
        return ResponseEntity.ok(entrenadorService.getMetricasBySesion(sesionId));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

@GetMapping("/deportistas/{deportistaId}/metricas")
public ResponseEntity<?> getMetricasByDeportista(@PathVariable Long deportistaId) {
    try {
        return ResponseEntity.ok(entrenadorService.getMetricasByDeportista(deportistaId));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

@PostMapping("/sesiones/{sesionId}/metricas")
public ResponseEntity<?> registrarMetrica(HttpServletRequest request,
                                           @PathVariable Long sesionId,
                                           @RequestBody Map<String, Object> body) {
    try {
        String email = (String) request.getAttribute("email");
        if (email == null) return ResponseEntity.status(401).body("No autorizado");
        return ResponseEntity.ok(entrenadorService.registrarMetrica(email, sesionId, body));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

@DeleteMapping("/metricas/{metricaId}")
public ResponseEntity<?> eliminarMetrica(@PathVariable Long metricaId) {
    try {
        return ResponseEntity.ok(entrenadorService.eliminarMetrica(metricaId));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

// ─────────────────────────────────────────────
// RENDIMIENTO
// ─────────────────────────────────────────────

@GetMapping("/rendimiento/evolucion/{deportistaId}")
public ResponseEntity<?> getEvolucion(
        @PathVariable Long deportistaId,
        @RequestParam(required = false) Long actividadId) {
    try {
        return ResponseEntity.ok(entrenadorService.getEvolucionDeportista(deportistaId, actividadId));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

@GetMapping("/rendimiento/comparacion/{sesionId}")
public ResponseEntity<?> getComparacion(
        @PathVariable Long sesionId,
        @RequestParam(required = false) Long actividadId) {
    try {
        return ResponseEntity.ok(entrenadorService.getComparacionSesion(sesionId, actividadId));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

@GetMapping("/rendimiento/asistencia/{grupoId}")
public ResponseEntity<?> getAsistenciaGrupo(@PathVariable Long grupoId) {
    try {
        return ResponseEntity.ok(entrenadorService.getAsistenciaDeportistas(grupoId));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

@GetMapping("/rendimiento/promedios/{grupoId}")
public ResponseEntity<?> getPromedios(@PathVariable Long grupoId) {
    try {
        return ResponseEntity.ok(entrenadorService.getPromediosSesiones(grupoId));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

}