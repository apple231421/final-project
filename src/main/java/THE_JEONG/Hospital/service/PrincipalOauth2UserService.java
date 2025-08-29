package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.constant.Role;
import THE_JEONG.Hospital.entity.CustomOAuth2User;
import THE_JEONG.Hospital.entity.CustomUserDetails;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.oauth.GoogleUserInfo;
import THE_JEONG.Hospital.oauth.KakaoUserInfo;
import THE_JEONG.Hospital.oauth.OAuth2UserInfo;
import THE_JEONG.Hospital.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Autowired
    private HttpServletRequest request; // 세션 접근용

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        OAuth2UserInfo oAuth2UserInfo;

        String provider = userRequest.getClientRegistration().getRegistrationId();

        if ("google".equals(provider)) {
            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if ("kakao".equals(provider)) {
            oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"));
        }

        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        if (email == null || email.isEmpty()) {
            email = provider + "_" + providerId + "@noemail.com"; // 기본값 설정
        }
        String socialId = provider + "_" + providerId;

        // DB에서 해당 이메일로 사용자 조회
        User user = userRepository.findByEmail(email);

        if (user == null) {
            // 새 사용자일 경우: DB에 바로 저장
            user = new User();
            user.setEmail(email);
            user.setName(oAuth2UserInfo.getName());
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setSocialId(socialId);
            user.setRole(Role.U); // 기본 권한 'U' (손님)
            user.setPassword("SOCIAL_LOGIN");
            user.setCreatedAt(LocalDateTime.now());  // 생성일자 추가

            // DB에 새 사용자 저장
            userRepository.save(user);
        } else {
            // 기존 사용자일 경우: 필요한 정보만 업데이트
            user.setProviderId(providerId);
            user.setSocialId(socialId);
            userRepository.save(user); // DB에 정보 업데이트
        }

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", email); // 이메일을 추가
        attributes.put("socialId", socialId); // 사용자 정의 ID 추가

        CustomUserDetails userDetails = new CustomUserDetails(user);

        // 최종적으로 사용자 정보를 DefaultOAuth2User에 담아서 리턴
        /*
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email" // 이메일을 기준으로 로그인 처리
        );
        */
        return new CustomOAuth2User(userDetails, attributes);
    }
}
