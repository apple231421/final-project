package THE_JEONG.Hospital.oauth;

import java.util.Map;

public class KakaoUserInfo extends OAuth2UserInfo {

    public KakaoUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getProviderId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getName() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount != null) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                return (String) profile.get("nickname");
            }
        }
        return null;
    }

    // 아래 항목은 일반적인 Kakao 응답에는 포함되지 않지만 구조 맞춤을 위해 남겨둠
    @Override
    public String getPhone() {
        return null;
    }

    @Override
    public String getPCode() {
        return null;
    }

    @Override
    public String getLoadAddr() {
        return null;
    }

    @Override
    public String getLotAddr() {
        return null;
    }

    @Override
    public String getDetailAddr() {
        return null;
    }

    @Override
    public String getExtraAddr() {
        return null;
    }
}