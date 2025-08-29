package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.repository.DisableScheduleRepository;
import THE_JEONG.Hospital.repository.DoctorRepository;
import THE_JEONG.Hospital.repository.UserRepository;
import THE_JEONG.Hospital.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DoctorRepository doctorRepository;
    private final DisableScheduleRepository disableScheduleRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    // 관리자 권한 확인 메서드
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_A"));
        }
        return false;
    }

    // 모든 의사들의 진료 불가능일 관리 페이지
    @GetMapping("/admin/disable-schedules")
    public String manageDisableSchedules(
            Authentication authentication, 
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchName) {
        
        if (!isAdmin(authentication)) {
            return "redirect:/login";
        }

        // 페이징 설정 (이름순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        
        // 의사 목록 조회 (페이징 + 검색)
        Page<Doctor> doctorPage = adminService.getDoctorsWithPaging(searchName, pageable);
        Map<Long, List<LocalDate>> doctorDisableDates = adminService.getDoctorDisableDatesForPage(doctorPage);
        
        // JavaScript에서 사용할 다국어 메시지들
        model.addAttribute("msgSelectDate", messageSource.getMessage("admin.disable.schedules.select.date", null, LocaleContextHolder.getLocale()));
        model.addAttribute("msgConfirmClearAll", messageSource.getMessage("admin.disable.schedules.confirm.clear.all", null, LocaleContextHolder.getLocale()));
        model.addAttribute("msgConfirmRemove", messageSource.getMessage("admin.disable.schedules.confirm.remove", null, LocaleContextHolder.getLocale()));
        model.addAttribute("msgErrorLoad", messageSource.getMessage("admin.disable.schedules.error.load", null, LocaleContextHolder.getLocale()));
        model.addAttribute("msgErrorAdd", messageSource.getMessage("admin.disable.schedules.error.add", null, LocaleContextHolder.getLocale()));
        model.addAttribute("msgErrorRemove", messageSource.getMessage("admin.disable.schedules.error.remove", null, LocaleContextHolder.getLocale()));
        model.addAttribute("msgNoDates", messageSource.getMessage("admin.disable.schedules.no.dates", null, LocaleContextHolder.getLocale()));
        
        // 페이징 정보
        model.addAttribute("doctors", doctorPage.getContent());
        model.addAttribute("doctorDisableDates", doctorDisableDates);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", doctorPage.getTotalPages());
        model.addAttribute("totalElements", doctorPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("searchName", searchName);
        
        return "admin/disable_schedules";
    }

    // 특정 의사의 진료 불가능일 조회 (AJAX)
    @GetMapping("/admin/doctor/{doctorId}/disable-dates")
    @ResponseBody
    public List<String> getDoctorDisableDates(@PathVariable Long doctorId) {
        return adminService.getDoctorDisableDates(doctorId);
    }

    // 의사의 진료 불가능일 추가
    @PostMapping("/admin/doctor/{doctorId}/disable-dates")
    @ResponseBody
    public Map<String, Object> addDisableDates(
            @PathVariable Long doctorId,
            @RequestBody List<String> dates) {
        
        try {
            adminService.addDisableDates(doctorId, dates);
            String message = messageSource.getMessage("admin.disable.schedules.success.add", null, LocaleContextHolder.getLocale());
            return Map.of("success", true, "message", message);
        } catch (Exception e) {
            String message = messageSource.getMessage("admin.disable.schedules.error.add", null, LocaleContextHolder.getLocale());
            return Map.of("success", false, "message", message + ": " + e.getMessage());
        }
    }

    // 의사의 진료 불가능일 삭제
    @DeleteMapping("/admin/doctor/{doctorId}/disable-dates")
    @ResponseBody
    public Map<String, Object> removeDisableDates(
            @PathVariable Long doctorId,
            @RequestBody List<String> dates) {
        
        try {
            adminService.removeDisableDates(doctorId, dates);
            String message = messageSource.getMessage("admin.disable.schedules.success.remove", null, LocaleContextHolder.getLocale());
            return Map.of("success", true, "message", message);
        } catch (Exception e) {
            String message = messageSource.getMessage("admin.disable.schedules.error.remove", null, LocaleContextHolder.getLocale());
            return Map.of("success", false, "message", message + ": " + e.getMessage());
        }
    }

    // 의사의 모든 진료 불가능일 삭제
    @DeleteMapping("/admin/doctor/{doctorId}/disable-dates/all")
    @ResponseBody
    public Map<String, Object> removeAllDisableDates(@PathVariable Long doctorId) {
        try {
            adminService.removeAllDisableDates(doctorId);
            String message = messageSource.getMessage("admin.disable.schedules.success.clear.all", null, LocaleContextHolder.getLocale());
            return Map.of("success", true, "message", message);
        } catch (Exception e) {
            String message = messageSource.getMessage("admin.disable.schedules.error.remove", null, LocaleContextHolder.getLocale());
            return Map.of("success", false, "message", message + ": " + e.getMessage());
        }
    }
} 