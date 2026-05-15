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

    @PostMapping
public ResponseEntity<?> crearClub(
        @RequestBody Club club,
        HttpServletRequest request
) {

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
                club.getContacto()
        );

        return ResponseEntity.ok("Club creado correctamente");

    } catch (Exception e) {

        Map<String, String> error = new HashMap<>();

        if (e.getMessage().contains("El club ya existe")) {
            error.put("message", "El club ya existe en esa ciudad");
        } else {
            error.put("message", "Error al crear club");
        }

        return ResponseEntity.badRequest().body(error);
    }
}

}