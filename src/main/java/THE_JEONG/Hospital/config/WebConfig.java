package THE_JEONG.Hospital.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import THE_JEONG.Hospital.repository.UserRepository;
import THE_JEONG.Hospital.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserRepository userRepository;

    // ✅ 정적 이미지 리소스 매핑
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/profile/**")
                .addResourceLocations("file:///C:/upload/profile/");

        //volunteer 파일 경로 추가
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:///C:/upload/volunteer/");

        //notice 파일 경로 추가
        registry.addResourceHandler("/images/notice/**")
                .addResourceLocations("file:///C:/upload/notice/");

        //news 파일 경로 추가
        registry.addResourceHandler("/images/news/**")
                .addResourceLocations("file:///C:/upload/news/");        
    }
//    날짜 형식 지정
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalDateTime.class, new LocalDateTimeFormatter());
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver clr = new CookieLocaleResolver();
        clr.setDefaultLocale(Locale.KOREAN);
        clr.setCookieName("lang");
        clr.setCookieMaxAge(60 * 60 * 24 * 30); // 30일
        return clr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(new SocialSignupInterceptor(userRepository))
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/social_signup/**", "/logout", "/login", "/css/**", "/js/**", "/images/**", "/error"
                );
        registry.addInterceptor(new HttpsRedirectInterceptor())
                .addPathPatterns("/**");
    }

    // HTTPS 리다이렉트 Interceptor
    public static class HttpsRedirectInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String forwardedProto = request.getHeader("X-Forwarded-Proto");
            if (forwardedProto != null && "https".equals(forwardedProto)) {
                // HTTPS로 접근한 경우, 모든 리다이렉트를 HTTPS로 유지
                request.setAttribute("https", true);
            }
            return true;
        }
    }

    // 소셜 로그인 추가 정보 미입력 사용자 리다이렉트 Interceptor
    public static class SocialSignupInterceptor implements HandlerInterceptor {
        private final UserRepository userRepository;
        public SocialSignupInterceptor(UserRepository userRepository) {
            this.userRepository = userRepository;
        }
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String uri = request.getRequestURI();
            if (uri.startsWith("/social_signup") || uri.startsWith("/logout") || uri.startsWith("/login") || uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images")) {
                return true;
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email);
                if (user != null && user.getProvider() != null && !user.getProvider().isEmpty() &&
                    (user.getAddress() == null || user.getAddress().isBlank() ||
                     user.getGender() == null || user.getGender().isBlank() ||
                     user.getBirthDate() == null)) {
                    HttpSession session = request.getSession();
                    session.setAttribute("oauthUser", user);
                    response.sendRedirect("/social_signup");
                    return false;
                }
            }
            return true;
        }
    }
}
