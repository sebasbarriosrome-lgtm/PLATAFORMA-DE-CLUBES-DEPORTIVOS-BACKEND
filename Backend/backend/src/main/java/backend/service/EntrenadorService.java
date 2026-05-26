package backend.service;

import backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EntrenadorService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UsuarioRepository usuarioRepository;

    public EntrenadorService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Map<String, Object> getPanelEntrenador(String email) {

        var usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Long usuarioId = usuario.getId();

        // 1) obtener grupos asignados al entrenador
        List<Object[]> grupos = entityManager
                .createNativeQuery(
                        "SELECT g.id, g.nombre, g.club_id FROM grupo_entrenador ge JOIN grupo_deportivo g ON ge.grupo_id = g.id WHERE ge.entrenador_id IN (SELECT e.id FROM entrenador e WHERE e.usuario_club_id IN (SELECT uc.id FROM usuario_club uc WHERE uc.usuario_id = ?))")
                .setParameter(1, usuarioId)
                .getResultList();

        List<Map<String, Object>> gruposResponse = new ArrayList<>();

        for (Object[] g : grupos) {
            Long grupoId = ((Number) g[0]).longValue();
            String nombre = (String) g[1];
            Long clubId = g[2] != null ? ((Number) g[2]).longValue() : null;

            Map<String, Object> grupoMap = new HashMap<>();
            grupoMap.put("id", grupoId);
            grupoMap.put("nombre", nombre);

            // obtener deportistas del grupo
            List<Object[]> atletas = entityManager
                    .createNativeQuery(
                            "SELECT u.id, u.nombre, u.apellido FROM deportista d JOIN usuario_club uc ON uc.id = d.usuario_club_id JOIN usuario u ON u.id = uc.usuario_id WHERE d.grupo_id = ?")
                    .setParameter(1, grupoId)
                    .getResultList();

            List<Map<String, Object>> atletasResp = new ArrayList<>();

            for (Object[] a : atletas) {
                Map<String, Object> atleta = new HashMap<>();
                atleta.put("id", ((Number) a[0]).longValue());
                atleta.put("nombre", a[1] + " " + a[2]);
                atletasResp.add(atleta);
            }

            grupoMap.put("deportistas", atletasResp);

            // obtener horarios del grupo (usamos el SP si existe)
            List<Object[]> horarios = entityManager
                    .createNativeQuery("CALL sp_get_horarios_by_grupo(?)")
                    .setParameter(1, grupoId)
                    .getResultList();

            List<Map<String, Object>> horariosResp = new ArrayList<>();

            for (Object[] h : horarios) {
                Map<String, Object> horario = new HashMap<>();
                horario.put("id", h[0]);
                horario.put("dia", h[1]);
                horario.put("horaInicio", h[2]);
                horario.put("horaFin", h[3]);
                horario.put("descripcion", h[4]);
                horario.put("ubicacion", h[5]);
                horario.put("estado", h[6]);
                horariosResp.add(horario);
            }

            grupoMap.put("horarios", horariosResp);

            // obtener sesiones programadas para el grupo (últimas 10)
            List<Object[]> sesiones = entityManager
                    .createNativeQuery(
                            "SELECT id, fecha, hora_inicio, hora_fin, estado, descripcion FROM sesion_entrenamiento WHERE grupo_id = ? ORDER BY fecha DESC LIMIT 10")
                    .setParameter(1, grupoId)
                    .getResultList();

            List<Map<String, Object>> sesionesResp = new ArrayList<>();

            for (Object[] s : sesiones) {
                Map<String, Object> ses = new HashMap<>();
                ses.put("id", ((Number) s[0]).longValue());
                ses.put("fecha", s[1]);
                ses.put("horaInicio", s[2]);
                ses.put("horaFin", s[3]);
                ses.put("estado", s[4]);
                ses.put("descripcion", s[5]);
                sesionesResp.add(ses);
            }

            grupoMap.put("sesiones", sesionesResp);

            gruposResponse.add(grupoMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("grupos", gruposResponse);
        response.put("coachEmail", email);

        return response;
    }

}
