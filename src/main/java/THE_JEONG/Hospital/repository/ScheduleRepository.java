package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.Schedule;
import THE_JEONG.Hospital.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDoctor(Doctor doctor);

    List<Schedule> findByDoctorIdAndDayOfWeek(Long doctorId, String dayOfWeek);

    void deleteByDoctor(Doctor doctor);
}
