package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.constant.Role;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal UserDetails userDetails, HttpSession session) {
        if (userDetails != null) {
            String email = userDetails.getUsername(); // 기본적으로 email이 username이라 가정
            User user = userRepository.findByEmail(email);

            // 조건: 의사 + 초기 주소 + 초기 비밀번호
            if (user.getRole() == Role.D &&
                    "-/-/-/-".equals(user.getAddress()) &&
                    "$2a$10$F6prpBVm/2Wao5QGRkhCauHJmWkRmC1lcMSH9hVDOod928ci5Eria".equals(user.getPassword())) {

                model.addAttribute("showChangeInfoPopup", true);
            }
            
            // 현재 사용자 정보를 모델에 추가
            model.addAttribute("currentUser", user);
        }

        return "index";
    }
    // ✅ 추가된 챗봇 페이지 라우팅
    @GetMapping("/chat")
    public String chatPage(@AuthenticationPrincipal UserDetails userDetails, HttpSession session) {
        return "chat"; // => templates/chat.html 렌더링
    }
}
