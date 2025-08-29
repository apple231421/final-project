package THE_JEONG.Hospital.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "qnaAnswer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnAAnswer {
    @Id
    private Integer qnaId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qnaId")
    @MapsId
    private QnA qna; //답변한 qnaId

    private LocalDateTime answeredAt; //답변한 시간

    @Column(columnDefinition = "TEXT")
    private String content; //답변 내용
}
