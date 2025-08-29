// OAuth2UserInfo.java
package THE_JEONG.Hospital.oauth;

import java.util.Map;

public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getProviderId();
    public abstract String getProvider();
    public abstract String getEmail();
    public abstract String getName();
    public String getPhone() { return null; }
    public String getPCode() { return null; }
    public String getLoadAddr() { return null; }
    public String getLotAddr() { return null; }
    public String getDetailAddr() { return null; }
    public String getExtraAddr() { return null; }
}