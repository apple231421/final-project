package THE_JEONG.Hospital.dto;

import THE_JEONG.Hospital.entity.QnA;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QnADto {

    private Integer qnaId; //문의 등록 번호

    private Integer userId; //문의한 유저 고유번호

    private String title; //문의 제목
    
    private String qnaCategory; //문의 카테고리 분류

    private String content; //문의 내용

    private LocalDateTime createdAt; //문의한 날짜

    private boolean isAnswered; //답변 상태

    private List<String> filenames; //파일 이름(여러 파일)

    private List<MultipartFile> files; //실제 파일(여러 파일)

    public QnADto(QnA qnA) {
        this.qnaId = qnA.getQnaId();
        this.userId = qnA.getUser().getUserId();
        this.title = qnA.getTitle();
        this.qnaCategory = qnA.getQnaCategory();
        this.content = qnA.getContent();
        this.createdAt = qnA.getCreatedAt();
        this.isAnswered = qnA.isAnswered();
        this.filenames = qnA.getFilenames();
    }

    /** DTO → Entity 변환 (Builder 사용) */
    public QnA createQnA() {
      //  System.out.println("파일명 리스트: " + filenames); // 디버깅용
        return QnA.builder()
                .qnaId(this.qnaId)
                .title(this.title)
                .qnaCategory(this.qnaCategory)
                .content(this.content)
                .createdAt(this.createdAt != null ? this.createdAt : LocalDateTime.now())
                .isAnswered(this.isAnswered)
                .filenames(this.filenames != null ? this.filenames : new ArrayList<>())
                .build();
    }
}
