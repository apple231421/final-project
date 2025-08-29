package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.constant.ReservationState;
import THE_JEONG.Hospital.constant.Role;
import THE_JEONG.Hospital.dto.DoctorDto;
import THE_JEONG.Hospital.entity.*;
import THE_JEONG.Hospital.repository.*;
import THE_JEONG.Hospital.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import THE_JEONG.Hospital.service.EmailService;
import java.util.Locale;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import THE_JEONG.Hospital.service.DeepLService;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;
    private final DepartmentRepository departmentRepository;
    private final ReservationRepository reservationRepository;
    private final DisableScheduleRepository disableScheduleRepository;
    private final DoctorReservationRepository doctorReservationRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;
    private final EmailService emailService;
    private final DeepLService deepLService;

    // ✅ 전체 or 진료과별 의료진 페이지
    @GetMapping
    public String listDoctors(@RequestParam(required = false) String department, Model model) {
        List<Doctor> doctors;

        if (department != null && !department.isEmpty()) {
            Department deptEntity = departmentRepository.findByName(department);
            if (deptEntity == null) {
                deptEntity = departmentRepository.findByNameEn(department);
            }

            if (deptEntity != null) {
                doctors = doctorRepository.findByDepartment(deptEntity);  // 정확히 일치하는 진료과만 조회
                model.addAttribute("selectedDept", deptEntity);
            } else {
                doctors = List.of();  // 존재하지 않는 부서 → 빈 리스트
                model.addAttribute("selectedDept", null);
            }
        } else {
            doctors = doctorRepository.findAll();
            model.addAttribute("selectedDept", null);
        }

        model.addAttribute("doctors", doctors);
        model.addAttribute("departmentList", departmentRepository.findAll());
        return "doctors/list";
    }



    // ✅ 개별 프로필 보기
    @GetMapping("/{id:\\d+}")
    public String viewDoctor(@PathVariable Long id, Model model) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor == null) return "redirect:/doctors";
        model.addAttribute("doctor", doctor);
        return "doctors/view";
    }

    // ✅ JSON 전체
    @GetMapping("/all")
    @ResponseBody
    public List<DoctorDto> getAllDoctorsJson() {
        return doctorRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/by-department")
    @ResponseBody
    public List<DoctorDto> getDoctorsByDepartmentJson(@RequestParam String department, Authentication authentication) {
        List<Doctor> doctors;

        // ✅ 한글/영문 둘 다 검색 (or 조건)
        doctors = doctorRepository.findByDepartment_NameContainingOrDepartment_NameEnContaining(department, department);

        // 로그인 의사 제외
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            String userEmail = userDetails.getUsername();
            doctors = doctors.stream()
                    .filter(doc -> !userEmail.equals(doc.getEmail()))
                    .collect(Collectors.toList());
        }

        return doctors.stream().map(this::convertToDto).toList();
    }



    // ✅ DTO 변환
    private DoctorDto convertToDto(Doctor doctor) {
        DoctorDto dto = new DoctorDto();
        dto.setId(doctor.getId());
        dto.setName(doctor.getName());
        dto.setDepartment(doctor.getDepartment().getName());
        dto.setDepartmentEn(doctor.getDepartment().getNameEn()); // ✅ 추가
        dto.setProfileImage(doctor.getProfileImage());
        dto.setPosition(doctor.getPosition());
        dto.setDescription(doctor.getDescription());
        dto.setDescriptionEn(doctor.getDescriptionEn());
        return dto;
    }


    // ✅ 등록 폼 이동
    @GetMapping("/new")
    public String showAddDoctorForm(Authentication authentication, Model model, Locale locale) {
        if (authentication == null) return "redirect:/login";

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = customUserDetails.getUser();

        if (!user.getRole().equals(Role.A)) return "redirect:/doctors";

        model.addAttribute("departmentList", departmentRepository.findAll()); // select 박스용
        model.addAttribute("lang", locale.getLanguage());
        return "doctors/new";
    }

    @PostMapping("/add")
    public String registerDoctor(DoctorDto doctorDto, @RequestParam("profileImageFile") MultipartFile profileImageFile) {
        if(profileImageFile == null || profileImageFile.isEmpty()){
            return "redirect:/doctors/add?error=missingImage";
        }

        doctorDto.setProfileImageFile(profileImageFile);

        try {
            // 부서명(한글/영문) 모두로 조회
            Department dept = departmentRepository.findByName(doctorDto.getDepartment());
            if (dept == null) {
                dept = departmentRepository.findByNameEn(doctorDto.getDepartment());
            }
            if (dept == null) {
                // 부서가 없으면 에러 처리
                return "redirect:/doctors/add?error=invalidDepartment";
            }
            doctorDto.setDepartment(dept.getName()); // 한글명으로 통일해서 저장
            doctorService.registerDoctorWithDefaultSchedule(doctorDto);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
        return "redirect:/doctors";
    }

    // ✅ 검색
    @GetMapping("/Search")
    public String searchDoctors(@RequestParam(required = false) String name,
                                @RequestParam(required = false) String department,
                                Model model) {

        String searchName = (name != null) ? name : "";
        List<Doctor> doctors;

        if (department != null && !department.isEmpty()) {
            // ✅ 먼저 한글 부서명으로 찾기
            Department deptEntity = departmentRepository.findByName(department);

            if (deptEntity == null) {
                // ✅ 없으면 영문 부서명으로 찾기
                deptEntity = departmentRepository.findByNameEn(department);
            }

            if (deptEntity != null) {
                // ✅ 정확히 일치하는 진료과만 검색 (이름 검색 포함)
                if (!searchName.isEmpty()) {
                    // 이름 + 부서 검색
                    if (department.equals(deptEntity.getName())) {
                        doctors = doctorRepository.findByNameContainingAndDepartment_Name(searchName, deptEntity.getName());
                    } else {
                        doctors = doctorRepository.findByNameContainingAndDepartment_NameEn(searchName, deptEntity.getNameEn());
                    }
                } else {
                    // 이름 검색 없으면 그냥 부서만
                    doctors = doctorRepository.findByDepartment(deptEntity);
                }
            } else {
                doctors = List.of(); // 부서 못 찾으면 빈 결과
            }

        } else {
            // ✅ department 선택 안하면 전체 검색
            doctors = searchName.isEmpty()
                    ? doctorRepository.findAll()
                    : doctorRepository.findByNameContainingIgnoreCase(searchName, Pageable.unpaged()).getContent();
        }

        List<Department> departmentList = departmentRepository.findAll();
        model.addAttribute("doctors", doctors);
        model.addAttribute("departmentList", departmentList);

        return "doctors/Search";
    }



    @GetMapping("/reservationList")
    public String reservationList(@RequestParam(value = "searchDate", required = false) String searchDateStr,
                                  @RequestParam(value = "searchName", required = false) String searchName,
                                  @RequestParam(value = "page", defaultValue = "0") int page,  // 페이지 번호 (기본값 0)
                                  @RequestParam(value = "size", defaultValue = "10") int size,  // 페이지 사이즈 (기본값 10)
                                  Authentication authentication, Model model, Locale locale) {
        if (authentication == null) return "redirect:/login";

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = customUserDetails.getUser();

        if (!user.getRole().equals(Role.D)) return "redirect:/doctors";

        Doctor doctor = doctorRepository.findByEmail(user.getEmail());

        if (doctor == null) {
            return "redirect:/";
        }

        // searchDate가 String 형태로 들어오기 때문에 LocalDate로 변환
        LocalDate searchDate = null;
        if (searchDateStr != null && !searchDateStr.isEmpty()) {
            try {
                searchDate = LocalDate.parse(searchDateStr);  // String을 LocalDate로 변환
            } catch (DateTimeParseException e) {
                model.addAttribute("error", "잘못된 날짜 형식입니다. 올바른 형식으로 입력해주세요.");
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").ascending());  // 날짜 기준 오름차순 정렬

        Page<Reservation> reservationPage;

        if (searchDate != null) {
            // 날짜로 필터링
            reservationPage = doctorReservationRepository.findByDoctorIdAndDate(doctor.getId(), searchDate, pageable);
        } else if (searchName != null && !searchName.isEmpty()) {
            // 환자명으로 필터링
            reservationPage = doctorReservationRepository.findByDoctorIdAndUserNameContaining(doctor.getId(), searchName, pageable);
        } else {
            // 기본 예약 목록
            reservationPage = doctorReservationRepository.findByDoctorId(doctor.getId(), pageable);
        }

        if (reservationPage.isEmpty()) {
            model.addAttribute("message", "현재 등록된 진료 예약이 없습니다.");
        }

        model.addAttribute("reservations", reservationPage.getContent());
        model.addAttribute("totalPages", reservationPage.getTotalPages());
        model.addAttribute("currentPage", reservationPage.getNumber());
        model.addAttribute("totalElements", reservationPage.getTotalElements());
        model.addAttribute("searchDate", searchDateStr);
        model.addAttribute("searchName", searchName);
        model.addAttribute("size", size);
        model.addAttribute("lang", locale.getLanguage());
        return "doctors/reservation_List";
    }

    @GetMapping("/reservationDisability")
    public String showDisabilityForm(Authentication authentication, Model model, HttpServletRequest request) {
        if (authentication == null) return "redirect:/login";

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Doctor doctor = doctorRepository.findByEmail(userDetails.getUsername());

        List<DisableSchedule> schedules = disableScheduleRepository.findByDoctorId(doctor.getId());

        // 휴가 날짜만 추출 (yyyy-MM-dd 형식 문자열 리스트로)
        List<String> disableDates = schedules.stream()
                .map(s -> s.getDate().toString())
                .toList();

        // 다국어 메시지 내려주기
        var locale = localeResolver.resolveLocale(request);
        String msgSelectDate = messageSource.getMessage("doctors.reservationDisability.selectDate", null, locale);
        String msgAlreadyDisabled = messageSource.getMessage("doctors.reservationDisability.alreadyDisabled", null, locale);
        model.addAttribute("disableDates", disableDates);
        model.addAttribute("doctor", doctor);
        model.addAttribute("msgSelectDate", msgSelectDate);
        model.addAttribute("msgAlreadyDisabled", msgAlreadyDisabled);
        return "doctors/reservation_disability";
    }

    @PostMapping("/reservationDisability")
    public String handleDisability(
            @RequestParam List<String> dates,
            @RequestParam String doctorEmail,
            Model model
    ) {
        Doctor doctor = doctorRepository.findByEmail(doctorEmail);
        java.util.Locale locale = LocaleContextHolder.getLocale();

        for (String dateStr : dates) {
            LocalDate date = LocalDate.parse(dateStr);
            // 이미 등록된 휴가일이면 건너뜀
            boolean exists = disableScheduleRepository.existsByDoctorAndDate(doctor, date);
            if (!exists) {
                DisableSchedule schedule = DisableSchedule.builder()
                        .date(date)
                        .doctor(doctor)
                        .build();
                disableScheduleRepository.save(schedule);
            }

            // 해당 날짜의 예약 모두 조회 및 삭제 + 메일 발송
            List<Reservation> reservations = reservationRepository.findByDoctorIdAndDate(doctor.getId(), date);
            for (Reservation reservation : reservations) {
                // 1. 예약 상태가 D(완료)면 스킵
                if (ReservationState.D.equals(reservation.getState())) {
                    continue;
                }

                // 2. 이 예약에 연결된 진단서가 있다면 스킵
                Diagnosis diagnosis = diagnosisRepository.findByReservationId(reservation.getId());
                if (diagnosis != null) {
                    continue;
                }

                // 3. 진단서 없고, 예약 상태도 D가 아니면 삭제 진행
                String patientEmail = reservation.getUser().getEmail();
                String patientName = reservation.getUser().getName();
                String subject = messageSource.getMessage("reservation.cancel.mail.subject", null, locale);
                String message = messageSource.getMessage("reservation.cancel.mail.body", new Object[]{patientName, date}, locale);
                emailService.sendSimpleMail(patientEmail, subject, message);

                reservationRepository.delete(reservation);
            }
        }

        return "redirect:/myPage";
    }

    @GetMapping("/{id}/disable-dates")
    @ResponseBody
    public List<String> getDisableDates(@PathVariable Long id) {
        List<DisableSchedule> schedules = disableScheduleRepository.findByDoctorId(id);
        return schedules.stream()
                .map(schedule -> schedule.getDate().toString()) // yyyy-MM-dd
                .toList();
    }

    @GetMapping("/manage")
    public String manageDoctors(
            @AuthenticationPrincipal Object principal, Model model, Locale locale
    ) {
        // 1. 로그인 확인 및 타입 체크
        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            return "redirect:/login";
        }

        // 2. 유저 정보 및 권한 확인
        User user = customUserDetails.getUser();
        if (!user.getRole().equals(Role.A)) {
            return "redirect:/doctors";
        }

        // 3. 의사 리스트 조회 및 모델 전달
        List<Doctor> doctors = doctorRepository.findAll();
        model.addAttribute("doctors", doctors);
        model.addAttribute("lang", locale.getLanguage());

        return "doctors/manage";
    }

    @PostMapping("/delete/{id}")
    public String deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctorById(id);
        return "redirect:/doctors/manage";
    }

    @GetMapping("/medical_certificate/{id}")
    public String showMedicalCertificateForm(@PathVariable Long id, Model model, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        User doctor = customUserDetails.getUser();

        if (!doctor.getRole().equals(Role.D)) return "redirect:/";

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        User user = reservation.getUser();

        // 핵심: 예약 ID 기준으로 진단서 조회
        Diagnosis diagnosis = diagnosisRepository.findByReservation(reservation);

        model.addAttribute("reservation", reservation);
        model.addAttribute("user", user);
        model.addAttribute("diagnosis", diagnosis);

        return "doctors/medical_certificate";
    }

    @PostMapping("/submitMedicalCertificate")
    public String submitMedicalCertificate(@RequestParam String diagnosis,
                                           @RequestParam String treatment,
                                           @RequestParam String treatmentPeriod,
                                           @RequestParam String diagnosisPeriod,
                                           @RequestParam String doctorOpinion,
                                           @RequestParam Long reservationId,
                                           @RequestParam String state,
                                           Model model) {

        // 1. 예약 정보 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        // 2. 예약 상태 처리
        switch (state) {
            case "I" -> {
                reservation.setState(ReservationState.I);
                reservationRepository.save(reservation);
            }
            case "C" -> {
                reservation.setState(ReservationState.C);
                reservationRepository.save(reservation);
            }
            case "D" -> {
                reservation.setState(ReservationState.D);
                reservationRepository.save(reservation);
            }
        }

        // 3. 진단서 처리
        if ("I".equals(state)) {
            Diagnosis existingDiagnosis = diagnosisRepository.findByReservation(reservation);

            if (existingDiagnosis == null) {
                // 진단서 없으면 새로 작성
                Diagnosis newDiagnosis = new Diagnosis();
                newDiagnosis.setDiagnosis(diagnosis);
                newDiagnosis.setTreatment(treatment);
                newDiagnosis.setTreatmentPeriod(treatmentPeriod);
                newDiagnosis.setDiagnosisPeriod(diagnosisPeriod);
                newDiagnosis.setDoctorOpinion(doctorOpinion);
                newDiagnosis.setUser(reservation.getUser());
                newDiagnosis.setReservation(reservation);
                diagnosisRepository.save(newDiagnosis);
            } else {
                // 진단서 있으면 수정
                existingDiagnosis.setDiagnosis(diagnosis);
                existingDiagnosis.setTreatment(treatment);
                existingDiagnosis.setTreatmentPeriod(treatmentPeriod);
                existingDiagnosis.setDiagnosisPeriod(diagnosisPeriod);
                existingDiagnosis.setDoctorOpinion(doctorOpinion);
                diagnosisRepository.save(existingDiagnosis);
            }
        }

        // 4. 완료
        return "redirect:/myPage";
    }

    // 번역 요청 DTO
    public static class TranslateRequest {
        public String text;
        public String targetLang;
    }

    @PostMapping("/update")
    @ResponseBody
    public Map<String, String> updateDoctorInfo(@RequestBody Map<String, Object> req) {
        Long id = Long.valueOf(req.get("id").toString());
        String position = req.get("position").toString();
        String description = req.get("description").toString();
        String descriptionEn = req.get("descriptionEn").toString();
        doctorService.updateDoctorInfo(id, position, description, descriptionEn);
        return Map.of("status", "success");
    }



}
