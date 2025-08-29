package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.DoctorDto;
import THE_JEONG.Hospital.entity.*;
import THE_JEONG.Hospital.constant.Role;
import THE_JEONG.Hospital.repository.*;
import THE_JEONG.Hospital.constant.ReservationState;
import THE_JEONG.Hospital.service.EmailService;
import THE_JEONG.Hospital.service.DeepLService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.context.MessageSource;
import java.util.Locale;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DisableScheduleRepository disableScheduleRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private DeepLService deepLService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private final String uploadDir = "C:/upload/profile/";

    public void registerDoctor(DoctorDto dto) throws IOException {
        Department dept = departmentRepository.findByName(dto.getDepartment());

        // 1. 프로필 이미지 저장
        String profileImagePath = saveProfileImage(dto.getProfileImageFile());

        // 1-1. 한글 자기소개를 영어로 번역
        String description = dto.getDescription();
        String descriptionEn = deepLService.translateToEnglish(description);

        // 2. Doctor 저장
        Doctor doctor = Doctor.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .position(dto.getPosition())
                .description(description)
                .descriptionEn(descriptionEn)
                .profileImage(profileImagePath)
                .department(dept)
                .build();

        doctorRepository.save(doctor);

        // 3. User 저장
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address("-/-/-/-")
                .gender(dto.getGender())
                .birthDate(parseDate(dto.getBirthDate()))
                .role(Role.D)  // 의사
                .password("$2a$10$F6prpBVm/2Wao5QGRkhCauHJmWkRmC1lcMSH9hVDOod928ci5Eria") // 실제로는 암호화 필요
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }

    private String saveProfileImage(MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.createDirectories(path.getParent());
            file.transferTo(path.toFile());
            return "/images/profile/" + fileName;  // DB에 저장될 경로 (원하는 대로)
        }
        return null;
    }

    private LocalDate parseDate(String birthDateStr) {
        try {
            return LocalDate.parse(birthDateStr); // yyyy-MM-dd 형식 기대
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void updateDoctorInfo(Long id, String position, String description, String descriptionEn) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 의사입니다."));
        doctor.setPosition(position);
        doctor.setDescription(description);
        doctor.setDescriptionEn(descriptionEn);
        doctorRepository.save(doctor);
    }

    @Transactional
    public void deleteDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor != null) {
            // 0-1. 해당 의사가 작성자로 포함된 모든 뉴스 삭제
            List<News> newsList = newsRepository.findByAuthors_Id(id);
            for (News news : newsList) {
                newsRepository.delete(news);
            }
            // 0-2. 해당 의사의 모든 예약 row 삭제 전 진단서 먼저 삭제
            List<Reservation> reservations = reservationRepository.findByDoctorId(id);
            for (Reservation reservation : reservations) {
                diagnosisRepository.deleteByReservation(reservation);
            }

            // 0-3. schedule 테이블에서 해당 의사의 일정 삭제 (추가)
            scheduleRepository.deleteByDoctor(doctor);

            reservationRepository.deleteByDoctor(doctor);
            // 1. 휴가일(DisableSchedule) 삭제
            disableScheduleRepository.deleteByDoctor(doctor);
            // 2. User 삭제
            userRepository.deleteByEmail(doctor.getEmail());
            // 3. Doctor 삭제
            doctorRepository.deleteById(id);
        }
    }
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }
    public Optional<Doctor> findByContainedName(String message) {
        return doctorRepository.findAll().stream()
                .filter(doc -> message.contains(doc.getName()))
                .findFirst();
    }

    public List<Doctor> findByDepartment(Department dept) {
        return doctorRepository.findByDepartment(dept);
    }

    @Transactional
    public void registerDoctorWithDefaultSchedule(DoctorDto dto) throws IOException {
        // ✅ 1. Department 조회
        Department dept = departmentRepository.findByName(dto.getDepartment());
        if (dept == null) {
            throw new IllegalArgumentException("존재하지 않는 진료과입니다.");
        }

        // ✅ 2. 프로필 이미지 저장
        String profileImagePath = saveProfileImage(dto.getProfileImageFile());

        // ✅ 3. 한글 자기소개 → 영어 번역
        String description = dto.getDescription();
        String descriptionEn = deepLService.translateToEnglish(description);

        // ✅ 4. Doctor 생성 & 저장
        Doctor doctor = Doctor.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .position(dto.getPosition())
                .description(description)
                .descriptionEn(descriptionEn)
                .profileImage(profileImagePath)
                .department(dept)
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);

        // ✅ 5. User 엔티티 생성 (의사 계정 생성)
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address("-/-/-/-")
                .gender(dto.getGender())
                .birthDate(parseDate(dto.getBirthDate()))
                .role(Role.D)  // 의사 역할
                .password("$2a$10$F6prpBVm/2Wao5QGRkhCauHJmWkRmC1lcMSH9hVDOod928ci5Eria") // 더미 암호
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // ✅ 6. 기본 스케줄 자동 추가
        createDefaultSchedule(savedDoctor);
    }

    private void createDefaultSchedule(Doctor doctor) {
        // 평일 (월~금) → 09:00~18:30
        String[] weekdays = {"월", "화", "수", "목", "금"};
        for (String day : weekdays) {
            Schedule s = new Schedule();
            s.setDoctor(doctor);
            s.setDayOfWeek(day);
            s.setStartTime(LocalTime.of(9, 0));
            s.setEndTime(LocalTime.of(18, 30));
            scheduleRepository.save(s);
        }

        // 주말 (토/일) → 10:00~16:00
        String[] weekends = {"토", "일"};
        for (String day : weekends) {
            Schedule s = new Schedule();
            s.setDoctor(doctor);
            s.setDayOfWeek(day);
            s.setStartTime(LocalTime.of(10, 0));
            s.setEndTime(LocalTime.of(16, 30));
            scheduleRepository.save(s);
        }
    }
}