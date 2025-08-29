package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.constant.ReservationState;
import THE_JEONG.Hospital.repository.ReservationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReservationScheduler {
    private final ReservationRepository reservationRepository;

    // 프로그램 시작 시 즉시 실행되는 메서드
    @PostConstruct
    public void startSchedulerImmediately() {
        cancelExpiredReservations();  // 애플리케이션 시작 시 바로 실행
    }

    @Scheduled(cron = "0 * * * * ?")  // 매 분 0초마다
    @Transactional
    public void cancelExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        reservationRepository.findByState(ReservationState.R).forEach(reservation -> {
            try {
                LocalDateTime reservationDateTime = LocalDateTime.of(
                        reservation.getDate(),
                        LocalTime.parse(reservation.getTime())  // "13:30" → LocalTime
                );

                if (reservationDateTime.isBefore(now)) {
                    reservation.setState(ReservationState.C);
                    reservationRepository.save(reservation);
                    System.out.println("예약 취소됨: " + reservation.getId());
                }

            } catch (Exception e) {
                System.err.println("예약 시간 파싱 실패 (예약 ID: " + reservation.getId() + "): " + e.getMessage());
            }
        });
    }
}
