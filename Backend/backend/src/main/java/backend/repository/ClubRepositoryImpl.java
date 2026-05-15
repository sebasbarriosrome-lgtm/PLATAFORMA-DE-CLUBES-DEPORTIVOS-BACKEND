package backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

@Repository
public class ClubRepositoryImpl implements ClubRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void crearClub(
            String nombre,
            String ciudad,
            String descripcion,
            String logoUrl,
            String bannerUrl,
            String colorPrimario,
            String colorSecundario,
            String contacto
    ) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("sp_create_club");

        query.registerStoredProcedureParameter("p_nombre", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_ciudad", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_descripcion", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_logo_url", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_banner_url", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_color_primario", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_color_secundario", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("p_contacto", String.class, jakarta.persistence.ParameterMode.IN);

        query.setParameter("p_nombre", nombre);
        query.setParameter("p_ciudad", ciudad);
        query.setParameter("p_descripcion", descripcion);
        query.setParameter("p_logo_url", logoUrl);
        query.setParameter("p_banner_url", bannerUrl);
        query.setParameter("p_color_primario", colorPrimario);
        query.setParameter("p_color_secundario", colorSecundario);
        query.setParameter("p_contacto", contacto);

        query.execute();
    }
}