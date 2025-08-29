package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.dto.ReservationDto;
import THE_JEONG.Hospital.entity.*;
import THE_JEONG.Hospital.repository.DepartmentRepository;
import THE_JEONG.Hospital.repository.DisableScheduleRepository;
import THE_JEONG.Hospital.repository.DoctorRepository;
import THE_JEONG.Hospital.repository.UserRepository;
import THE_JEONG.Hospital.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    private final DepartmentRepository departmentRepository;
    private final ReservationService reservationService;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DisableScheduleRepository disableScheduleRepository;
    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    @GetMapping("/reservation")
    public String reservationPage(
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String doctorName,
            Model model,
            Authentication authentication
    ) {
        String email = extractEmail(authentication);
        if (email == null) return "redirect:/login";

        User user = userRepository.findByEmail(email);
        model.addAttribute("user", user);

        model.addAttribute("dept", dept);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("doctorName", doctorName);

        return "reservation";
    }
    @PostMapping("/reservation")
    @ResponseBody
    public ResponseEntity<Map<String, String>> reserve(@RequestBody ReservationDto dto, Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "fail", "message", "로그인이 필요합니다."));
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "fail", "message", "사용자 정보를 찾을 수 없습니다."));
        }

        Doctor doctor = doctorRepository.findById(dto.getDoctorId()).orElse(null);
        if (doctor == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "fail", "message", "의사를 찾을 수 없습니다."));
        }

        log.info("예약 시도: 로그인 사용자 이메일 = {}, 대상 의사 이메일 = {}", email, doctor.getEmail());

        // 의사 본인이 본인에게 예약하는 경우 방지
        if (doctor.getEmail() != null && doctor.getEmail().equals(email)) {
            log.warn("본인 예약 시도 차단: 의사 이메일 = {}", email);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "fail", "message", "의사 본인은 본인에게 예약할 수 없습니다."));
        }

        // 날짜 파싱
        LocalDate date;
        try {
            String[] parts = dto.getDate().split("-");
            date = LocalDate.of(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "fail", "message", "날짜 형식이 올바르지 않습니다."));
        }

        // 점심시간(12:00~13:59) 예약 불가
        try {
            String[] timeParts = dto.getTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            if (hour == 12 || hour == 13) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "fail", "message", "점심시간(12:00~13:59)에는 예약이 불가합니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "fail", "message", "시간 형식이 올바르지 않습니다."));
        }

        // ✅ 중복 예약 체크 (예약중 R 상태만 확인)
        boolean alreadyReserved = reservationService.isAlreadyReserved(doctor, date, dto.getTime());
        if (alreadyReserved) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "fail", "message", "이미 예약된 시간입니다."));
        }

        // ✅ 중복 없으면 예약 진행
        reservationService.reserveWithDoctorAndTime(
                date,
                dto.getTime(),
                doctor,
                user,
                dto.getDepartment()
        );

        return ResponseEntity.ok(Map.of("status", "success"));
    }


    @GetMapping("/reservation/my/json")
    @ResponseBody
    public List<Map<String, Object>> myReservations(Authentication authentication, Locale locale) {
        String email = extractEmail(authentication);
        if (email == null) return List.of();

        User user = userRepository.findByEmail(email);

        List<Reservation> reservations;
        if (user.getRole().name().equals("A")) {
            // 관리자: 전체 예약
            reservations = reservationService.getAllReservations();
        } else {
            // 일반 사용자: 본인 예약 전체 (상태 관계없이)
            reservations = reservationService.getAllReservationsByUser(user);
        }


        List<Map<String, Object>> result = new ArrayList<>();
        for (Reservation r : reservations) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("userName", r.getUser() != null ? r.getUser().getName() : null);
            map.put("doctorName", r.getDoctor() != null ? r.getDoctor().getName() : null);
            // 언어에 따라 진료과명 분기
            String departmentName = null;
            if (r.getDoctor() != null && r.getDoctor().getDepartment() != null) {
                if ("en".equals(locale.getLanguage())) {
                    departmentName = r.getDoctor().getDepartment().getNameEn();
                } else {
                    departmentName = r.getDoctor().getDepartment().getName();
                }
            }
            map.put("department", departmentName);
            map.put("date", r.getDate().toString());
            map.put("time", r.getTime());
            map.put("state", r.getState().name()); // ✅ JS 필터링용으로 추가
            result.add(map);
        }
        return result;
    }


    @PostMapping("/reservation/cancel")
    @ResponseBody
    public Map<String, String> cancelReservation(@RequestParam Long reservationId, Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null) return Map.of("status", "fail", "message", "로그인이 필요합니다.");

        boolean success = reservationService.cancelReservation(reservationId, email);
        return success
                ? Map.of("status", "success")
                : Map.of("status", "fail", "message", "취소 실패 또는 권한 없음");
    }


    @GetMapping("/reservation/my")
    public String reservationConfirmPage(Model model, Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null) return "redirect:/login";

        User user = userRepository.findByEmail(email);
        model.addAttribute("currentUser", user);
        return "my_reservations";
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUser) {
            return customUser.getUsername(); // 또는 customUser.getUser().getEmail();
        } else if (principal instanceof CustomOAuth2User oauthUser) {
            return oauthUser.getUserDetails().getUsername(); // 또는 getUser().getEmail();
        }
        return null;
    }

    @GetMapping("/reservation/doctor-availability")
    @ResponseBody
    public Map<String, Object> getDoctorAvailability(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        System.out.println("[DEBUG] API called doctorId=" + doctorId + ", date=" + date);

        Map<String, Object> availability = reservationService.getAvailabilityByDoctorAndDate(doctorId, date);
        System.out.println("[DEBUG] API response availability=" + availability);
        boolean isDisabled = disableScheduleRepository.existsByDoctorIdAndDate(doctorId, date);

        Map<String, Object> response = new HashMap<>();
        response.put("isHoliday", availability.get("isHoliday"));
        response.put("disabled", isDisabled);
        response.put("times", isDisabled ? new HashMap<>() : availability.get("times"));

        return response;
    }


    @GetMapping("/reservation/complete")
    public String reservationCompletePage(
            @RequestParam String name,
            @RequestParam String date,      // yyyy-MM-dd
            @RequestParam String time,      // HH:mm
            @RequestParam String department,
            @RequestParam String doctorName,
            Authentication authentication,
            Locale locale,
            Model model
    ) {
        // ✅ 현재 로그인 사용자 이메일 가져오기 → 성별 확인용
        String email = extractEmail(authentication);
        String gender = null;

        if (email != null) {
            User user = userRepository.findByEmail(email);
            if (user != null) {
                gender = user.getGender(); // DB에 저장된 gender (M/F)
            }
        }

        /* ✅ 예약일 + 시간 → LocalDateTime으로 변환 */
        LocalDate localDate = LocalDate.parse(date);   // 2025-07-29
        LocalTime localTime = LocalTime.parse(time);   // 09:30
        LocalDateTime dateTime = LocalDateTime.of(localDate, localTime);

        // 한국어 요일
        String[] daysKo = {"월","화","수","목","금","토","일"};
        String formattedDateTime;

        if ("en".equals(locale.getLanguage())) {
            // ✅ 영어 → July 29, 2025 (Tue) 09:30 AM
            DateTimeFormatter engFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy (EEE) hh:mm a", Locale.ENGLISH);
            formattedDateTime = dateTime.format(engFormatter);
        } else {
            // ✅ 한국어 → 2025-07-29(화) 09:30
            DayOfWeek dayOfWeek = localDate.getDayOfWeek(); // MONDAY(1) ~ SUNDAY(7)
            String dayKo = daysKo[dayOfWeek.getValue() - 1];
            formattedDateTime = date + "(" + dayKo + ") " + time;
        }

        /* ✅ 진료과명 로케일 맞춰 변환 */
        String localizedDept = department;
        Optional<Department> deptOpt = departmentRepository.findByNameOrNameEn(department, department);
        if (deptOpt.isPresent()) {
            Department deptEntity = deptOpt.get();
            localizedDept = "en".equals(locale.getLanguage())
                    ? deptEntity.getNameEn()   // 영어 모드면 영문명
                    : deptEntity.getName();    // 한국어면 한글명
        }

        /* ✅ 의사 프로필 이미지 (없으면 기본 이미지) */
        String doctorImage = "/images/default-doctor.jpg";
        List<Doctor> doctorList = doctorRepository.findByNameContaining(doctorName);
        if (!doctorList.isEmpty()) {
            Doctor doctor = doctorList.get(0); // 동일 이름 시 첫 번째만 사용
            if (doctor.getProfileImage() != null && !doctor.getProfileImage().isEmpty()) {
                doctorImage = doctor.getProfileImage();
            }
        }

        /* ✅ 담당의 이름도 로케일 맞게 */
        String doctorDisplayName;
        if ("en".equals(locale.getLanguage())) {
            doctorDisplayName = "Dr. " + doctorName;     // 영어 → Dr. 홍길동
        } else {
            doctorDisplayName = doctorName + " 선생님";   // 한국어 → 홍길동 선생님
        }

        /* ✅ 예약자 이름도 Mr./Ms. or 홍길동님 으로 가공 */
        String displayPatientName;
        if ("en".equals(locale.getLanguage())) {
            if ("M".equals(gender)) displayPatientName = "Mr. " + name;
            else if ("F".equals(gender)) displayPatientName = "Ms. " + name;
            else displayPatientName = name;
        } else {
            displayPatientName = name + "님";
        }

        /* ✅ Model에 데이터 추가 */
        model.addAttribute("displayPatientName", displayPatientName);  // 환자 이름 완성
        model.addAttribute("formattedDateTime", formattedDateTime);    // 완성된 예약일시
        model.addAttribute("department", localizedDept);               // 로케일 맞춘 진료과명
        model.addAttribute("doctorDisplayName", doctorDisplayName);    // 담당의 최종 이름
        model.addAttribute("doctorImage", doctorImage);
        model.addAttribute("lang", locale.getLanguage());              // ko / en 구분

        return "reservationComplete";
    }

}
