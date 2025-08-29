package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.constant.ReservationState;
import THE_JEONG.Hospital.constant.Role;
import THE_JEONG.Hospital.entity.*;
import THE_JEONG.Hospital.repository.DiagnosisRepository;
import THE_JEONG.Hospital.repository.ReservationRepository;
import THE_JEONG.Hospital.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping("/diagnosis")
public class DiagnosisController {

    private final ReservationRepository reservationRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PdfService pdfService;

    @GetMapping("/view/{reservationId}")
    public String viewDiagnosis(Authentication authentication,
                                @PathVariable Long reservationId,
                                Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Object principal = authentication.getPrincipal();
        User loginUser = null;

        if (principal instanceof CustomUserDetails userDetails) {
            loginUser = userDetails.getUser();
        } else if (principal instanceof CustomOAuth2User oauthUser) {
            loginUser = oauthUser.getUserDetails().getUser();
        }

        if (loginUser == null) {
            return "redirect:/login";
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        if (reservation.getState() != ReservationState.D) {
            return "redirect:/diagnosis/list";
        }

        User reservationUser = reservation.getUser();
        boolean isOwner = loginUser.getUserId().equals(reservationUser.getUserId());
        boolean isAdmin = loginUser.getRole() == Role.A;

        if (!isOwner && !isAdmin) {
            return "redirect:/diagnosis/list";
        }

        Diagnosis diagnosis = diagnosisRepository.findByReservation(reservation);

        model.addAttribute("reservation", reservation);
        model.addAttribute("diagnosis", diagnosis);
        model.addAttribute("user", reservationUser);

        return "diagnosis/diagnosis_view";
    }

    @GetMapping("/list")
    public String userDiagnosisList(@AuthenticationPrincipal Object principal,
                                    Model model,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String keyword) {

        User user = null;

        if (principal instanceof CustomUserDetails customUserDetails) {
            user = customUserDetails.getUser();
        } else if (principal instanceof CustomOAuth2User oauthUser) {
            user = oauthUser.getUserDetails().getUser();
        }

        if (user == null) {
            return "redirect:/login";
        }

        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Diagnosis> diagnosisPage;

        if (user.getRole() == Role.A) {
            diagnosisPage = diagnosisRepository.searchByPatientNameAndReservationStateD(keyword, pageable);
        } else {
            diagnosisPage = diagnosisRepository.findByUserAndReservationStateD(user, pageable);
        }

        model.addAttribute("diagnosisPage", diagnosisPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", diagnosisPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "diagnosis/diagnosis_list";
    }

    @GetMapping("/pdf/{reservationId}")
    public ResponseEntity<byte[]> downloadDiagnosisPdf(@PathVariable Long reservationId,
                                                      Authentication authentication,
                                                      @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
                                                      @RequestParam(value = "lang", required = false) String lang) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();
        User user = null;

        if (principal instanceof CustomUserDetails customUserDetails) {
            user = customUserDetails.getUser();
        } else if (principal instanceof CustomOAuth2User oauthUser) {
            user = oauthUser.getUserDetails().getUser();
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));
        Diagnosis diagnosis = diagnosisRepository.findByReservation(reservation);

        if (diagnosis == null || reservation.getState() != ReservationState.D) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean isOwner = reservation.getUser().getUserId().equals(user.getUserId());
        boolean isAdmin = user.getRole() == Role.A;

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> data = Map.of(
                "reservation", reservation,
                "diagnosis", diagnosis,
                "user", reservation.getUser()
        );

        Locale locale = Locale.KOREAN;
        if (lang != null) {
            if (lang.equalsIgnoreCase("en")) {
                locale = Locale.ENGLISH;
            } else if (lang.equalsIgnoreCase("ko")) {
                locale = Locale.KOREAN;
            }
        } else if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            String firstLang = acceptLanguage.split(",")[0].toLowerCase();
            if (firstLang.startsWith("en")) {
                locale = Locale.ENGLISH;
            } else if (firstLang.startsWith("ko")) {
                locale = Locale.KOREAN;
            } else {
                locale = Locale.forLanguageTag(firstLang);
            }
        }

        byte[] pdfBytes = pdfService.generateDiagnosisPdf("diagnosis/diagnosis_pdf", data, locale);

        String patientName = reservation.getUser().getName().replaceAll("\\s+", "");
        String fileName = URLEncoder.encode(patientName + "_진단서.pdf", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
