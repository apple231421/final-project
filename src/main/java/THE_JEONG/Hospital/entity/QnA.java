package THE_JEONG.Hospital.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "qna")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer qnaId; //문의 등록 번호

    @ManyToOne(fetch = FetchType.LAZY) // 여러 QnA가 하나의 User를 참조함
    @JoinColumn(name = "user_id", nullable = false)
    private User user; //문의한 유저

    private String title; //문의 제목

    private String qnaCategory; //문의 카테고리 분류

    @Column(columnDefinition = "TEXT")
    private String content; //문의 내용

    private LocalDateTime createdAt; //문의한 날짜

    private boolean isAnswered; //답변 상태

    @ElementCollection
    @CollectionTable(name = "qna_filenames", joinColumns = @JoinColumn(name = "qna_id"))
    @Column(name = "filename")
    private List<String> filenames; //파일 이름(여러 파일)
}
