package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.repository.DepartmentRepository;
import THE_JEONG.Hospital.repository.DoctorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@AllArgsConstructor
public class IntroduceController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping(value = "/guide/visit")
    public String map() {
        return "guide/visit";
    }

    @GetMapping(value = "/guide/parking")
    public String parking() { return "guide/parking"; }

    @GetMapping("/api/doctors")
    @ResponseBody
    public List<Doctor> getDoctorsByDepartment(@RequestParam String department) {
        return doctorRepository.findByDepartment_Name(department);
    }


    @GetMapping(value = "/guide/room")
    public String hospitalMap(Model model) {
        List<Doctor> doctors = doctorRepository.findAll();
        model.addAttribute("doctors", doctors);
        model.addAttribute("departmentList", departmentRepository.findAll()); // 선택 필터용
        return "guide/room";
    }

    @GetMapping(value = "/about/greeting")
    public String doctorKingIntro() { return "/about/greeting"; }

    @GetMapping(value = "/about/org")
    public String organization() { return "/about/org"; }

    @GetMapping(value = "/about/history")
    public String history() { return "/about/history"; }

}
