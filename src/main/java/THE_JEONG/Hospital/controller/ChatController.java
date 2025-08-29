package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.entity.Department;
import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.entity.Schedule;
import THE_JEONG.Hospital.service.DepartmentService;
import THE_JEONG.Hospital.service.DoctorService;
import THE_JEONG.Hospital.service.ReservationService;
import THE_JEONG.Hospital.service.ScheduleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
public class ChatController {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DepartmentService departmentService;
    private final DoctorService doctorService;
    private final ScheduleService scheduleService;
    private final ReservationService reservationService;
    private final MessageSource messageSource;  // ✅ 다국어 메시지 주입

    @PostMapping("/chat")
    public ResponseEntity<?> chatWithGPT(@RequestBody Map<String, Object> request) {
        String userMessage = extractUserMessage(request);
        String normalizedMsg = userMessage.replaceAll("\\s+", "");
        Locale locale = LocaleContextHolder.getLocale();

        // ✅ 예약 안내
        if (normalizedMsg.contains("예약") || userMessage.contains("reservation")) {
            String url = "/reservation";
            String reply = messageSource.getMessage("chat.reservation", new Object[]{url}, locale);
            return simpleText(reply);
        }

        // ✅ 주차 안내
        if (normalizedMsg.contains("주차") || userMessage.contains("Parking Info")) {
            String url = "/guide/parking";
            String reply = messageSource.getMessage("chat.parking", new Object[]{url}, locale);
            return simpleText(reply);
        }

        // ✅ 약국 안내
        if (normalizedMsg.contains("약국") || userMessage.contains("Nearby Pharmacies")) {
            String url = "/guide/pharmacy_map";
            String reply = messageSource.getMessage("chat.pharmacy", new Object[]{url}, locale);
            return simpleText(reply);
        }

        // ✅ 진료시간 안내
        if (normalizedMsg.contains("진료시간") || userMessage.contains("Clinic Hours")) {
            String reply = messageSource.getMessage("chat.timetable", null, locale);
            return simpleText(reply);
        }

        LocalDate today = LocalDate.now();


        // ✅ 진료표 (스케줄)
        if (normalizedMsg.contains("진료표") || normalizedMsg.toLowerCase().contains("schedule")) {
            String doctorName = findDoctorName(normalizedMsg);
            LocalDate date = parseDate(normalizedMsg, today);
            if (doctorName != null) {
                Optional<Doctor> doctorOpt = doctorService.findByContainedName(doctorName);
                if (doctorOpt.isPresent()) {
                    return buildDoctorScheduleReply(doctorOpt.get(), date, locale);
                }
            }
        }

        // ✅ “진료 가능 시간” 버튼
        if (userMessage.contains("진료 시간")) {
            String doctorName = findDoctorName(userMessage);
            if (doctorName != null) {
                Optional<Doctor> doctorOpt = doctorService.findByContainedName(doctorName);
                if (doctorOpt.isPresent()) {
                    return buildAvailableTimesReply(doctorOpt.get(), today, locale);
                }
            }
        }

        // ✅ 의사 이름 포함 → 예약 링크
        LocalDate linkDate = parseDate(normalizedMsg, today);
        Optional<Doctor> doctorOpt = doctorService.findByContainedName(userMessage);
        if (doctorOpt.isPresent()) {
            return buildDoctorReservationLinkReply(doctorOpt.get(), linkDate, locale);
        }

        // ✅ 진료과 전체 목록
        if (userMessage.contains("진료과") || userMessage.contains("department") || userMessage.toLowerCase().contains("department list")) {
            return buildDepartmentCarouselReply(locale);
        }

        // ✅ 특정 진료과 입력 → 해당 의사 출력
        for (Department dept : departmentService.findAll()) {
            String deptNameKo = dept.getName();                                // 예: 내과
            String deptNameEn = dept.getNameEn().toLowerCase();                // 예: internal medicine
            String deptNameEnNoSpace = deptNameEn.replaceAll("\\s+", "");      // 예: internalmedicine

            String message = userMessage.toLowerCase().replaceAll("\\s+", ""); // 사용자 메시지 소문자 + 공백 제거

            boolean matchKo = Pattern.compile("\\b" + Pattern.quote(deptNameKo) + "\\b")
                    .matcher(userMessage).find();

            boolean matchEn = Arrays.stream(deptNameEn.split("\\s+"))
                    .anyMatch(word -> message.contains(word))         // 단어 단위 포함
                    || message.contains(deptNameEnNoSpace);                 // 공백 없는 형태 포함

            if (matchKo || matchEn) {
                return buildDoctorCarouselReply(dept, locale);
            }
        }



        // ✅ 진료과 못 찾음
        if (userMessage.matches(".*과.*")) {
            String reply = messageSource.getMessage("chat.noDepartment", null, locale);
            return simpleText(reply);
        }

        // ✅ 전체 의사 목록
        if (userMessage.contains("의사") || userMessage.contains("doctors") || userMessage.toLowerCase().contains("doctor list")) {
            return buildAllDoctorsCarouselReply(locale);
        }

        // ✅ 그 외 GPT 응답
        String gptReply = callGPT(userMessage, locale);
        return simpleText(gptReply);
    }

