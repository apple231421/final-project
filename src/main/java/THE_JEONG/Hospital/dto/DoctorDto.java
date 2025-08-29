package THE_JEONG.Hospital.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DoctorDto {

    private Long id;
    private String name;
    private String department;
    private String departmentEn;
    private String profileImage;    // 저장된 이미지 경로
    private String description;     // 소개글
    private String descriptionEn;  // 영어 소개글
    private String phone;
    private String email;
    private String position;

    // 추가 정보
    private String gender;
    private String birthDate;       // yyyy-MM-dd
    private String address;

    // 이미지 업로드용 (폼에서 받음)
    private MultipartFile profileImageFile;
}
