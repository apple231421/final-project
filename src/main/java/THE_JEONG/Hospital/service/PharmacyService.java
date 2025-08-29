package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.PharmacyDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PharmacyService {

    public List<PharmacyDto> loadPharmaciesFromJson() {
        List<PharmacyDto> pharmacies = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = getClass().getResourceAsStream("/pharmacy.json")) {
            JsonNode root = mapper.readTree(is);
            JsonNode dataArray = root.get("DATA");

            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode node : dataArray) {
                    PharmacyDto dto = mapper.treeToValue(node, PharmacyDto.class);
                    pharmacies.add(dto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();  // 혹은 로그 처리
        }

        return pharmacies;
    }
}
