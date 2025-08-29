package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.dto.NoticeDto;
import THE_JEONG.Hospital.entity.Notice;
import THE_JEONG.Hospital.service.DeepLService;
import THE_JEONG.Hospital.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/board")
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;
    private final DeepLService deepLService;

    public NoticeController(NoticeService noticeService, DeepLService deepLService) {
        this.noticeService = noticeService;
        this.deepLService = deepLService;
    }

    private final String uploadDir = "C:/upload/notice/";

    // 리스트 (모든 사용자 접근 허용)
    @GetMapping("/notice")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable,
                       @RequestParam(required = false) String searchType,
                       @RequestParam(required = false) String keyword,
                       Model model, Authentication authentication) {

        if (page < 0) {
            return "redirect:/board/notice?page=0";
        }

        Page<NoticeDto> noticePage;

        // 관리자 여부 확인
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_A"));

        if (searchType != null && keyword != null && !keyword.isBlank()) {
            noticePage = noticeService.searchNotice(searchType, keyword, pageable, isAdmin);
        } else {
            if (isAdmin) {
                noticePage = noticeService.getPagedNotice(pageable);
            } else {
                noticePage = noticeService.getAllNotice(pageable);
            }
        }


        model.addAttribute("noticePage", noticePage);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        return "board/notice";
    }

    // 등록 폼 (admin만 접근)
    @GetMapping("/notice_writeForm")
    @PreAuthorize("hasRole('A')")
    public String showForm(Model model) {
        model.addAttribute("notice", new NoticeDto());
        return "board/notice_writeForm";
    }

    @GetMapping("/notice/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) throws IOException {
        File file = new File("C:/upload/notice/", fileName);
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new InputStreamResource(new FileInputStream(file));
        String encodedFilename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }


    // 등록 처리 (admin만 접근)
    @PostMapping("/notice/new")
    @PreAuthorize("hasRole('A')")
    public String submitForm(@ModelAttribute("notice") NoticeDto noticeDto,
                             @RequestParam("files") List<MultipartFile> files,
                             @RequestParam(value = "filesEn", required = false) List<MultipartFile> filesEn) throws IOException {
        Notice notice = noticeDto.toEntity(); // DTO를 엔티티로 변환
        noticeService.saveNotice(notice, files, filesEn); // 파일 저장 + DB 저장
        return "redirect:/board/notice";
    }

    @PostMapping("/api/translate")
    @ResponseBody
    public Map<String, String> autoTranslate(@RequestParam String text) {
        String en = deepLService.translate(text, "KO", "EN");
        return Map.of(
                "text", en != null ? en : ""
        );
    }


    // 글 보기 폼 불러오기
    @GetMapping("/notice_view/{id}")
    public String viewForm(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            Notice notice = noticeService.getNoticeById(id);
            if (notice == null) {
                return "redirect:/board/notice";
            }

            NoticeDto noticeDto = NoticeDto.fromEntity(notice);
            model.addAttribute("imageExts", List.of("jpg", "jpeg", "png", "gif", "webp"));  // ✅ 여기!
            model.addAttribute("notice", noticeDto);
            
            // 관리자 여부 확인
            boolean isAdmin = authentication != null &&
                    authentication.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_A"));
            
            // 다음글/이전글 정보 추가
            if (isAdmin) {
                model.addAttribute("nextNotice", noticeService.getNextNotice(id));
                model.addAttribute("prevNotice", noticeService.getPrevNotice(id));
            } else {
                model.addAttribute("nextNotice", noticeService.getNextNoticeForUser(id));
                model.addAttribute("prevNotice", noticeService.getPrevNoticeForUser(id));
            }
            
            return "board/notice_view";
        } catch (Exception e) {
            log.error("공지사항 조회 중 오류 발생", e);
            return "redirect:/error";
        }
    }

    // 관리자 게시글 삭제(안보이게)
    @PreAuthorize("hasRole('A')")
    @PostMapping("/notice/delete/{id}")
    public String delete(@PathVariable Long id) {
        noticeService.markAsDeleted(id);
        return "redirect:/board/notice";
    }

    // 관리자 게시글 복구(보이게)
    @PreAuthorize("hasRole('A')")
    @PostMapping("/notice/restore/{id}")
    public String restore(@PathVariable Long id) {
        noticeService.restoreDeleted(id);
        return "redirect:/board/notice";
    }

    @PreAuthorize("hasRole('A')")
    @GetMapping("/notice_trash")
    public String trashList(Pageable pageable, Model model) {
        Page<NoticeDto> noticePage = noticeService.getDeletedNotice(pageable);
        model.addAttribute("noticePage", noticePage);
        return "board/notice_trash";
    }

    @GetMapping("/notice/editForm/{id}")
    @PreAuthorize("hasRole('A')")
    public String editForm(@PathVariable Long id, Model model) {
        NoticeDto noticeDto = noticeService.findById(id);
        model.addAttribute("notice", noticeDto);
        return "board/notice_editForm";
    }

    // 등록 처리 (admin만 접근)
    @PostMapping("/notice/edit/{id}")
    @PreAuthorize("hasRole('A')")
    public String editNotice(@PathVariable Long id,
                            @ModelAttribute("notice") NoticeDto noticeDto,
                            @RequestParam("files") List<MultipartFile> files,
                            @RequestParam(value = "filesEn", required = false) List<MultipartFile> filesEn,
                            @RequestParam(value = "deleteFiles", required = false) List<String> deleteFiles,
                            @RequestParam(value = "deleteFilesEn", required = false) List<String> deleteFilesEn) throws IOException {
        noticeService.updateNotice(id, noticeDto, files, filesEn, deleteFiles, deleteFilesEn);
        return "redirect:/board/notice";
    }


}
