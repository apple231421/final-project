package THE_JEONG.Hospital.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PharmacyDto {
    private String dutyname;     // 약국 이름
    private String dutytel1;     // 전화번호
    private String wgs84lat;     // 위도
    private String wgs84lon;     // 경도
}
