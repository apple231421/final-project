package THE_JEONG.Hospital.dto;

import THE_JEONG.Hospital.entity.Press;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PressDto {

    private Long id;
    private String mediaName;       // 보도기관
    private String title;           // 기사 제목
    private String link;            // 기사 링크 (URL)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishedDate; // 보도일자
    private boolean deleted;



    public Press toEntity() {
        Press entity = new Press();
        entity.setId(this.id);
        entity.setMediaName(this.mediaName);
        entity.setTitle(this.title);
        entity.setLink(this.link);
        entity.setPublishedDate(this.publishedDate);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static PressDto fromEntity(Press press) {
        if (press == null) return null;

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
