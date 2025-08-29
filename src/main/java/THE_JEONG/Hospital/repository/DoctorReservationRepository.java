package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DoctorReservationRepository extends JpaRepository<Reservation, Long> {
    Page<Reservation> findByDoctorIdAndDate(Long doctorId, LocalDate date, Pageable pageable);
    Page<Reservation> findByDoctorIdAndUserNameContaining(Long doctorId, String userName, Pageable pageable);
    Page<Reservation> findByDoctorId(Long doctorId, Pageable pageable);
}
