package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.dto.OgMetaDto;
import THE_JEONG.Hospital.service.OgPreviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/press")
public class OgPreviewController {

    private final OgPreviewService ogPreviewService;

    public OgPreviewController(OgPreviewService ogPreviewService) {
        this.ogPreviewService = ogPreviewService;
    }

    @GetMapping("/oginfo/{id}")
    public OgMetaDto getOgInfo(@PathVariable Long id) {
        return ogPreviewService.fetchOgInfo(id);
    }
}
