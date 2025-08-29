package THE_JEONG.Hospital.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 의사 이름

    @ManyToOne
    @JoinColumn(name = "department_id") // FK 컬럼 이름
    private Department department; // ✅ 관계로 변경

    private String phone;

    private String email;

    private String position;

    private String profileImage;

    private String description;

    private String descriptionEn; // 영어 자기소개
}
