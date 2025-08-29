package THE_JEONG.Hospital.entity;

import THE_JEONG.Hospital.constant.VolunteerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "volunteer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Volunteer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long volunteerId ; //봉사활동 프로그램 고유 아이디

    private String title; //봉사활동 제목

    @Column(columnDefinition = "TEXT")
    private String content; //프로그램 내용

    private LocalDateTime applyStartTime; //프로그램 신청 시작시간
    private LocalDateTime applyEndTime; //프로그램 신청 마감시간

    @Enumerated(EnumType.STRING)
    private VolunteerStatus status; //프로그램 진행 상태 (모집중, 모집완료, 진행중, 완료)

    private int applyCount; //신청 인원
    private Integer maxApplyCount = 1; //신청 최대 인원
    private String filename; //파일 이름

    @ManyToMany
    @JoinTable(
            name = "volunteer_applicants",
            joinColumns = @JoinColumn(name = "volunteer_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> applicants; //신청한 유저들
}
