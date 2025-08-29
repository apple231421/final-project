package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.DisableSchedule;
import THE_JEONG.Hospital.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DisableScheduleRepository extends JpaRepository<DisableSchedule, Long> {
    List<DisableSchedule> findByDoctorId(Long doctorId);

    boolean existsByDoctorAndDate(Doctor doctor, LocalDate date);

    boolean existsByDoctorIdAndDate(Long doctorId, LocalDate date);

    void deleteByDoctor(Doctor doctor);
}
