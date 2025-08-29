package THE_JEONG.Hospital.dto;

import THE_JEONG.Hospital.entity.FaQ;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class FaQDto {
    private Integer faqId; //faq 등록 번호

    private String title; //faq 제목

    private String faqCategory; //faq 분류

    private String content; //faq 내용

    public FaQDto(FaQ faq){
        this.faqId = faq.getFaqId();
        this.title = faq.getTitle();
        this.faqCategory = faq.getFaqCategory();
        this.content = faq.getContent();
    }

    /** DTO → Entity 변환 (Builder 사용) */
    public FaQ createFaQ(){
        return FaQ.builder()
                .faqId(this.faqId)
                .title(this.title)
                .faqCategory(this.faqCategory)
                .content(this.content)
                .build();
    }
}
