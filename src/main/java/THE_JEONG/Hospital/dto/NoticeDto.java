package THE_JEONG.Hospital.dto;

import THE_JEONG.Hospital.entity.Notice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeDto {

    private Long id;
    private String category;
    private String title;
    private String content;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private boolean deleted;
    private String filename;
    private String storedFilename;
    private String filePath;
    private String titleEn;
    private String contentEn;
    private String filenameEn;
    private String storedFilenameEn;
    private String filePathEn;

    // Entity → DTO
    public static NoticeDto fromEntity(Notice notice) {
        if (notice == null) return null;

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

    // DTO → Entity
    public Notice toEntity() {
        Notice notice = new Notice();
        notice.setId(this.id);
        notice.setCategory(this.category);
        notice.setTitle(this.title);
        notice.setContent(this.content);
        notice.setDeleted(this.deleted);
        notice.setFilename(this.filename);
        notice.setStoredFilename(this.storedFilename);
        notice.setFilePath(this.filePath);
        notice.setTitleEn(this.titleEn);
        notice.setContentEn(this.contentEn);
        notice.setFilenameEn(this.filenameEn);
        notice.setStoredFilenameEn(this.storedFilenameEn);
        notice.setFilePathEn(this.filePathEn);
        // createdDate, modifiedDate는 Auditing에 의해 자동 처리됨
        return notice;
    }
}
