package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.QnA;
import THE_JEONG.Hospital.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnARepository extends JpaRepository<QnA, Integer> {
//    내 qna를 리스트로 반환하는 부분 (user 엔티티에서 userId를 찾아오는 부분)
    List<QnA> findAllByUserUserIdOrderByQnaIdDesc(Integer userId);  // 최신순
//    모든 qna를 카테고리 상관없이 반환하는 부분
    List<QnA> findAllByOrderByQnaIdDesc();  // 최신순
//    모든 qna를 카테고리 별로 반환하는 부분
    List<QnA> findByQnaCategoryOrderByQnaIdDesc(String qnaCategory); // 카테고리 + 최신순
//    내 qna를 리스트로 반환하는 부분 (user 엔티티 자체로 찾는 부분)
    List<QnA> findAllByUserOrderByQnaIdDesc(User user);
    
    void deleteByUser(User user);
}
