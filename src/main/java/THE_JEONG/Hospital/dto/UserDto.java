package THE_JEONG.Hospital.dto;

import THE_JEONG.Hospital.constant.Role;
import THE_JEONG.Hospital.entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDto {

    private Integer userId;

    @NotBlank(message = "이름은 필수 입력값입니다.")
    private String name;

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    private String email;

    @NotBlank(message = "주소는 필수 입력값입니다.")
    private String address;

    @NotBlank(message = "전화번호는 필수 입력값입니다.")
    private String phone;

    private String password;

    private Role role;

    private LocalDateTime createdAt;

    // 소셜 로그인 관련
    private String provider;
    private String providerId;
    private String socialId;

    // 성별 (null 허용)
    private String gender; // 'M' = 남성, 'F' = 여성

    // 생년월일 (null 허용)
    private LocalDate birthDate;

    public UserDto(User user) {
        this.userId = user.getUserId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.address = user.getAddress();
        this.phone = user.getPhone();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
        this.provider = user.getProvider();
        this.providerId = user.getProviderId();
        this.socialId = user.getSocialId();
        this.gender = user.getGender();
        this.birthDate = user.getBirthDate(); // 생년월일 매핑
    }
}
