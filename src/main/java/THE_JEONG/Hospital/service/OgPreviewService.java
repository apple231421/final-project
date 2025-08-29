package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.OgMetaDto;
import THE_JEONG.Hospital.entity.Press;
import THE_JEONG.Hospital.repository.PressRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class OgPreviewService {

    private final PressRepository pressRepository;

    public OgPreviewService(PressRepository pressRepository) {
        this.pressRepository = pressRepository;
    }

    public OgMetaDto fetchOgInfo(Long pressId) {
        Press press = pressRepository.findById(pressId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 언론보도를 찾을 수 없습니다: " + pressId));

        OgMetaDto dto = new OgMetaDto();

        try {
            Document doc = Jsoup.connect(press.getLink()).timeout(5000).get();
            dto.setImage(doc.select("meta[property=og:image]").attr("content"));
            dto.setTitle(doc.select("meta[property=og:title]").attr("content"));
            dto.setDescription(doc.select("meta[property=og:description]").attr("content"));
        } catch (Exception e) {
            dto.setImage("/images/placeholder.png");
            dto.setTitle(press.getTitle());
            dto.setDescription("미리보기를 불러올 수 없습니다.");
        }

        return dto;
    }
}
