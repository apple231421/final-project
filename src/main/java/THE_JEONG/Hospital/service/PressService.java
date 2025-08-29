package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.PressDto;
import THE_JEONG.Hospital.entity.Press;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import THE_JEONG.Hospital.repository.PressRepository;
import org.springframework.stereotype.Service;

@Service
public class PressService {

    private final PressRepository pressRepository;

    public PressService(PressRepository articleRepository) {
        this.pressRepository = articleRepository;
    }

    // 등록
    public void save(PressDto dto) {
        Press article = dto.toEntity();
        pressRepository.save(article);
    }

    // 전체 조회 (관리자)
    public Page<PressDto> getPagedPress(Pageable pageable) {
        return pressRepository.findAllByOrderByPublishedDateDesc(pageable)
                .map(PressDto::fromEntity);
    }

    // 삭제된 글 안보이게 조회 (모든 사용자)
    public Page<PressDto> getAllPress(Pageable pageable) {
        return pressRepository.findByDeletedFalse(pageable)
                .map(Press::fromEntity);
    }


    public void markAsDeleted(Long id) {
        Press press = pressRepository.findById(id).orElseThrow();
        press.setDeleted(true);
        pressRepository.save(press);
    }

    public void restoreDeleted(Long id) {
        Press press = pressRepository.findById(id).orElseThrow();
        press.setDeleted(false);
        pressRepository.save(press);
    }

    // 휴지통 복구 관련 메서드
    public Page<PressDto> getDeletedPress(Pageable pageable) {
        return pressRepository.findByDeletedTrueOrderByPublishedDateDesc(pageable)
                .map(PressDto::fromEntity);
    }

    // 언론보도 수정
    public PressDto findById(Long id) {
        Press press = pressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보도자료입니다."));
        return PressDto.fromEntity(press);
    }

    public Page<PressDto> searchPress(String searchType, String keyword, Pageable pageable, boolean isAdmin) {
        Page<Press> result;

        switch (searchType) {
            case "mediaName":
                result = isAdmin
                        ? pressRepository.findByMediaNameContainingIgnoreCaseOrderByPublishedDateDesc(keyword, pageable)
                        : pressRepository.findByMediaNameContainingIgnoreCaseAndDeletedFalseOrderByPublishedDateDesc(keyword, pageable);
                break;
            case "title":
                result = isAdmin
                        ? pressRepository.findByTitleContainingIgnoreCaseOrderByPublishedDateDesc(keyword, pageable)
                        : pressRepository.findByTitleContainingIgnoreCaseAndDeletedFalseOrderByPublishedDateDesc(keyword, pageable);
                break;
            default:
                result = pressRepository.findByDeletedFalse(pageable); // fallback
        }

        return result.map(PressDto::fromEntity);
    }
}
