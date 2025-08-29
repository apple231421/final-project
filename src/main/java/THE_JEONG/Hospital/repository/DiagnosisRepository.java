package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.Diagnosis;
import THE_JEONG.Hospital.entity.Reservation;
import THE_JEONG.Hospital.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiagnosisRepository extends JpaRepository<Diagnosis,Long> {
    Diagnosis findByReservation(Reservation reservation);

    @Query("SELECT d FROM Diagnosis d WHERE d.user = :user AND d.reservation.state = 'D'")
    Page<Diagnosis> findByUserAndReservationStateD(@Param("user") User user, Pageable pageable);

    @Query("""
    SELECT d FROM Diagnosis d
    WHERE d.reservation.state = 'D'
      AND (:name IS NULL OR d.user.name LIKE %:name%)
""")
    Page<Diagnosis> searchByPatientNameAndReservationStateD(@Param("name") String name, Pageable pageable);

    void deleteByUser(User user);
    void deleteByReservation(Reservation reservation);

    Diagnosis findByReservationId(Long id);
}
