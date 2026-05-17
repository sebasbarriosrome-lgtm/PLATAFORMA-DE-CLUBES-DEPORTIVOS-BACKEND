package backend.repository;

import backend.entity.Club;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository 
        extends JpaRepository<Club, Long>, ClubRepositoryCustom {
        List<Club> findByDeletedAtIsNull();
        Optional<Club> findBySlug(String slug);

        

}