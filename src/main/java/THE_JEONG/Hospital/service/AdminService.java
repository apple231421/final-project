package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.constant.ReservationState;
import THE_JEONG.Hospital.entity.Diagnosis;
import THE_JEONG.Hospital.entity.DisableSchedule;
import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.repository.DiagnosisRepository;
import THE_JEONG.Hospital.repository.DisableScheduleRepository;
import THE_JEONG.Hospital.repository.DoctorRepository;
import THE_JEONG.Hospital.repository.ReservationRepository;
import THE_JEONG.Hospital.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DisableScheduleRepository disableScheduleRepository;
    private final DoctorRepository doctorRepository;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;
    private final MessageSource messageSource;
    private final DiagnosisRepository diagnosisRepository;

    // 페이징된 의사 목록 조회 (이름순 정렬, 검색 기능 포함)
    public Page<Doctor> getDoctorsWithPaging(String searchName, Pageable pageable) {
        if (searchName != null && !searchName.trim().isEmpty()) {
            return doctorRepository.findByNameContainingIgnoreCase(searchName.trim(), pageable);
        } else {
            return doctorRepository.findAll(pageable);
        }
    }

    // 모든 의사들의 진료 불가능일 조회 (페이징된 의사 목록에 대해서만)
    public Map<Long, List<LocalDate>> getDoctorDisableDatesForPage(Page<Doctor> doctorPage) {
        Map<Long, List<LocalDate>> result = new HashMap<>();
        
        for (Doctor doctor : doctorPage.getContent()) {
            List<DisableSchedule> schedules = disableScheduleRepository.findByDoctorId(doctor.getId());
            List<LocalDate> dates = schedules.stream()
                    .map(DisableSchedule::getDate)
                    .collect(Collectors.toList());
            result.put(doctor.getId(), dates);
        }
        
        return result;
    }

    // 모든 의사들의 진료 불가능일 조회 (기존 메서드 - 전체 조회용)
    public Map<Long, List<LocalDate>> getAllDoctorDisableDates() {
        List<Doctor> doctors = doctorRepository.findAll();
        Map<Long, List<LocalDate>> result = new HashMap<>();
        
        for (Doctor doctor : doctors) {
            List<DisableSchedule> schedules = disableScheduleRepository.findByDoctorId(doctor.getId());
            List<LocalDate> dates = schedules.stream()
                    .map(DisableSchedule::getDate)
                    .collect(Collectors.toList());
            result.put(doctor.getId(), dates);
        }
        
        return result;
    }

    // 특정 의사의 진료 불가능일 조회 (문자열 리스트로 반환)
    public List<String> getDoctorDisableDates(Long doctorId) {
        List<DisableSchedule> schedules = disableScheduleRepository.findByDoctorId(doctorId);
        return schedules.stream()
                .map(schedule -> schedule.getDate().toString())
                .collect(Collectors.toList());
    }

    // 의사의 진료 불가능일 추가
    @Transactional
    public void addDisableDates(Long doctorId, List<String> dateStrings) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));
        java.util.Locale locale = LocaleContextHolder.getLocale();

        for (String dateStr : dateStrings) {
            LocalDate date = LocalDate.parse(dateStr);
            
            // 이미 등록된 날짜인지 확인
            if (!disableScheduleRepository.existsByDoctorAndDate(doctor, date)) {
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
    }

    // 의사의 진료 불가능일 삭제
    @Transactional
    public void removeDisableDates(Long doctorId, List<String> dateStrings) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));

        for (String dateStr : dateStrings) {
            LocalDate date = LocalDate.parse(dateStr);
            List<DisableSchedule> schedules = disableScheduleRepository.findByDoctorId(doctorId);
            
            schedules.stream()
                    .filter(schedule -> schedule.getDate().equals(date))
                    .findFirst()
                    .ifPresent(disableScheduleRepository::delete);
        }
    }

    // 의사의 모든 진료 불가능일 삭제
    @Transactional
    public void removeAllDisableDates(Long doctorId) {
        List<DisableSchedule> schedules = disableScheduleRepository.findByDoctorId(doctorId);
        disableScheduleRepository.deleteAll(schedules);
    }
} 