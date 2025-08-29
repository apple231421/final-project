package THE_JEONG.Hospital.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

@Service
public class DeepLService {

    private final String apiKey = "65091472-45d3-4558-b6a6-49ee8c37aaa7:fx";
    private final String apiURL = "https://api-free.deepl.com/v2/translate";

    /**
     * ✅ 범용 번역 메서드 (sourceLang 생략 가능 → 자동 감지)
     */
    public String translate(String text, String sourceLang, String targetLang) {
        System.out.println("[DeepLService] translate 호출됨: text=" + text + ", sourceLang=" + sourceLang + ", targetLang=" + targetLang);
        if (text == null || text.isBlank()) {
            System.out.println("[DeepLService] 입력값 없음");
            return ""; // 빈 문자열이면 그냥 반환
        }

        try {
            // ✅ source_lang은 optional → null이면 자동 감지
            StringBuilder params = new StringBuilder();
            params.append("auth_key=").append(apiKey);
            params.append("&text=").append(URLEncoder.encode(text, StandardCharsets.UTF_8));
            if (sourceLang != null && !sourceLang.isBlank()) {
                params.append("&source_lang=").append(sourceLang.toUpperCase());
            }
            params.append("&target_lang=").append(targetLang.toUpperCase());

            String fullUrl = apiURL + "?" + params;
            System.out.println("[DeepLService] API 요청: " + fullUrl);

            URL url = new URL(fullUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);

            int responseCode = con.getResponseCode();
            System.out.println("[DeepLService] 응답 코드: " + responseCode);

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode == 200 ? con.getInputStream() : con.getErrorStream(),
                    StandardCharsets.UTF_8));

            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            System.out.println("[DeepLService] 응답 내용: " + response);

            if (responseCode != 200) {
                System.err.println("[DeepLService] DeepL API Error: " + response);
                return null;
            }

            JSONObject json = new JSONObject(response.toString());
            String result = json.getJSONArray("translations").getJSONObject(0).getString("text");
            System.out.println("[DeepLService] 번역 결과: " + result);
            return result;

        } catch (Exception e) {
            System.err.println("[DeepLService] DeepL API 호출 중 예외 발생");
            e.printStackTrace();
            return null; // 실패 시 null 반환
        }
    }

    /**
     * ✅ 한글 → 영어 전용 (sourceLang 생략 → 자동 감지)
     */
    public String translateToEnglish(String koreanText) {
        return translate(koreanText, "KO", "EN");
    }

    /**
     * ✅ 영어 → 한글 전용
     */
    public String translateToKorean(String englishText) {
        return translate(englishText, "EN", "KO");
    }

    /**
     * ✅ 언어 자동 감지 후 영어 번역 (source_lang 생략)
     */
    public String autoDetectToEnglish(String text) {
        return translate(text, null, "EN");
    }
}
