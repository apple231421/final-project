package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.entity.Department;
import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.repository.DepartmentRepository;
import THE_JEONG.Hospital.repository.DoctorRepository;
import THE_JEONG.Hospital.repository.ReservationRepository;
import THE_JEONG.Hospital.service.DepartmentService;
import THE_JEONG.Hospital.service.DeepLService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final ReservationRepository reservationRepository;
    private final DepartmentService departmentService;
    private final DeepLService deepLService;

    /** ✅ 기존 JSON: 한글만 반환 (호환 유지) */
    @GetMapping("/all")
    @ResponseBody
    public List<String> getAllDepartmentNames() {
        return departmentRepository.findAll()
                .stream()
                .map(Department::getName)
                .collect(Collectors.toList());
    }

    /** ✅ 새 JSON: 한글 + 영어 둘 다 반환 (예약페이지/다국어 지원용) */
    @GetMapping("/all-with-lang")
    @ResponseBody
    public List<Map<String, String>> getAllDepartmentsWithLang() {
        return departmentRepository.findAll()
                .stream()
                .map(d -> Map.of(
                        "name", d.getName(),      // 한글명
                        "nameEn", d.getNameEn()   // 영문명
                ))
                .toList();
    }

    /** ✅ 진료과 추가 폼 */
    @GetMapping("/add")
    public String showAddForm() {
        return "add-department";
    }

    /** ✅ 진료과 추가 처리 */
    @PostMapping("/add")
    public String addDepartment(@RequestParam String name,
                                @RequestParam(required = false) String nameEn,
                                RedirectAttributes redirectAttributes) {

        // 프론트에서 넘어온 nameEn 없으면 서버에서 DeepL 번역
        if (nameEn == null || nameEn.isBlank()) {
            nameEn = deepLService.translate(name, "KO", "EN");
        }

        Department dept = new Department();
        dept.setName(name);    // 한글명
        dept.setNameEn(nameEn); // 영문명
        departmentRepository.save(dept);

        redirectAttributes.addFlashAttribute("message", "진료과가 추가되었습니다.");
        return "redirect:/departments/list";
    }

    /** ✅ 진료과 목록 */
    @GetMapping("/list")
    public String listDepartments(Model model,
                                  @ModelAttribute("message") String message,
                                  @ModelAttribute("error") String error) {
        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("departments", departments);
        return "department-list";
    }

    /** ✅ 진료과 수정 폼 */
    @GetMapping("/edit/{id}")
    public String editDepartmentForm(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 진료과 ID입니다."));
        model.addAttribute("department", dept);
        return "edit-department";
    }

    /** ✅ 진료과 수정 처리 */
    @PostMapping("/edit/{id}")
    public String updateDepartment(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam(required = false) String nameEn,
                                   RedirectAttributes redirectAttributes) {

        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 진료과입니다."));

        dept.setName(name);

        // 프론트에서 번역값 없으면 DeepL 호출
        if (nameEn == null || nameEn.isBlank()) {
            nameEn = deepLService.translate(name, "KO", "EN");
        }
        dept.setNameEn(nameEn);

        departmentRepository.save(dept);

        redirectAttributes.addFlashAttribute("message", "진료과가 성공적으로 수정되었습니다.");
        return "redirect:/departments/list";
    }

    /** ✅ 진료과 삭제 처리 */
    @PostMapping("/delete/{id}")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 진료과입니다."));

        // 의사 존재 확인
        List<Doctor> doctors = doctorRepository.findByDepartment(department);
        if (!doctors.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "해당 진료과에 소속된 의사가 있어 삭제할 수 없습니다.");
            return "redirect:/departments/list";
        }

        // 예약 존재 확인
        boolean hasReservation = doctors.stream()
                .anyMatch(doc -> !reservationRepository.findByDoctorId(doc.getId()).isEmpty());
        if (hasReservation) {
            redirectAttributes.addFlashAttribute("error", "해당 진료과 의사의 예약이 남아 있어 삭제할 수 없습니다.");
            return "redirect:/departments/list";
        }

        departmentRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "진료과가 성공적으로 삭제되었습니다.");
        return "redirect:/departments/list";
    }

    /** ✅ 프론트 자동 번역 API */
    @PostMapping("/api/translate")
    @ResponseBody
    public Map<String, String> autoTranslate(@RequestParam String text) {
        String en = deepLService.translate(text, "KO", "EN");
        return Map.of(
                "en", en != null ? en : ""
        );
    }
}
