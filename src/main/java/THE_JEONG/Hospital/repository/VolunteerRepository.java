package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.entity.Volunteer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

    List<Volunteer> findAllByApplicantsContaining(User user);

    Page<Volunteer> findByTitleContaining(Pageable pageable, String title);
}
