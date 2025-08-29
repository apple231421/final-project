package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.service.DeepLService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TranslateController {

    private final DeepLService deepLService;

    @PostMapping("/api/translate")
    public ResponseEntity<Map<String, String>> translate(@RequestParam String text) {
        try {
            // ✅ 입력값 검증
            if (text == null || text.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "text parameter is required"));
            }

            // ✅ DeepL API 호출
            String translated = deepLService.translateToEnglish(text);

            // ✅ 번역 실패 시 에러 처리
            if (translated == null || translated.isBlank()) {
                return ResponseEntity.status(502)
                        .body(Map.of("error", "Translation service unavailable"));
            }

            // ✅ 정상 응답
            return ResponseEntity.ok(Map.of("en", translated));

        } catch (IllegalArgumentException iae) {
            // ✅ 잘못된 요청 처리
            return ResponseEntity.badRequest()
                    .body(Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // 서버 로그 출력
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal Server Error while translating"));
        }
    }
}
