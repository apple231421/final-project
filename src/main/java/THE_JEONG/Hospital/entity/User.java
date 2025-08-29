package THE_JEONG.Hospital.entity;

import THE_JEONG.Hospital.constant.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 소셜 로그인 관련
    private String provider;
    private String providerId;
    private String socialId;

    // 성별 (null 허용)
    @Column(length = 1)
    private String gender; // 'M' = 남성, 'F' = 여성

    // 생년월일 (null 허용)
    @Column(name = "birth_date")
    private LocalDate birthDate;
//    봉사활동 신청
    @ManyToMany(mappedBy = "applicants")
    private List<Volunteer> appliedVolunteers;
}