package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.entity.CustomUserDetails;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        // user가 null이면 예외 처리
        if (user == null) {
            throw new UsernameNotFoundException("해당 이메일을 가진 사용자가 없습니다: " + email);
        }

        return new CustomUserDetails(user); // ✅ 커스텀 UserDetails 반환
    }
}
