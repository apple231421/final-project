package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.constant.ReservationState;
import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.entity.Reservation;
import THE_JEONG.Hospital.entity.Schedule;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.repository.DisableScheduleRepository;
import THE_JEONG.Hospital.repository.DoctorRepository;
import THE_JEONG.Hospital.repository.ReservationRepository;
import THE_JEONG.Hospital.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ScheduleRepository scheduleRepository;
    private final ReservationRepository reservationRepository;
    private final DoctorRepository doctorRepository;
    private final DisableScheduleRepository disableScheduleRepository;

    /** ✅ 부서 단위 예약 생성 */
    public void reserve(LocalDate date, String department, User user) {
        Reservation reservation = Reservation.builder()
                .date(date)
                .department(department)
                .user(user)
                .state(ReservationState.R)   // 기본 상태: 예약중
                .build();
        reservationRepository.save(reservation);
    }

    /** ✅ 특정 부서, 특정 날짜 예약 개수 조회 */
    public int getReservationCount(LocalDate date, String department) {
        return reservationRepository.countByDateAndDepartment(date, department);
    }

    /** ✅ 사용자 예약 목록(예약중 상태만) */
    public List<Reservation> getReservationsByUser(User user) {
        return reservationRepository.findByUserAndState(user, ReservationState.R);
    }

    /** ✅ 예약 취소 */
    public boolean cancelReservation(Long reservationId, String email) {
        Optional<Reservation> optional = reservationRepository.findById(reservationId);
        if (optional.isPresent()) {
            Reservation reservation = optional.get();
            if (reservation.getUser().getEmail().equals(email)) {
                reservation.setState(ReservationState.C); // 취소 상태로 변경
                reservationRepository.save(reservation);
                return true;
            }
        }
        return false;
    }

    /** ✅ 특정 의사/날짜/시간에 예약 생성 */
    public void reserveWithDoctorAndTime(LocalDate date, String time, Doctor doctor, User user, String department) {
        Reservation reservation = Reservation.builder()
                .date(date)
                .time(time)
                .doctor(doctor)
                .user(user)
                .department(doctor.getDepartment().getName())
                .state(ReservationState.R)   // 기본 상태: 예약중
                .build();
        reservationRepository.save(reservation);
    }

    /** ✅ 특정 의사/날짜 예약 가능 여부 조회 */
    public Map<String, Object> getAvailabilityByDoctorAndDate(Long doctorId, LocalDate date) {
        Map<String, Object> result = new HashMap<>();

        // 1. 휴무일 체크
        boolean isHoliday = disableScheduleRepository.existsByDoctorIdAndDate(doctorId, date);
        result.put("isHoliday", isHoliday);
        if (isHoliday) {
            result.put("times", Collections.emptyMap());
            return result;
        }

        // 2. 해당 요일 스케줄 조회
        String dayOfWeek = getKoreanDayOfWeek(date.getDayOfWeek());
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        if (schedules.isEmpty()) {
            result.put("times", Collections.emptyMap());
            return result;
        }

        // 3. 이미 예약된 시간 (예약중 상태만)
        List<String> reservedTimes = reservationRepository.findByDoctorIdAndDate(doctorId, date)
                .stream()
                .filter(r -> r.getState() == ReservationState.R)
                .map(Reservation::getTime)
                .toList();

        Set<String> reservedSet = reservedTimes.stream()
                .map(this::normalizeTime)
                .collect(Collectors.toSet());

        // 4. 예약 가능/불가 시간 매핑
        Map<String, Integer> timeMap = new LinkedHashMap<>();
        for (Schedule s : schedules) {
            LocalTime start = s.getStartTime();
            LocalTime end = s.getEndTime();
            for (LocalTime t = start; t.isBefore(end); t = t.plusMinutes(30)) {
                // 점심시간 제외
                if (t.getHour() == 12 || t.getHour() == 13) continue;

                String timeStr = String.format("%02d:%02d", t.getHour(), t.getMinute());
                timeMap.put(timeStr, reservedSet.contains(timeStr) ? 1 : 0);
            }
        }

        result.put("times", timeMap);
        return result;
    }

    /** ✅ 시간 포맷 정규화 */
    private String normalizeTime(String time) {
        if (time == null) return "";
        String[] parts = time.split(":");
        if (parts.length < 2) return time;
        return String.format("%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /** ✅ 요일 한글 변환 */
    public String getKoreanDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    /** ✅ 의사 휴무일 여부 체크 */
    public boolean isHoliday(Long doctorId, LocalDate date) {
        return disableScheduleRepository.existsByDoctorIdAndDate(doctorId, date);
    }

    /** ✅ 이미 예약된 시간 리스트 조회 */
    public List<String> getReservedTimes(Long doctorId, LocalDate date) {
        return reservationRepository.findTimeByDoctorIdAndDate(doctorId, date);
    }

    /** ✅ 특정 날짜가 이미 다 찼는지 여부 */
    public boolean isFullyBooked(LocalDate date, Long doctorId) {
        int maxTimeSlots = 1; // 하루 예약 가능 개수 (임시)
        int currentReservations = reservationRepository.countByDoctorIdAndDate(doctorId, date);
        return currentReservations >= maxTimeSlots;
    }

    /** ✅ 1개월간 예약 꽉 찬 날짜 조회 */
    public List<String> getFullyBookedDates(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusMonths(1);

        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor == null) return List.of();

        List<String> fullDates = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(end); date = date.plusDays(1)) {
            // 휴무일 제외
            if (disableScheduleRepository.existsByDoctorAndDate(doctor, date)) continue;

            if (isFullyBooked(date, doctorId)) {
                fullDates.add(date.toString());
            }
        }
        return fullDates;
    }

    /** ✅ 특정 시간에 예약 존재 여부 */
    public boolean existsByDoctorAndDateTime(Doctor doctor, LocalDate date, String time) {
        return reservationRepository.existsByDoctorAndDateAndTime(doctor, date, time);
    }

    /** ✅ 전체 예약 목록 조회 */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll(); // 모든 상태 포함
    }

    /** ✅ 예약 가능한 시간 리스트 조회 */
    public List<String> getAvailableTimes(Long doctorId, LocalDate date) {
        // 1. 휴무일 체크
        if (disableScheduleRepository.existsByDoctorIdAndDate(doctorId, date)) {
            return List.of(); // 휴가면 예약 불가
        }

        // 2. 요일 구하기
        String dayOfWeek = getKoreanDayOfWeek(date.getDayOfWeek());

        // 3. 진료 가능 시간 조회
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);

        // 4. 이미 예약된 시간 조회
        List<String> reservedTimes = reservationRepository.findTimeByDoctorIdAndDate(doctorId, date);

        // 5. 오늘 날짜면 2시간 전 마감
        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        ZonedDateTime nowKST = ZonedDateTime.now(seoulZone);
        boolean isToday = date.equals(nowKST.toLocalDate());
        LocalTime cutoffTime = nowKST.toLocalTime().plusHours(2);

        List<String> availableTimes = new ArrayList<>();

        for (Schedule s : schedules) {
            LocalTime start = s.getStartTime();
            LocalTime end = s.getEndTime();
            for (LocalTime t = start; t.isBefore(end); t = t.plusMinutes(30)){
                String timeStr = t.toString().substring(0, 5);

                // 오늘은 2시간 이후만 가능
                if (isToday && t.isBefore(cutoffTime)) {
                    continue;
                }
                // 이미 예약된 시간은 제외
                if (!reservedTimes.contains(timeStr)) {
                    availableTimes.add(timeStr);
                }
            }
        }
        return availableTimes;
    }

    /** ✅ 특정 시간 이미 예약중인지 여부 */
    public boolean isAlreadyReserved(Doctor doctor, LocalDate date, String time) {
        return reservationRepository.findByDoctorIdAndDate(doctor.getId(), date)
                .stream()
                .anyMatch(r ->
                        r.getTime().equals(time) &&
                                r.getState() == ReservationState.R
                );
    }

    public List<Reservation> getAllReservationsByUser(User user) {
        return reservationRepository.findByUserOrderByDateDesc(user);
    }

}
