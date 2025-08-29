package THE_JEONG.Hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "news")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;
    
    // 여러 의사를 작성자로 설정할 수 있도록 ManyToMany 관계 설정
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "news_authors",
        joinColumns = @JoinColumn(name = "news_id"),
        inverseJoinColumns = @JoinColumn(name = "doctor_id")
    )
    private List<Doctor> authors = new ArrayList<>();

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 파일 업로드 관련 필드들
    private String filename;        // 원본 파일명
    private String storedFilename;  // 저장된 UUID 파일명
    private String filePath;        // 서버 경로
} 