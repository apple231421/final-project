package THE_JEONG.Hospital.entity;

import THE_JEONG.Hospital.dto.PressDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "press")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Press {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mediaName;  // 보도기관
    private String title;      // 제목
    private String link;       // 외부 기사 URL
    private LocalDate publishedDate;  // 보도일자

    @Column(nullable = false)
    private boolean deleted = false;

    public static PressDto fromEntity(Press press) {
        return new PressDto(
                press.getId(),
                press.getMediaName(),
                press.getTitle(),
                press.getLink(),
                press.getPublishedDate(),
                press.isDeleted()
        );
    }

}
