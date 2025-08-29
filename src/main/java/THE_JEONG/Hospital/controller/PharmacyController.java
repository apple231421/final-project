package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.dto.PharmacyDto;
import THE_JEONG.Hospital.service.PharmacyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @GetMapping("/guide/pharmacy_map")
    public String pharmacyMap(Model model) throws JsonProcessingException {
        List<PharmacyDto> pharmacies = pharmacyService.loadPharmaciesFromJson();

        // ✅ 데이터 크기 확인
        System.out.println("약국 수: " + pharmacies.size());

        // ✅ 일부 약국 정보 샘플 출력 (앞 3개)
        pharmacies.stream().limit(3).forEach(p -> {
            System.out.println("약국 이름: " + p.getDutyname());
            System.out.println("전화번호: " + p.getDutytel1());
            System.out.println("위도: " + p.getWgs84lat());
            System.out.println("경도: " + p.getWgs84lon());
            System.out.println("--------------------------");
        });

        ObjectMapper mapper = new ObjectMapper();
        String pharmaciesJson = mapper.writeValueAsString(pharmacies);

        model.addAttribute("pharmaciesJson", pharmaciesJson);

        return "guide/pharmacy_map";  // HTML 파일명과 경로 (예: templates/guide/pharmacy_map.html)
    }
}
