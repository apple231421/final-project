package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findAllByOrderByCreatedDateDesc(Pageable pageable);             // 관리자 전체 조회
    Page<Notice> findByDeletedFalse(Pageable pageable);                            // 일반사용자 조회
    Page<Notice> findByDeletedTrueOrderByCreatedDateDesc(Pageable pageable);     // 삭제한 게시글 전체 조회(관리자 복구용)


    Page<Notice> findBycategoryContainingIgnoreCaseOrderByCreatedDateDesc(String category, Pageable pageable);
    Page<Notice> findBycategoryContainingIgnoreCaseAndDeletedFalseOrderByCreatedDateDesc(String category, Pageable pageable);
    Page<Notice> findByTitleContainingIgnoreCaseOrderByCreatedDateDesc(String title, Pageable pageable);
    Page<Notice> findByTitleContainingIgnoreCaseAndDeletedFalseOrderByCreatedDateDesc(String title, Pageable pageable);

    // 다음글 조회 (관리자용)
    @Query("SELECT n FROM Notice n WHERE n.createdDate < (SELECT n2.createdDate FROM Notice n2 WHERE n2.id = :id) ORDER BY n.createdDate DESC, n.id DESC")
    List<Notice> findNextNotices(@Param("id") Long id);
    
    // 이전글 조회 (관리자용)
    @Query("SELECT n FROM Notice n WHERE n.createdDate > (SELECT n2.createdDate FROM Notice n2 WHERE n2.id = :id) ORDER BY n.createdDate ASC, n.id ASC")
    List<Notice> findPrevNotices(@Param("id") Long id);
    
    // 다음글 조회 (일반사용자용 - 삭제되지 않은 글만)
    @Query("SELECT n FROM Notice n WHERE n.deleted = false AND n.createdDate < (SELECT n2.createdDate FROM Notice n2 WHERE n2.id = :id) ORDER BY n.createdDate DESC, n.id DESC")
    List<Notice> findNextNoticesForUser(@Param("id") Long id);
    
    // 이전글 조회 (일반사용자용 - 삭제되지 않은 글만)
    @Query("SELECT n FROM Notice n WHERE n.deleted = false AND n.createdDate > (SELECT n2.createdDate FROM Notice n2 WHERE n2.id = :id) ORDER BY n.createdDate ASC, n.id ASC")
    List<Notice> findPrevNoticesForUser(@Param("id") Long id);

}

