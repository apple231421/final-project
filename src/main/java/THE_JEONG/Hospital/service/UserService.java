package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.constant.Role;
import THE_JEONG.Hospital.dto.UserDto;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(UserDto dto) {
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.U)
                .createdAt(LocalDateTime.now())
                .gender(dto.getGender())
                .birthDate(dto.getBirthDate())
                .build();

        userRepository.save(user);
    }

    public User findByNameAndPhone(String name, String phone) {
        return userRepository.findByNameAndPhone(name, phone);
    }

    public User findByEmailAndName(@NotBlank(message = "이메일은 필수 입력값입니다.") String email, @NotBlank(message = "이름은 필수 입력값입니다.") String name) {
        return userRepository.findByEmailAndName(email,name);
    }
}