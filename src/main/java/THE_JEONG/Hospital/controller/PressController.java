package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.dto.PressDto;
import THE_JEONG.Hospital.service.PressService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/board")
public class PressController {

    private final PressService pressService;

    public PressController(PressService pressService) {
        this.pressService = pressService;
    }

    // 리스트 (모든 사용자 접근 허용)
    @GetMapping("/press")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @PageableDefault(size = 10, sort = "publishedDate", direction = Sort.Direction.DESC) Pageable pageable,
                       @RequestParam(required = false) String searchType,
                       @RequestParam(required = false) String keyword,
                       Model model, Authentication authentication) {

        if (page < 0) {
            return "redirect:/board/press?page=0";
        }

        Page<PressDto> pressPage;

        // 관리자 여부 확인
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_A"));

        if (searchType != null && keyword != null && !keyword.isBlank()) {
            pressPage = pressService.searchPress(searchType, keyword, pageable, isAdmin);
        } else {
            if (isAdmin) {
                pressPage = pressService.getPagedPress(pageable);
            } else {
                pressPage = pressService.getAllPress(pageable);
            }
        }


        model.addAttribute("pressPage", pressPage);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        return "board/press";
    }

    // 등록 폼 (admin만 접근)
    @GetMapping("/press_writeForm")
    @PreAuthorize("hasRole('A')")
    public String showForm(Model model) {
        model.addAttribute("press", new PressDto());
        return "board/press_writeForm";
    }

    // 등록 처리 (admin만 접근)
    @PostMapping("/press/new")
    @PreAuthorize("hasRole('A')")
    public String submitForm(@ModelAttribute("press") PressDto pressDto) {
        pressService.save(pressDto);
        return "redirect:/board/press";
    }

    // 관리자 게시글 삭제(안보이게)
    @PreAuthorize("hasRole('A')")
    @PostMapping("/press/delete/{id}")
    public String delete(@PathVariable Long id) {
        pressService.markAsDeleted(id);
        return "redirect:/board/press";
    }

    // 관리자 게시글 복구(보이게)
    @PreAuthorize("hasRole('A')")
    @PostMapping("/press/restore/{id}")
    public String restore(@PathVariable Long id) {
        pressService.restoreDeleted(id);
        return "redirect:/board/press";
    }

    @PreAuthorize("hasRole('A')")
    @GetMapping("/press_trash")
    public String trashList(Pageable pageable, Model model) {
        Page<PressDto> pressPage = pressService.getDeletedPress(pageable);
        model.addAttribute("pressPage", pressPage);
        return "board/press_trash";
    }

    @GetMapping("/press/editForm/{id}")
    @PreAuthorize("hasRole('A')")
    public String editForm(@PathVariable Long id, Model model) {
        PressDto pressDto = pressService.findById(id);
        model.addAttribute("press", pressDto);
        return "board/press_editForm";
    }

    // 등록 처리 (admin만 접근)
    @PostMapping("press/edit/{id}")
    @PreAuthorize("hasRole('A')")
    public String submitEditForm(@ModelAttribute("press") PressDto pressDto) {
        pressService.save(pressDto);
        return "redirect:/board/press";
    }
}
