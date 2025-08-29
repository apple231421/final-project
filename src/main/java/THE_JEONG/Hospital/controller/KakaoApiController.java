package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class KakaoApiController {

    private final DepartmentService departmentService;

    // 생성자 주입
    public KakaoApiController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping("/validate-department")
    public ResponseEntity<?> validateDepartment(@RequestBody Map<String, Object> body) {
        // Kakao에서 넘어온 데이터에서 진료과 이름 추출
        Map<String, Object> action = (Map<String, Object>) body.get("action");
        Map<String, Object> params = (Map<String, Object>) action.get("params");
        String department = (String) params.get("value");

        // 진료과 존재 여부 확인
        boolean isValid = departmentService.existsByName(department);

        // 응답 메시지 생성
        String responseText = isValid
                ? department + " 진료과는 예약 가능합니다."
                : department + " 진료과는 존재하지 않습니다. 다시 확인해주세요.";

        // Kakao i 응답 형식 맞춰서 반환
        Map<String, Object> simpleText = Map.of(
                "simpleText", Map.of("text", responseText)
        );

        Map<String, Object> responseBody = Map.of(
                "version", "2.0",
                "template", Map.of("outputs", java.util.List.of(simpleText))
        );

        return ResponseEntity.ok().body(responseBody);
    }

}
