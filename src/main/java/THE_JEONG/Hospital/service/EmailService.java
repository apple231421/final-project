package THE_JEONG.Hospital.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;

    public void sendPasswordResetEmail(String to, String tempPassword) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setTo(to);
            java.util.Locale locale = LocaleContextHolder.getLocale();
            String subject = messageSource.getMessage("password.reset.mail.subject", null, locale);
            helper.setSubject(subject);

            String html = messageSource.getMessage("password.reset.mail.body", new Object[]{tempPassword}, locale);

            helper.setText(html, true); // 두 번째 인자 true: HTML 사용
            javaMailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        }
    }

    public void sendSimpleMail(String to, String subject, String text) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false); // 일반 텍스트 메일
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        }
    }
}