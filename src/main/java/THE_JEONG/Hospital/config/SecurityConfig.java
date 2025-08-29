package THE_JEONG.Hospital.config;

import THE_JEONG.Hospital.service.CustomUserDetailsService;
import THE_JEONG.Hospital.service.PrincipalOauth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private PrincipalOauth2UserService principalOauth2UserService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService; // 일반 로그인 사용자 서비스

    @Bean
    public SecurityFilterChain config(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 로그인 필요
                        .requestMatchers(
                                "/user/**", "/pay/**", "/userEdit", "/myPage",
                                "/qnas/create", "/qnas/edit", "/qnas/new", "guide/qnaWrite", "guide/myQnA",
                                "/reservation",              // 예약 페이지
                                "/reservation/**"           // 예약 관련 API (예약, 조회, 취소 등)


                ).authenticated()

                        // 의사(D)
                        .requestMatchers(
                                "/doctors/edit/**", "/doctors/schedule/**", "/doctors/profile/update"
                        ).hasRole("D")

                        // 관리자
                        .requestMatchers(
                                "/admin/**",
                                "/departments/add",
                                "/departments/edit/**",
                                "/departments/delete/**",
                                "/departments/list",
                                "/board/press_editForm",
                                "/board/press_writeForm",
                                "/board/press_trash",
                                "/board/notice_editForm",
                                "/board/notice_trash",
                                "/board/notice_writeForm",
                                "/board/news/write",
                                "/board/news/edit/**",
                                "/board/news/delete/**",
                                "/guide/faqWrite",
                                "/guide/faqUpdate",
                                "/guide/qnaAnswer",
                                "/guide/qnaAnswerDetail/**",
                                "/volunteer/volunteerWrite"
                        ).hasRole("A")
                        .requestMatchers("/chat", "/volunteer/volunteerDetail/**").permitAll()

                        // 나머지는 모두 허용
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/")
                        .failureHandler(loginFailureHandler())
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(principalOauth2UserService)
                        )
                        .defaultSuccessUrl("/loginSuccess", true)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                )
                .userDetailsService(customUserDetailsService); // 필수 등록

        return http.build();
    }

    // 소셜 로그인 요청 파라미터 추가용 (구글/카카오에만 필요할 경우)
    private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                OAuth2AuthorizationRequest originalRequest = defaultResolver.resolve(request);
                return customizeRequestByProvider(request, originalRequest);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                OAuth2AuthorizationRequest originalRequest = defaultResolver.resolve(request, clientRegistrationId);
                return customizeRequestByProvider(request, originalRequest);
            }

            private OAuth2AuthorizationRequest customizeRequestByProvider(HttpServletRequest request, OAuth2AuthorizationRequest originalRequest) {
                if (originalRequest != null) {
                    String requestUri = request.getRequestURI();
                    String registrationId = requestUri.substring(requestUri.lastIndexOf("/") + 1);

                    Map<String, Object> extraParams = new HashMap<>(originalRequest.getAdditionalParameters());

                    if ("kakao".equals(registrationId)) {
                        extraParams.put("prompt", "login");
                    } else if ("google".equals(registrationId)) {
                        extraParams.put("prompt", "select_account");
                    }

                    return OAuth2AuthorizationRequest.from(originalRequest)
                            .additionalParameters(extraParams)
                            .build();
                }
                return originalRequest;
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request,
                                                HttpServletResponse response,
                                                AuthenticationException exception) throws IOException {
                request.getSession().setAttribute("loginError", "아이디 또는 비밀번호가 일치하지 않습니다.");

                // 리디렉션
                response.sendRedirect("/login?error=true");
            }
        };
    }
}
