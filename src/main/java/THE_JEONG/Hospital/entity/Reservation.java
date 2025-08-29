package THE_JEONG.Hospital.entity;

import THE_JEONG.Hospital.constant.ReservationState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate  date;

    private String time; // 추가해야 함

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor; // 추가해야 함

    private String department;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReservationState state = ReservationState.R; // 기본값은 예약중으로 설정 R이 예약중 C가 예약 취소
}
