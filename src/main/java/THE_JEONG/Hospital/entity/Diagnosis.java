package THE_JEONG.Hospital.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 진단서 ID

    private String diagnosis; // 진단명
    private String treatment; // 치료 방법
    private String treatmentPeriod; // 치료 기간
    private String diagnosisPeriod; // 진단 기간
    private String doctorOpinion; // 의사의 소견

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 유저와의 연관 관계

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

}