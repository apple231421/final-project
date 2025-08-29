package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.QnADto;
import THE_JEONG.Hospital.entity.QnA;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.repository.QnARepository;
import THE_JEONG.Hospital.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QnAService {
    private final QnARepository qnaRepository;
    private final UserRepository userRepository;

//    DTO -> 엔티티 변환 후 db에 저장
    @Transactional
    public void save(@Valid QnADto qnaDto) {
        User user = userRepository.findById(qnaDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        QnA qna = qnaDto.createQnA();
        qna.setUser(user);
        qnaRepository.save(qna);
    }

//    내 qna를 리스트 형식으로 받아오기, 유저 아이디
    public List<QnA> findAllByUserId(Integer userId) {
        return qnaRepository.findAllByUserUserIdOrderByQnaIdDesc(userId);
    }
//    내 qna를 한 개 받아오기
    public QnA findById(Integer qnaId) {
        return qnaRepository.findById(qnaId).orElseThrow(() -> new IllegalArgumentException("유효하지 않은 QnA 아이디입니다."));
    }
//    모든 qna를 리스트 형식으로 받아오기
    public List<QnA> findAll() {
        return qnaRepository.findAllByOrderByQnaIdDesc();
    }
//    모든 qna를 카테고리 필터링하고 리스트 형식으로 받아오기
    public List<QnA> findByCategory(String category) {
        return qnaRepository.findByQnaCategoryOrderByQnaIdDesc(category);
    }
//    내 qna를 리스트 형식으로 받아오기, 유저 객체
    public List<QnA> findByUser(User user) {
        return qnaRepository.findAllByUserOrderByQnaIdDesc(user);
    }
}
