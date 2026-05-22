package backend.repository;

import java.util.List;

public interface ClubRepositoryCustom {

    void crearClub(
            String nombre,
            String ciudad,
            String descripcion,
            String logoUrl,
            String bannerUrl,
            String colorPrimario,
            String colorSecundario,
            String contacto);

    void crearUsuarioClub(
            Long usuarioId,
            Long clubId,
            String rol);

    void actualizarClub(
            Long clubId,
            String descripcion,
            String logoUrl,
            String bannerUrl,
            String colorPrimario,
            String colorSecundario);

    Object getPanelClubData(Long usuarioId);

    // ✅ crear solicitud
    Long crearSolicitud(Long usuarioId, Long clubId, String rol, String mensaje);

    // ✅ solicitud deportiva
    void crearSolicitudDeportiva(
            Long solicitudId,
            Double peso,
            Long estatura,
            String experiencia,
            String especialidad);

    // ✅ obtener solicitudes
    List<Object[]> getSolicitudesByClub(Long clubId);

    // ✅ actualizar estado de solicitud
    void actualizarEstadoSolicitud(Long solicitudId, String estado);

}