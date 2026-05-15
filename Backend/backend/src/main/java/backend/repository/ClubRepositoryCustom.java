package backend.repository;

public interface ClubRepositoryCustom {

    void crearClub(
        String nombre,
        String ciudad,
        String descripcion,
        String logoUrl,
        String bannerUrl,
        String colorPrimario,
        String colorSecundario,
        String contacto
    );
}