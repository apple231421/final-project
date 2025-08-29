package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.QnA;
import THE_JEONG.Hospital.entity.QnAAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnAAnswerRepository extends JpaRepository<QnAAnswer, Integer> {
    void deleteByQna(QnA qna);
}