    private ResponseEntity<?> simpleText(String text) {
        return ResponseEntity.ok(Map.of(
                "template", Map.of(
                        "outputs", List.of(
                                Map.of("simpleText", Map.of("text", text))
                        )
                )
        ));
    }

    private String findDoctorName(String msg) {
        for (Doctor doc : doctorService.findAll()) {
            if (msg.contains(doc.getName())) return doc.getName();
        }
        return null;
    }

    private LocalDate parseDate(String msg, LocalDate today) {


        // 5. "8월5일", "8월 5일", "08월05일", "08월 05일"
        var m5 = java.util.regex.Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일").matcher(msg);
        if (m5.find()) {
            int month = Integer.parseInt(m5.group(1));
            int day = Integer.parseInt(m5.group(2));
            try {
                return LocalDate.of(today.getYear(), month, day);
            } catch (DateTimeException e) {
                return today;
            }
        }
        // 1. "30일" 한글 형식
        var m1 = java.util.regex.Pattern.compile("(\\d{1,2})일").matcher(msg);
        if (m1.find()) {
            int day = Integer.parseInt(m1.group(1));
            try {
                return LocalDate.of(today.getYear(), today.getMonthValue(), day);
            } catch (DateTimeException e) {
                return today;
            }
        }

        // 2. "30day" 영어 형식
        var m2 = java.util.regex.Pattern.compile("(\\d{1,2})day").matcher(msg.toLowerCase());
        if (m2.find()) {
            int day = Integer.parseInt(m2.group(1));
            try {
                return LocalDate.of(today.getYear(), today.getMonthValue(), day);
            } catch (DateTimeException e) {
                return today;
            }
        }

        // 3. ISO 날짜 형식 (예: 2025-08-30)
        var m3 = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(msg);
        if (m3.find()) {
            return LocalDate.parse(m3.group(1));
        }

        // 4. "MM-DD" 또는 "MM/DD" 형식
        var m4 = java.util.regex.Pattern.compile("(\\d{1,2})[/-](\\d{1,2})").matcher(msg);
        if (m4.find()) {
            int month = Integer.parseInt(m4.group(1));
            int day = Integer.parseInt(m4.group(2));
            try {
                return LocalDate.of(today.getYear(), month, day);
            } catch (DateTimeException e) {
                return today;
            }
        }


        // ✅ 6. "August 5", "Aug 5", etc. 영어 월 + 일 형식
        var m6 = java.util.regex.Pattern.compile("(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(msg);
        if (m6.find()) {
            String monthStr = m6.group(1).toLowerCase();
            int day = Integer.parseInt(m6.group(2));

            Map<String, Integer> monthMap = Map.ofEntries(
                    Map.entry("jan", 1), Map.entry("january", 1),
                    Map.entry("feb", 2), Map.entry("february", 2),
                    Map.entry("mar", 3), Map.entry("march", 3),
                    Map.entry("apr", 4), Map.entry("april", 4),
                    Map.entry("may", 5),
                    Map.entry("jun", 6), Map.entry("june", 6),
                    Map.entry("jul", 7), Map.entry("july", 7),
                    Map.entry("aug", 8), Map.entry("august", 8),
                    Map.entry("sep", 9), Map.entry("sept", 9), Map.entry("september", 9),
                    Map.entry("oct", 10), Map.entry("october", 10),
                    Map.entry("nov", 11), Map.entry("november", 11),
                    Map.entry("dec", 12), Map.entry("december", 12)
            );

            Integer month = monthMap.get(monthStr);
            if (month != null) {
                try {
                    return LocalDate.of(today.getYear(), month, day);
                } catch (DateTimeException e) {
                    return today;
                }
            }
        }

        // 기본: 오늘
        return today;
    }


    // ✅ 진료과 다국어 이름
    private String getDepartmentNameByLocale(Department dept, Locale locale) {
        if ("en".equals(locale.getLanguage())) {
            return dept.getNameEn() != null && !dept.getNameEn().isBlank() ? dept.getNameEn() : dept.getName();
        }
        return dept.getName();
    }

    // ✅ 의사 이름 (현재 영어 컬럼 없음 → 그냥 name 사용)
    private String getDoctorNameByLocale(Doctor doctor, Locale locale) {
        return doctor.getName();
    }

    // ✅ 의사 스케줄 안내
    private ResponseEntity<?> buildDoctorScheduleReply(Doctor doctor, LocalDate date, Locale locale) {

        boolean isHoliday = reservationService.isHoliday(doctor.getId(), date);
        String dayOfWeek = reservationService.getKoreanDayOfWeek(date.getDayOfWeek());
        List<Schedule> schedules = scheduleService.getByDoctor(doctor).stream()
                .filter(s -> s.getDayOfWeek().equals(dayOfWeek)).toList();

        List<String> allSlots = new ArrayList<>();
        for (Schedule s : schedules) {
            for (LocalTime t = s.getStartTime(); t.isBefore(s.getEndTime()); t = t.plusMinutes(30)) {
                allSlots.add(t.toString().substring(0, 5));
            }
        }

        List<String> available = reservationService.getAvailableTimes(doctor.getId(), date);

        String header = messageSource.getMessage("chat.schedule.header", null, locale);
        String availableMsg = messageSource.getMessage("chat.schedule.available", new Object[]{doctor.getName(), date}, locale);
        String legend = messageSource.getMessage("chat.schedule.legend", null, locale);
        String noSlot = messageSource.getMessage("chat.schedule.no", null, locale);

        StringBuilder reply = new StringBuilder(header).append("\n\n")
                .append(availableMsg).append("\n")
                .append(legend).append("\n────────────\n");

        if (isHoliday) {
            // ✅ 휴가일
            String holidayMsg = messageSource.getMessage("chat.schedule.holiday", new Object[]{doctor.getName()}, locale);
            reply.append("🚫 ").append(holidayMsg).append("\n");
            return simpleText(reply.toString());
        }

        if (allSlots.isEmpty() || available.isEmpty()) {
            // ✅ 진료시간 없음 or 예약 가능 시간 없음
            reply.append("🚫 ").append(noSlot).append("\n");
            return simpleText(reply.toString());  // 예약 버튼 없이 조기 반환
        }

        // ✅ 시간별 슬롯 출력
        int col = 4;
        for (int i = 0; i < allSlots.size(); i++) {
            String slot = allSlots.get(i);
            int hour = Integer.parseInt(slot.substring(0, 2));
            boolean isLunch = (hour == 12 || hour == 13);
            reply.append(isLunch ? "🔵 " : available.contains(slot) ? "🟢 " : "🔴 ");
            reply.append(slot).append(" ");
            if ((i + 1) % col == 0) reply.append("\n");
        }

        // ✅ 예약 링크 정보
        String deptEncoded = URLEncoder.encode(doctor.getDepartment().getName(), StandardCharsets.UTF_8);
        String doctorNameEncoded = URLEncoder.encode(doctor.getName(), StandardCharsets.UTF_8);
        String doctorImageEncoded = URLEncoder.encode(
                doctor.getProfileImage() != null ? doctor.getProfileImage() : "/images/default-doctor.jpg",
                StandardCharsets.UTF_8
        );

        String url = String.format(
                "/reservation?dept=%s&doctorId=%d&doctorName=%s&doctorImage=%s&date=%s",
                deptEncoded,
                doctor.getId(),
                doctorNameEncoded,
                doctorImageEncoded,
                date != null ? date.toString() : ""
        );

        String reserveText = messageSource.getMessage("chat.schedule.reserve", new Object[]{doctor.getName()}, locale);

        String currentUserEmail = null;
        try {
            org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                    currentUserEmail = userDetails.getUsername();
                } else if (principal instanceof String str) {
                    currentUserEmail = str;
                }
            }
        } catch (Exception e) {
            // ignore
        }

