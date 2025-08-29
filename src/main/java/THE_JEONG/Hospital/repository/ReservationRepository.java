package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.constant.ReservationState;
import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.entity.Reservation;
import THE_JEONG.Hospital.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    int countByDateAndDepartment(LocalDate date, String department); // 예약수 집계

    List<Reservation> findByUser(User user);                          // 사용자별 예약 조회

    List<Reservation> findByDoctorIdAndDate(Long doctorId, LocalDate date);

    List<Reservation> findByDoctorId(Long doctorId);

    List<Reservation> findByDoctorIdAndUserNameContaining(Long id, String searchName);

    boolean existsByDoctorAndDateAndTime(Doctor doctor, LocalDate date, String time);

    int countByDoctorIdAndDate(Long doctorId, LocalDate date);

    List<Reservation> findByUser_UserId(Integer userId);

    List<Reservation> findByUserAndState(User user, ReservationState reservationState);

    List<Reservation> findByDateBeforeAndState(LocalDate today, ReservationState reservationState);

    // ✅ 특정 날짜, 의사, 예약중(R) 상태인 예약된 시간 목록
    @Query("SELECT r.time FROM Reservation r " +
            "WHERE r.doctor.id = :doctorId " +
            "AND r.date = :date " +
            "AND r.state = 'R'")
    List<String> findTimeByDoctorIdAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date
    );

    void deleteByUser(User user);

    // ✅ 여러 상태 포함해서 중복 체크할 때 사용 가능 (기존 유지)
    boolean existsByDoctorIdAndDateAndTimeAndStateIn(
            Long doctorId,
            LocalDate date,
            String time,
            List<ReservationState> state
    );

    void deleteByDoctor(Doctor doctor);

    // ✅ **새로운 예약중(R) 상태만 체크하는 메서드 추가**
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
            "WHERE r.doctor = :doctor " +
            "AND r.date = :date " +
            "AND r.time = :time " +
            "AND r.state = 'R'")
    boolean existsActiveReservation(
            @Param("doctor") Doctor doctor,
            @Param("date") LocalDate date,
            @Param("time") String time
    );

    List<Reservation> findByState(ReservationState state);

    List<Reservation> findByUserOrderByDateDesc(User user);
}
