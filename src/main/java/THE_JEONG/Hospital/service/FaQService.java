package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.FaQDto;
import THE_JEONG.Hospital.entity.FaQ;
import THE_JEONG.Hospital.repository.FaQRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FaQService {
    private final FaQRepository faqRepository;
    
//    DTO -> 엔티티 변환 후 db에 저장
    public void save(@Valid FaQDto faqDto) {
        FaQ faq = faqDto.createFaQ();
        faqRepository.save(faq);
    }

//    모든 faq를 페이지 형식으로 받아오는 부분
    public Page<FaQ> findAllPage(Pageable pageable) {
        return faqRepository.findAll(pageable);
    }

//    카테고리 필터링하하는 부분
    public Page<FaQ> findByCategory(String category, Pageable pageable) {
        return faqRepository.findByFaqCategory(category, pageable);
    }
//    faq 한 개 받아오는 부분
    public FaQ findById(Integer id) {
        return faqRepository.findById(id).orElse(null);
    }
//    faq 한 개 삭제하는 부분
    public void deleteById(Integer id) {
        faqRepository.deleteById(id);
    }
//    faq 엔티티 -> DTO 변환하는 부분
    public FaQDto convertDto(FaQ faq) {
        return FaQDto.builder()
                .faqId(faq.getFaqId())
                .title(faq.getTitle())
                .content(faq.getContent())
                .faqCategory(faq.getFaqCategory())
                .build();
    }
//    faq 수정하는 부분
    public void updateFaQ(FaQDto faqDto, Integer id) {
        FaQ faq = faqRepository.findById(id).orElse(null);
        faq.setTitle(faqDto.getTitle());
        faq.setFaqCategory(faqDto.getFaqCategory());
        faq.setContent(faqDto.getContent());
        faqRepository.save(faq);
    }
}
