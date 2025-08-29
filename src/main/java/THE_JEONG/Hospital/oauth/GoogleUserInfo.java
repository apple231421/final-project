package THE_JEONG.Hospital.oauth;

import java.util.Map;

public class GoogleUserInfo extends OAuth2UserInfo {

    public GoogleUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getPhone() {
        return null; // 구글은 기본적으로 전화번호를 제공하지 않음
    }

    @Override
    public String getPCode() {
        return null; // 구글에서 제공하지 않음
    }

    @Override
    public String getLoadAddr() {
        return null; // 구글에서 제공하지 않음
    }

    @Override
    public String getLotAddr() {
        return null; // 구글에서 제공하지 않음
    }

    @Override
    public String getDetailAddr() {
        return null; // 구글에서 제공하지 않음
    }

    @Override
    public String getExtraAddr() {
        return null; // 구글에서 제공하지 않음
    }
}