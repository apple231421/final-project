package THE_JEONG.Hospital.config;

import THE_JEONG.Hospital.entity.CustomOAuth2User;
import THE_JEONG.Hospital.entity.CustomUserDetails;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalUserAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("currentUser")
    public User currentUser(@AuthenticationPrincipal Object principal) {
        System.out.println("✅ GlobalUserAdvice loaded");
        System.out.println("✅ principal: " + principal);

        String email = null;

        if (principal instanceof CustomOAuth2User oauthUser) {
            email = oauthUser.getUserDetails().getUsername(); // 또는 getUser().getEmail()
        } else if (principal instanceof CustomUserDetails customUser) {
            email = customUser.getUsername(); // 또는 getUser().getEmail()
        }

        System.out.println("✅ email: " + email);
        if (email == null) return null;

        User user = userRepository.findByEmail(email);
        System.out.println("✅ found user: " + user);

        return user;
    }
}