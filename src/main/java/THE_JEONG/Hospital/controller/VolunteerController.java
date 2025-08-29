package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.dto.VolunteerDto;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.entity.Volunteer;
import THE_JEONG.Hospital.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class VolunteerController {
    private final VolunteerService volunteerService;

    //    봉사활동 메인 페이지 이동
    @GetMapping("/volunteer/volunteerHome")
    public String volunteerHome(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(required = false) String title, Model model) {
//        페이지 기능
        Pageable pageable = PageRequest.of(page, 9, Sort.by(Sort.Direction.DESC, "volunteerId"));
        Page<Volunteer> volunteers;

        if (title != null){
//            검색어가 있을 경우
            volunteers = volunteerService.findByTitle(pageable, title);
        }else {
//            검색어가 없을 경우
            volunteers = volunteerService.findAll(pageable);
        }

        model.addAttribute("volunteers", volunteers);
        model.addAttribute("title", title);
        return "volunteer/volunteerHome";
    }

    //    봉사활동 신청 페이지 이동
    @GetMapping("/volunteer/volunteerDetail/{volunteerId}")
    public String volunteerDetail(@PathVariable Long volunteerId,
                                  @ModelAttribute("currentUser") User user, Model model) {
//        프로그램 세부 사항 출력
        Volunteer volunteer = volunteerService.findById(volunteerId);
        model.addAttribute("volunteer", volunteer);
//        로그인 여부, 신청 여부
        boolean isLoggedIn = user != null;
        boolean isApplied = false;
//        로그인 한 경우 신청 여부 받아옴
        if (user != null) {
            isApplied = volunteer.getApplicants().contains(user);
        }
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isApplied", isApplied);

        return "volunteer/volunteerDetail";
    }

    //    봉사활동 작성 페이지 이동
    @GetMapping("/volunteer/volunteerWrite")
    public String volunteerWriteForm(Model model) {
        VolunteerDto volunteerDto = new VolunteerDto();
        volunteerDto.setMaxApplyCount(1);
        model.addAttribute("volunteerForm", volunteerDto);
        return "volunteer/volunteerWrite";
    }

    //    봉사활동 추가
    @PostMapping("/volunteer/volunteerWrite")
    public String volunteerPost(@ModelAttribute("volunteerForm") VolunteerDto volunteerDto) {
        MultipartFile file = volunteerDto.getFile();
        if (file != null && !file.isEmpty()) {
//            첨부 파일이 있을 경우 실행
            try {
                String uploadDir = "C:/upload/volunteer/"; // 저장한 경로에 맞게 수정
                String originalFilename = file.getOriginalFilename();
                // 디렉토리 존재 확인 및 생성
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                Path savePath = Paths.get(uploadDir + originalFilename);
                file.transferTo(savePath.toFile());

                // 파일 이름을 dto에도 저장
                volunteerDto.setFilename(originalFilename);

            } catch (IOException e) {
                e.printStackTrace();  // 예외 처리
            }
        }

        volunteerService.save(volunteerDto);
        return "redirect:/volunteer/volunteerHome";
    }

    //    봉사활동 신청
    @PostMapping("/volunteer/apply/{id}")
    public String applyVolunteer(@PathVariable Long id,
                                 @ModelAttribute("currentUser") User user) {
        if (user == null) {
            return "redirect:/login";
        }
        Volunteer volunteer = volunteerService.findById(id);
        if (volunteer == null) {
            return "redirect:/volunteer/volunteerHome";
        }
        volunteerService.applyVolunteer(volunteer, user);

        return "redirect:/volunteer/volunteerDetail/{id}";
    }

    //    봉사활동 취소
    @PostMapping("/volunteer/cancel/{id}")
    public String cancelVolunteer(@PathVariable Long id,
                                  @ModelAttribute("currentUser") User user) {
        if (user == null) {
            return "redirect:/login";
        }
        Volunteer volunteer = volunteerService.findById(id);
        if (volunteer == null) {
            return "redirect:/volunteer/volunteerHome";
        }
        volunteerService.cancelVolunteer(volunteer, user);

        return "redirect:/volunteer/volunteerDetail/{id}";
    }

    //    봉사활동 삭제
    @Secured("ROLE_ADMIN")
    @PostMapping("/volunteer/delete/{id}")
    public String deleteVolunteer(@PathVariable Long id) {
        Volunteer volunteer = volunteerService.findById(id);
        if (volunteer != null) {
//            volunteerService.deleteById(id);
            volunteerService.updateProgram(id);
        }
        return "redirect:/volunteer/volunteerHome";
    }

    //    봉사활동 수정 페이지 이동
    @Secured("ROLE_ADMIN")
    @GetMapping("/volunteer/volunteerUpdate/{id}")
    public String updateVolunteerForm(@PathVariable Long id, Model model) {
        Volunteer volunteer = volunteerService.findById(id);
        VolunteerDto volunteerDto = volunteerService.convertToDto(volunteer);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        model.addAttribute("formattedApplyStartTime", volunteer.getApplyStartTime().format(formatter));
        model.addAttribute("formattedApplyEndTime", volunteer.getApplyEndTime().format(formatter));

        model.addAttribute("volunteerForm", volunteerDto);
        return "volunteer/volunteerUpdate";
    }

    //    봉사활동 수정
    @PostMapping("/volunteer/volunteerUpdate/{id}")
    public String updateVolunteer(@PathVariable Long id,
                                  @ModelAttribute("volunteerForm") VolunteerDto volunteerDto) {
        volunteerService.updateVolunteer(volunteerDto, id);

        return "redirect:/volunteer/volunteerHome";
    }

    //    봉사활동 페이지 파일 다운로드 관련
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws MalformedURLException {
        Path filePath = Paths.get("C:/upload/volunteer/").resolve(filename).normalize();
        UrlResource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 파일명 인코딩 (한글 파일명 대응)
        String encodedFilename = UriUtils.encode(filename, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)  // 여기 고정
                .body(resource);
    }
//    내가 신청한 봉사활동 목록
    @GetMapping("/volunteer/volunteerMy")
    public String volunteerMy(@ModelAttribute("currentUser") User user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }

        List<Volunteer> myVolunteers = volunteerService.findByApplicant(user);
//        최신 봉사활동 순으로 정렬
        Collections.reverse(myVolunteers);
        model.addAttribute("myVolunteers", myVolunteers);
        return "/volunteer/volunteerMy";
    }
}