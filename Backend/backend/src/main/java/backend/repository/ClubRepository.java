package backend.repository;

import backend.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository 
        extends JpaRepository<Club, Long>, ClubRepositoryCustom {
}