        boolean showReserveButton = currentUserEmail == null || !currentUserEmail.equals(doctor.getEmail());

        reply.append("\n────────────\n");
        if (showReserveButton) {
            reply.append("👉 <a href='").append(url).append("'>").append(reserveText).append("</a>");
        } else {
            String msg = messageSource.getMessage("chat.reservation.self_block", null, locale);
            reply.append(msg);
        }

        return simpleText(reply.toString());
    }


    private ResponseEntity<?> buildAvailableTimesReply(Doctor doctor, LocalDate date, Locale locale) {
        List<String> available = reservationService.getAvailableTimes(doctor.getId(), date);
        String todayMsg = messageSource.getMessage("chat.available.today", new Object[]{doctor.getName(), date}, locale);
        String noSlot = messageSource.getMessage("chat.available.none", null, locale);

        StringBuilder reply = new StringBuilder(todayMsg).append("\n");
        if (available.isEmpty()) reply.append(noSlot);
        else available.forEach(t -> reply.append(t).append(" "));
        return simpleText(reply.toString());
    }

    private ResponseEntity<?> buildDoctorReservationLinkReply(Doctor doctor, LocalDate date, Locale locale) {
        String currentUserEmail = getCurrentUserEmail();

        // 🔒 본인이면 예약 링크 금지
        if (currentUserEmail != null && currentUserEmail.equals(doctor.getEmail())) {
            String blockMsg = messageSource.getMessage("chat.reservation.self_block", null, locale);
            return simpleText(blockMsg);
        }

        // 예약 링크 생성
        String deptEncoded = URLEncoder.encode(doctor.getDepartment().getName(), StandardCharsets.UTF_8);
        String doctorNameEncoded = URLEncoder.encode(doctor.getName(), StandardCharsets.UTF_8);
        String doctorImageEncoded = URLEncoder.encode(
                doctor.getProfileImage() != null ? doctor.getProfileImage() : "/images/default-doctor.jpg",
                StandardCharsets.UTF_8
        );

        String url = "/reservation?dept=" + deptEncoded +
                "&doctorId=" + doctor.getId() +
                "&doctorName=" + doctorNameEncoded +
                "&doctorImage=" + doctorImageEncoded;

        String reply = messageSource.getMessage(
                "chat.doctor.link",
                new Object[]{doctor.getName(), url},
                locale
        );
        return simpleText(reply);
    }


    // ✅ 진료과 목록 Carousel (locale별) 주소값
    private ResponseEntity<?> buildDepartmentCarouselReply(Locale locale) {
        String baseUrl = "http://localhost/images/chatbot.png";
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Department dept : departmentService.findAll()) {
            String deptName = getDepartmentNameByLocale(dept, locale);
            String title = messageSource.getMessage("chat.carousel.department", new Object[]{deptName}, locale);
            String btnLabel = messageSource.getMessage("chat.department.view", new Object[]{deptName}, locale);

            cards.add(Map.of(
                    "title", title,
                    "thumbnail", Map.of("imageUrl", baseUrl),
                    "buttons", List.of(Map.of(
                            "action", "message",
                            "label", btnLabel,
                            "messageText", deptName
                    ))
            ));
        }
        return carousel(cards);
    }

    private ResponseEntity<?> buildDoctorCarouselReply(Department dept, Locale locale) {
        List<Doctor> doctors = doctorService.findByDepartment(dept);
        String currentUserEmail = getCurrentUserEmail();

        String deptName = getDepartmentNameByLocale(dept, locale);

        // 로그인한 유저 본인은 제외
        if (currentUserEmail != null) {
            doctors = doctors.stream()
                    .filter(doc -> !currentUserEmail.equals(doc.getEmail()))
                    .toList();
        }

        if (doctors.isEmpty()) {
            String reply = messageSource.getMessage("chat.noDoctor", new Object[]{deptName}, locale);
            return simpleText(reply);
        }

        List<Map<String, Object>> cards = new ArrayList<>();
        for (Doctor doctor : doctors) {
            String img = doctor.getProfileImage() != null ? doctor.getProfileImage() : "https://via.placeholder.com/80";
            String doctorName = doctor.getName();
            String title = messageSource.getMessage("chat.carousel.doctor", new Object[]{doctorName}, locale);
            String btnSchedule = messageSource.getMessage("chat.doctor.schedule", null, locale);
            String btnReserve = messageSource.getMessage("chat.doctor.reserve", null, locale);

            cards.add(Map.of(
                    "title", title,
                    "dept", deptName,
                    "thumbnail", Map.of("imageUrl", img),
                    "buttons", List.of(
                            Map.of("action", "message", "label", btnSchedule, "messageText", doctorName + " 진료표"),
                            Map.of("action", "message", "label", btnReserve, "messageText", doctorName)
                    )
            ));
        }

        return carousel(cards);
    }



    private ResponseEntity<?> buildAllDoctorsCarouselReply(Locale locale) {
        String currentUserEmail = getCurrentUserEmail();

        List<Map<String, Object>> cards = new ArrayList<>();
        String labelSchedule = messageSource.getMessage("chatbot.button.schedule", null, locale);
        String labelReservation = messageSource.getMessage("chatbot.button.reserve", null, locale);
        String labelReservation1 = messageSource.getMessage("chatbot.button.reserve1", null, locale);

        for (Doctor doctor : doctorService.findAll()) {
            // 로그인한 유저 본인은 건너뜀
            if (currentUserEmail != null && currentUserEmail.equals(doctor.getEmail())) continue;

            String img = doctor.getProfileImage() != null ? doctor.getProfileImage() : "https://via.placeholder.com/80";
            String deptName = getDepartmentNameByLocale(doctor.getDepartment(), locale);

            cards.add(Map.of(
                    "title", doctor.getName(),
                    "dept", deptName,
                    "thumbnail", Map.of("imageUrl", img),
                    "buttons", List.of(
                            Map.of("action", "message", "label", "⏰ " + labelSchedule, "messageText", doctor.getName() + " " + labelSchedule),
                            Map.of("action", "message", "label", "📅 " + labelReservation, "messageText", doctor.getName() + " " + labelReservation1)
                    )
            ));
        }

        return carousel(cards);
    }



    private ResponseEntity<?> carousel(List<Map<String, Object>> items) {
        return ResponseEntity.ok(Map.of(
                "version", "2.0",
                "template", Map.of("outputs", List.of(
                        Map.of("carousel", Map.of("type", "basicCard", "items", items))
                ))
        ));
    }

    private String extractUserMessage(Map<String, Object> request) {
        try {
            Map<String, Object> action = (Map<String, Object>) request.get("action");
            Map<String, Object> params = (Map<String, Object>) action.get("params");
            return (String) params.get("value");
        } catch (Exception e) {
            return "";
        }
    }
    private String callGPT(String message, Locale locale) {
        List<Department> departments = departmentService.findAll();
        List<Doctor> doctors = doctorService.findAll();
        Map<Long, List<Schedule>> doctorSchedules = scheduleService.getAllGroupedByDoctor();

        // ✅ 입력 언어 감지 (한글 포함 여부)
        boolean isKorean = message.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*");

        // ✅ 병원 정보는 참고, 일반 질문도 자유롭게
        StringBuilder systemPrompt = new StringBuilder();
        if (isKorean) {
            systemPrompt.append("너는 병원 AI 상담사야. ")
                    .append("병원 관련 질문은 아래 정보를 참고하고, 병원과 무관한 질문은 일반 AI처럼 답해도 돼. ")
                    .append("**하지만 반드시 한국어로만 답변해.**\n\n");
        } else {
            systemPrompt.append("You are a hospital AI assistant. ")
                    .append("If the user asks about the hospital, use the info below, otherwise you can answer freely. ")
                    .append("**But always answer in English.**\n\n");
        }

        // ✅ 병원 데이터 추가
        for (Department dept : departments) {
            systemPrompt.append("진료과: ").append(getDepartmentNameByLocale(dept, locale)).append("\n");
            for (Doctor doc : doctors.stream().filter(d -> d.getDepartment().getId().equals(dept.getId())).toList()) {
                systemPrompt.append("- ").append(doc.getName()).append(" (")
                        .append(getDepartmentNameByLocale(dept, locale)).append(")\n");
                List<Schedule> schedules = doctorSchedules.get(doc.getId());
                if (schedules != null) {
                    for (Schedule sched : schedules) {
                        systemPrompt.append("  > ").append(sched.getDayOfWeek()).append(": ")
                                .append(sched.getStartTime()).append("~").append(sched.getEndTime()).append("\n");
                    }
                }
            }
        }

        // ✅ GPT 호출
        WebClient client = WebClient.builder()
                .baseUrl("https://openrouter.ai/api/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + openaiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        String requestBody = """
            {
              "model": "mistralai/mistral-7b-instruct",
              "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user", "content": "%s"}
              ]
            }
            """.formatted(systemPrompt.toString(), message);

        try {
            String response = client.post().bodyValue(requestBody).retrieve().bodyToMono(String.class).block();
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("choices").get(0).path("message").path("content").asText();
        } catch (WebClientResponseException e) {
            String errMsg = messageSource.getMessage("chat.gpt.error", new Object[]{e.getStatusCode()}, locale);
            return errMsg + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return messageSource.getMessage("chat.error", null, locale);
        }
    }
    private String getCurrentUserEmail() {
        try {
            org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                    return userDetails.getUsername();
                } else if (principal instanceof String str) {
                    return str;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }


}
