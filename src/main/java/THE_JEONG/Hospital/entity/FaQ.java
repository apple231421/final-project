package THE_JEONG.Hospital.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaQ {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer faqId; //faq 등록 번호

    private String title; //faq 제목

    private String faqCategory; //faq 분류

    @Column(columnDefinition = "TEXT")
    private String content; //faq 내용
}
