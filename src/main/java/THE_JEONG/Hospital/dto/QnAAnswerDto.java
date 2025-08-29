package THE_JEONG.Hospital.dto;

import THE_JEONG.Hospital.entity.QnA;
import THE_JEONG.Hospital.entity.QnAAnswer;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QnAAnswerDto {
    private Integer qnaId; //답변한 qna 고유 번호
    private LocalDateTime answeredAt; //답변한 시간
    private String content; //답변 내용

    public QnAAnswerDto(QnAAnswer answer){
        this.qnaId = answer.getQna().getQnaId();
        this.content = answer.getContent();
        this.answeredAt = answer.getAnsweredAt();
    }

    /** DTO → Entity 변환 (Builder 사용) */
    public QnAAnswer createQnAAnswer(QnA qna){
        return QnAAnswer.builder()
                .qna(qna)
                .content(this.content)
                .answeredAt(this.answeredAt != null ? this.answeredAt : LocalDateTime.now())
                .build();
    }
}
