package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.constant.Role;
import THE_JEONG.Hospital.dto.UserDto;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.repository.DiagnosisRepository;
import THE_JEONG.Hospital.repository.QnAAnswerRepository;
import THE_JEONG.Hospital.repository.QnARepository;
import THE_JEONG.Hospital.repository.ReservationRepository;
import THE_JEONG.Hospital.repository.UserRepository;
import THE_JEONG.Hospital.repository.VolunteerRepository;
import THE_JEONG.Hospital.repository.DoctorRepository;
import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.service.EmailService;
import THE_JEONG.Hospital.service.UserService;
import THE_JEONG.Hospital.service.DoctorService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DiagnosisRepository diagnosisRepository;
    private final ReservationRepository reservationRepository;
    private final QnARepository qnaRepository;
    private final QnAAnswerRepository qnaAnswerRepository;
    private final VolunteerRepository volunteerRepository;
    private final MessageSource messageSource;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private DoctorService doctorService;

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/"; // 이미 로그인되어 있으면 홈으로
        }
        return "login"; // templates/login.html
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "signup"; // templates/signup.html
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute UserDto userDto) {
        // UserService 호출해서 저장
        userService.register(userDto);
        return "redirect:/login";
    }

    @GetMapping("/social_signup")
    public String socialSignupForm(HttpSession session, Model model) {
        User oauthUser = (User) session.getAttribute("oauthUser"); // User로 캐스팅
        if (oauthUser == null) return "redirect:/login"; // oauthUser가 없다면 로그인 페이지로 리디렉션

        // UserDto에 세션 정보 추가
        UserDto dto = new UserDto();
        dto.setEmail(oauthUser.getEmail());
        dto.setName(oauthUser.getName());
        dto.setSocialId(oauthUser.getSocialId()); // socialId 추가
        dto.setProviderId(oauthUser.getProviderId()); // providerId 추가

        model.addAttribute("userDto", dto); // userDto를 모델에 추가하여 Thymeleaf에서 사용
        return "social_signup"; // social_signup.html 페이지로 리디렉션
    }

    @PostMapping("/social_signup")
    public String completeSocialSignup(@ModelAttribute UserDto dto, HttpSession session) {
        // 세션에서 oauthUser 객체 가져오기
        User oauthUser = (User) session.getAttribute("oauthUser");
        if (oauthUser == null) return "redirect:/login";

        // 이메일을 기준으로 기존 사용자가 존재하는지 확인
        User user = userRepository.findByEmail(dto.getEmail());

        if (user == null) {
            user = new User();
            user.setEmail(dto.getEmail());
            user.setName(dto.getName());
            user.setPhone(dto.getPhone());
            user.setAddress(dto.getAddress());
            user.setPassword("SOCIAL_LOGIN");
            user.setRole(Role.U);
            user.setSocialId(dto.getSocialId());
            user.setProviderId(dto.getProviderId());
            user.setCreatedAt(LocalDateTime.now());

            user.setGender(dto.getGender()); // 성별
            user.setBirthDate(dto.getBirthDate()); // 생년월일
            user.setProvider(oauthUser.getProvider());

            userRepository.save(user);
        } else {
            user.setName(dto.getName());
            user.setPhone(dto.getPhone());
            user.setAddress(dto.getAddress());
            user.setGender(dto.getGender());
            user.setBirthDate(dto.getBirthDate());

            userRepository.save(user);
        }

        session.removeAttribute("oauthUser");
        return "redirect:/login";
    }

    @GetMapping("/loginSuccess")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User oAuth2User, HttpSession session) {
        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank()) return "redirect:/login";

        // 이메일로 유저 찾기
        User user = userRepository.findByEmail(email);

        if (user == null) {
            // 신규 유저라면 세션에 추가 (DB 저장 전)
            user = new User();
            user.setEmail(email);
            user.setName(oAuth2User.getAttribute("name"));
            user.setRole(Role.U); // 기본 역할은 'U' (손님)
            session.setAttribute("oauthUser", user); // 세션에 저장
            return "redirect:/social_signup"; // 추가 정보 입력 페이지로 리디렉션
        }

        return "redirect:/"; // 이미 모든 정보가 있으면 대시보드나 마이페이지로 이동
    }

    @PostMapping("/find_id")
    @ResponseBody
    public String findId(@ModelAttribute UserDto dto) {
        User user = userService.findByNameAndPhone(dto.getName(), dto.getPhone());
        Locale locale = LocaleContextHolder.getLocale();
        if (user == null) {
            String msg = messageSource.getMessage("auth.error.no_match", null, locale);
            return "<span class='text-danger'>" + msg + "</span>";
        }
        // 소셜 로그인 여부 확인
        if (user.getProvider() != null && !user.getProvider().isEmpty()) {
            String msg = messageSource.getMessage("auth.error.social_id", new Object[]{user.getEmail()}, locale);
            return "<span class='text-warning'>" + msg + "</span>";
        }
        String msg = messageSource.getMessage("auth.success.id", new Object[]{user.getEmail()}, locale);
        return "<span class='text-success'>" + msg + "</span>";
    }

    @GetMapping("/check_email")
    public ResponseEntity<String> checkEmail(@RequestParam String email) {
        boolean exists = userRepository.existsByEmail(email);
        if (exists) {
            return ResponseEntity.ok("duplicate");
        } else {
            return ResponseEntity.ok("available");
        }
    }

    @PostMapping("/find_password")
    @ResponseBody
    public Map<String, String> resetPassword(@ModelAttribute UserDto dto) {
        Map<String, String> result = new HashMap<>();
        User user = userService.findByEmailAndName(dto.getEmail(), dto.getName());
        Locale locale = LocaleContextHolder.getLocale();

        if (user == null) {
            result.put("status", "fail");
            result.put("message", messageSource.getMessage("auth.error.no_account", null, locale));
            return result;
        }

        // 소셜 로그인 계정 여부 확인
        if (user.getProvider() != null && !user.getProvider().isEmpty()) {
            result.put("status", "fail");
            result.put("message", messageSource.getMessage("auth.error.social_login", null, locale));
            return result;
        }

        // 임시 비밀번호 생성 및 저장
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // 이메일 전송
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), tempPassword);
            result.put("status", "success");
            result.put("message", messageSource.getMessage("auth.success.temp_password", null, locale));
        } catch (Exception e) {
            result.put("status", "fail");
            result.put("message", messageSource.getMessage("auth.error.email_send", null, locale));
        }

        return result;
    }

    @GetMapping("/change_password")
    public String changePasswordPage(Authentication authentication, HttpSession session) {
        if (authentication == null) return "redirect:/login";
        return "change_password"; // 위 HTML 뷰
    }

    @PostMapping("/change_password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        if (authentication == null) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        Locale locale = LocaleContextHolder.getLocale();

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("error", messageSource.getMessage("auth.error.current_password", null, locale));
            return "change_password";
        }

        // 새 비밀번호 확인
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", messageSource.getMessage("auth.error.new_password", null, locale));
            return "change_password";
        }

        // 비밀번호 저장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 성공 메시지 flash로 전달
        redirectAttributes.addFlashAttribute("message", messageSource.getMessage("auth.success.password_changed", null, locale));
        return "redirect:/myPage";
    }
    @GetMapping("/edit_profile")
    public String editProfilePage(Authentication authentication, Model model, HttpSession session) {
        if (authentication == null) return "redirect:/login";

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        // 전화번호 분리
        String[] phoneParts = (user.getPhone() != null) ? user.getPhone().split("-") : new String[]{"", "", ""};

        // 주소 분리 (최대 4개 요소로 안전하게 초기화)
        String[] addressRaw = (user.getAddress() != null) ? user.getAddress().split("/") : new String[0];
        String[] addressParts = new String[4];
        for (int i = 0; i < 4; i++) {
            addressParts[i] = (i < addressRaw.length) ? addressRaw[i] : "";
        }

        model.addAttribute("user", user);
        model.addAttribute("phoneParts", phoneParts);
        model.addAttribute("addressParts", addressParts);

        return "edit_profile";
    }

    @PostMapping("/edit_profile")
    public String editProfile(@ModelAttribute UserDto userDto,
                              Authentication authentication, Model model) {

        if (authentication == null) {
            return "redirect:/login"; // 로그인 안되어있으면 로그인 페이지로 리디렉션
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        // 프로필 정보 업데이트 (전화번호, 주소)
        user.setPhone(userDto.getPhone());
        user.setAddress(userDto.getAddress());

        userRepository.save(user);

        Locale locale = LocaleContextHolder.getLocale();
        model.addAttribute("message", messageSource.getMessage("auth.success.profile_updated", null, locale));

        return "redirect:/myPage"; // 수정 완료 후 마이페이지로 리디렉션
    }
    @GetMapping("/myPage")
    public String myPage(Authentication authentication, Model model, HttpSession session) {
        if (authentication == null) return "redirect:/login";

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        // 주소 분리 (최대 4개 요소로 안전하게 초기화)
        String[] addressRaw = (user.getAddress() != null) ? user.getAddress().split("/") : new String[0];
        String[] addressParts = new String[4];

        // addressRaw에서 최대 4개의 요소로 addressParts 초기화
        for (int i = 0; i < 4; i++) {
            addressParts[i] = (i < addressRaw.length) ? addressRaw[i] : "";  // 주소 정보가 없으면 빈 값
        }

        // 소셜 계정 여부를 확인
        boolean isSocialAccount = user.getProvider() != null && !user.getProvider().isEmpty();

        // 모델에 사용자와 주소 정보를 추가
        model.addAttribute("user", user);
        model.addAttribute("addressParts", addressParts);
        model.addAttribute("isSocialAccount", isSocialAccount);
        // 소셜 계정 여부

        return "mypage";  // mypage.html로 이동
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Authentication authentication, Model model, HttpSession session) {
        if (authentication == null) return "redirect:/login";

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        
        model.addAttribute("user", user);
        return "withdraw";
    }

    @PostMapping("/withdraw")
    @Transactional
    public String withdraw(@RequestParam(required = false) String password,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        
        if (authentication == null) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        Locale locale = LocaleContextHolder.getLocale();

        // 소셜 로그인 계정인지 확인
        if (user.getProvider() != null && !user.getProvider().isEmpty()) {
            // 의사 계정인지 확인
            if (user.getRole() == Role.D) {
                Doctor doctor = doctorRepository.findByEmail(user.getEmail());
                if (doctor != null) {
                    doctorService.deleteDoctorById(doctor.getId());
                }
            } else {
                // 일반 유저 탈퇴
                deleteUserData(user);
                userRepository.delete(user);
            }
            redirectAttributes.addFlashAttribute("message", messageSource.getMessage("auth.success.withdraw", null, locale));
            return "redirect:/logout";
        }

        // 일반 계정인 경우 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", messageSource.getMessage("auth.error.password_mismatch", null, locale));
            model.addAttribute("user", user);
            return "withdraw";
        }

        // 회원 탈퇴 처리
        if (user.getRole() == Role.D) {
            Doctor doctor = doctorRepository.findByEmail(user.getEmail());
            if (doctor != null) {
                doctorService.deleteDoctorById(doctor.getId());
            }
        } else {
            deleteUserData(user);
            userRepository.delete(user);
        }
        redirectAttributes.addFlashAttribute("message", messageSource.getMessage("auth.success.withdraw", null, locale));
        
        return "redirect:/logout";
    }

    // 사용자 관련 데이터 삭제 메서드
    @Transactional
    private void deleteUserData(User user) {
        // 1. 진단서 삭제
        diagnosisRepository.deleteByUser(user);
        
        // 2. 예약 삭제
        reservationRepository.deleteByUser(user);
        
        // 3. QnA 관련 데이터 삭제 (QnAAnswer 먼저 삭제)
        qnaRepository.findAllByUserOrderByQnaIdDesc(user).forEach(qna -> {
            qnaAnswerRepository.deleteByQna(qna);
        });
        qnaRepository.deleteByUser(user);
        
        // 4. 봉사활동 신청 관계 삭제 (ManyToMany 관계)
        volunteerRepository.findAllByApplicantsContaining(user).forEach(volunteer -> {
            volunteer.getApplicants().remove(user);
            volunteerRepository.save(volunteer);
        });
    }
}