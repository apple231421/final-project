package THE_JEONG.Hospital.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class ImageController {

    private static final String uploadDir = "C:/upload/notice/";

    @PreAuthorize("hasRole('A')")
    @PostMapping(value = "/image", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 디렉토리가 없으면 생성
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            // 저장할 파일명 생성
            String storedFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File dest = new File(uploadDir, storedFilename);
            file.transferTo(dest); // 실제 파일 저장

            // 프론트엔드에서 src로 바로 사용할 수 있도록 응답
            response.put("location", "/notice/" + storedFilename);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("error", "파일 업로드 실패");
        }

        return response;
    }
}
