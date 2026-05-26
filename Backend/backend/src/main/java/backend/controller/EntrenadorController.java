package backend.controller;

import backend.service.EntrenadorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/entrenador")
@CrossOrigin("*")
public class EntrenadorController {

    private final EntrenadorService entrenadorService;

    public EntrenadorController(EntrenadorService entrenadorService) {
        this.entrenadorService = entrenadorService;
    }

    @GetMapping("/panel")
    public ResponseEntity<?> getPanelEntrenador(HttpServletRequest request) {
        try {
            String email = (String) request.getAttribute("email");

            if (email == null) {
                return ResponseEntity.status(401).body("No autorizado");
            }

            Map<String, Object> data = entrenadorService.getPanelEntrenador(email);

            if (data == null) {
                return ResponseEntity.status(404).body("Panel no disponible");
            }

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
