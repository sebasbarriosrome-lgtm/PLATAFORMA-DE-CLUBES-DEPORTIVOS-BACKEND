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

        // ✅ horarios de entrenamiento
        Long crearHorarioEntrenamiento(
                        Long clubId,
                        Long grupoId,
                        String dia,
                        String horaInicio,
                        String horaFin,
                        String descripcion,
                        String ubicacion,
                        Long categoriaId);

        void actualizarHorarioEntrenamiento(
                        Long horarioId,
                        Long grupoId,
                        String dia,
                        String horaInicio,
                        String horaFin,
                        String descripcion,
                        String ubicacion,
                        Long categoriaId);

        void eliminarHorarioEntrenamiento(Long horarioId);

        List<Object[]> getHorariosByClubSlug(String slug);

        List<Object[]> getHorariosByClubId(Long clubId);

        // Categorías
        Long crearCategoria(Long clubId, String nombre, String descripcion);

        void actualizarCategoria(Long categoriaId, String nombre, String descripcion);

        void eliminarCategoria(Long categoriaId);

        List<Object[]> getCategoriasByClub(Long clubId, String search);

        Object[] getCategoriaById(Long categoriaId);

        void clearEntrenadoresCategoria(Long categoriaId);

        void insertEntrenadorCategoria(Long categoriaId, Long entrenadorId);

        // Grupos
        Long crearGrupoDeportivo(Long clubId, String nombre, String descripcion);

        void actualizarGrupoDeportivo(Long grupoId, String nombre, String descripcion);

        void eliminarGrupoDeportivo(Long grupoId);

        List<Object[]> getGruposByClub(Long clubId, String search);

        Object[] getGrupoById(Long grupoId);

        void clearEntrenadoresGrupo(Long grupoId);

        void insertGrupoEntrenador(Long grupoId, Long entrenadorId);

}