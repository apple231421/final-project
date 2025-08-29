package THE_JEONG.Hospital.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Locale;
import java.util.Map;

@Service
public class PdfService {

    private final SpringTemplateEngine templateEngine;

    public PdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateDiagnosisPdf(String templateName, Map<String, Object> data, Locale locale) {
        Context context = new Context(locale);
        context.setVariables(data);

        String html = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            // 맑은 고딕 폰트 경로 지정
            String fontPath = "C:/Windows/Fonts/malgun.ttf"; // Malgun Gothic Regular
            builder.useFont(new File(fontPath), "MalgunGothic");

            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 중 오류", e);
        }
    }

    // 기존 시그니처도 유지 (호환성)
    public byte[] generateDiagnosisPdf(String templateName, Map<String, Object> data) {
        return generateDiagnosisPdf(templateName, data, Locale.KOREAN);
    }
}