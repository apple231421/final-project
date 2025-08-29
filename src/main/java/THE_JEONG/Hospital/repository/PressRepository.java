package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.Press;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PressRepository extends JpaRepository<Press, Long> {
    Page<Press> findAllByOrderByPublishedDateDesc(Pageable pageable);             // 관리자 전체 조회
    Page<Press> findByDeletedFalse(Pageable pageable);                            // 일반사용자 조회
    Page<Press> findByDeletedTrueOrderByPublishedDateDesc(Pageable pageable);     // 삭제한 게시글 전체 조회(관리자 복구용)


    Page<Press> findByMediaNameContainingIgnoreCaseOrderByPublishedDateDesc(String mediaName, Pageable pageable);
    Page<Press> findByMediaNameContainingIgnoreCaseAndDeletedFalseOrderByPublishedDateDesc(String mediaName, Pageable pageable);
    Page<Press> findByTitleContainingIgnoreCaseOrderByPublishedDateDesc(String title, Pageable pageable);
    Page<Press> findByTitleContainingIgnoreCaseAndDeletedFalseOrderByPublishedDateDesc(String title, Pageable pageable);

}

