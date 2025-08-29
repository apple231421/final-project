package THE_JEONG.Hospital.entity;

import THE_JEONG.Hospital.dto.NoticeDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime modifiedDate;

    @Column(nullable = false)
    private boolean deleted = false;

    private String filename;        // 원본 파일명 목록 (콤마 구분)
    private String storedFilename;  // 저장된 UUID 파일명 목록
    private String filePath;        // 서버 경로 목록

    private String titleEn;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String contentEn;

    private String filenameEn;
    private String storedFilenameEn;
    private String filePathEn;

    public static NoticeDto fromEntity(Notice notice) {
        return new NoticeDto(
                notice.getId(),
                notice.getCategory(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedDate(),
                notice.getModifiedDate(),
                notice.isDeleted(),
                notice.getFilename(),
                notice.getStoredFilename(),
                notice.getFilePath(),
                notice.getTitleEn(),
                notice.getContentEn(),
                notice.getFilenameEn(),
                notice.getStoredFilenameEn(),
                notice.getFilePathEn()
        );
    }

    public String getFilenameEn() { return filenameEn; }
    public void setFilenameEn(String filenameEn) { this.filenameEn = filenameEn; }
    public String getStoredFilenameEn() { return storedFilenameEn; }
    public void setStoredFilenameEn(String storedFilenameEn) { this.storedFilenameEn = storedFilenameEn; }
    public String getFilePathEn() { return filePathEn; }
    public void setFilePathEn(String filePathEn) { this.filePathEn = filePathEn; }
}
