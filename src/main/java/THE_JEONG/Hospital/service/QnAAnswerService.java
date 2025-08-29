package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.QnAAnswerDto;
import THE_JEONG.Hospital.entity.QnA;
import THE_JEONG.Hospital.entity.QnAAnswer;
import THE_JEONG.Hospital.repository.QnAAnswerRepository;
import THE_JEONG.Hospital.repository.QnARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QnAAnswerService {
    private final QnARepository qnaRepository;
    private final QnAAnswerRepository qnaAnswerRepository;

    public void save(QnAAnswerDto qnaAnswerDto, QnA qna) {
//        QnA 답변을 db에 저장
        QnAAnswer qnaAnswer = qnaAnswerDto.createQnAAnswer(qna);
        qnaAnswerRepository.save(qnaAnswer);

//        QnA 답변 완료 상태로 바꿈
        qna.setAnswered(true);
        qnaRepository.save(qna);
    }

//    QnA 답변 불러오기
    public QnAAnswer findByQnAId(Integer qnaId) {
        return qnaAnswerRepository.findById(qnaId).orElse(null);
    }
}
