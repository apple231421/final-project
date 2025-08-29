package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    User findByEmail(String email);

    User findByUserId(Integer userId);

    boolean existsByEmail(String email);

    User findByNameAndPhone(String name, String phone);

    User findByEmailAndName(@NotBlank(message = "이메일은 필수 입력값입니다.") String email, @NotBlank(message = "이름은 필수 입력값입니다.") String name);

    void deleteByEmail(String email);
}
