package backend.service;

import backend.repository.ClubRepository;
import org.springframework.stereotype.Service;

@Service
public class ClubService {

    private final ClubRepository clubRepository;

    public ClubService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

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

        clubRepository.crearClub(
                nombre,
                ciudad,
                descripcion,
                logoUrl,
                bannerUrl,
                colorPrimario,
                colorSecundario,
                contacto
        );
    }
}