package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.dto.FaQDto;
import THE_JEONG.Hospital.dto.QnAAnswerDto;
import THE_JEONG.Hospital.dto.QnADto;
import THE_JEONG.Hospital.entity.*;
import THE_JEONG.Hospital.service.FaQService;
import THE_JEONG.Hospital.service.QnAAnswerService;
import THE_JEONG.Hospital.service.QnAService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class QnAController {
    private final QnAService qnaService;
    private final FaQService faqService;
    private final QnAAnswerService qnaAnswerService;

//    FaQ 페이지 이동
    @GetMapping("/guide/faq")
    public String faqHome(@RequestParam(defaultValue = "all") String category,
                          @RequestParam(defaultValue = "0") int page, Model model){

//        페이지 기능
        Pageable pageable = PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, "faqId"));
        Page<FaQ> faqPage;

//        카테고리 필터링 기능
        if ("all".equals(category)){
            faqPage = faqService.findAllPage(pageable);
        } else {
            faqPage = faqService.findByCategory(category, pageable);
        }

        model.addAttribute("faqPage", faqPage);
        model.addAttribute("currentCategory", category);
        return "guide/faq";
    }
//    FaQ 작성 페이지 이동
    @GetMapping("/guide/faqWrite")
    public String faqWrite(Model model){
        FaQDto faqDto = new FaQDto();
        model.addAttribute("faqForm", faqDto);
        return "guide/faqWrite";
    }
//    FaQ 작성
    @PostMapping("/guide/faqWrite")
    public String faqPost(@Valid @ModelAttribute FaQDto faqDto){
        faqService.save(faqDto);
        return "redirect:/guide/faq";
    }
//    FaQ 수정 페이지 이동
    @GetMapping("/guide/faqUpdate/{id}")
    public String faqUpdateForm(@PathVariable Integer id, Model model){
        FaQ faq = faqService.findById(id);
        FaQDto faqDto = faqService.convertDto(faq);
        model.addAttribute("faqForm", faqDto);
        return "guide/faqUpdate";
    }
//    FaQ 수정
    @Secured("ROLE_ADMIN")
    @PostMapping("/guide/faqUpdate/{id}")
        public String faqUpdate(@PathVariable Integer id,
                                @ModelAttribute("faqForm") FaQDto faqDto){
            faqService.updateFaQ(faqDto, id);
            return "redirect:/guide/faq";
        }
//    FaQ 삭제
    @Secured("ROLE_ADMIN")
    @PostMapping("/guide/faqDelete/{id}")
        public String faqDelete(@PathVariable Integer id){
            FaQ faq = faqService.findById(id);
            if (faq != null){
                faqService.deleteById(id);
            }
            return "redirect:/guide/faq";
        }

//    QnA 홈페이지 이동
    @GetMapping("/guide/consult")
    public String qnAHome(Model model){
        return "guide/qna";
    }

    //    QnA 문의작성 페이지 이동
    @GetMapping("/guide/qnaWrite")
    public String qna(@AuthenticationPrincipal Object principal, Model model) {
        QnADto qnaDto = new QnADto();

//        html에 hidden으로 보낼 유저 고유 번호
        if (principal instanceof CustomUserDetails userDetails) {
            qnaDto.setUserId(userDetails.getUserId());
        } else if (principal instanceof CustomOAuth2User oauthUser) {
            qnaDto.setUserId(oauthUser.getUserDetails().getUserId());
        }

        model.addAttribute("qnaForm", qnaDto);
        return "guide/qnaWrite";
    }

    //    QnA 문의 작성
    @PostMapping("/guide/qnaWrite")
    public String qnaPost(@Valid @ModelAttribute QnADto qnaDto, HttpServletRequest request){
//        파일 첨부 기능
        List<MultipartFile> files = qnaDto.getFiles();
        List<String > storedFilenames = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            if (files.size() > 3) {
                throw new IllegalArgumentException("최대 3개의 파일만 업로드할 수 있습니다.");
            }
//            첨부 파일이 있을 경우 실행
                String uploadDir = "C:/upload/"; // 저장한 경로에 맞게 수정
                // 디렉토리 존재 확인 및 생성
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                for (MultipartFile file : files){
                    try {
                        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                            throw new IllegalArgumentException("파일 크기는 10MB 이하만 가능합니다.");
                        }
                        String originalFilename = file.getOriginalFilename();
                        Path savePath = Paths.get(uploadDir + originalFilename);
                        file.transferTo(savePath.toFile());
                        storedFilenames.add(originalFilename); // 저장된 파일명 리스트에 추가
                    } catch (IOException e) {
                        e.printStackTrace();  // 예외 처리
                    }
                }
                // 파일 이름을 dto에도 저장
                qnaDto.setFilenames(storedFilenames);
        }
        qnaService.save(qnaDto);
        
        // Mixed Content 에러 방지를 위해 현재 프로토콜에 맞는 URL 생성
        String protocol = request.getHeader("X-Forwarded-Proto");
        if (protocol == null) {
            protocol = request.isSecure() ? "https" : "http";
        }
        String redirectUrl = protocol + "://" + request.getServerName() + "/guide/myQnA";
        return "redirect:" + redirectUrl;
    }

//    내가 작성한 QnA 목록 페이지 이동
    @GetMapping("/guide/myQnA")
    public String myQnA(@ModelAttribute("currentUser") User user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }
        try {
            List<QnA> qnaList = qnaService.findAllByUserId(user.getUserId());
            model.addAttribute("qnaList", qnaList != null ? qnaList : Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("qnaList", Collections.emptyList());
            // 필요시 에러 페이지 리턴 or 에러 메시지 전달
        }
        return "guide/myQnA";
    }

//    내가 작성한 QnA 상세 페이지 이동
    @GetMapping("/guide/myQnA/{qnaId}")
    public String myQnADetail(@PathVariable Integer qnaId, Model model){
        QnA qna = qnaService.findById(qnaId);
        model.addAttribute("qna", qna);

        QnAAnswer qnaAnswer = qnaAnswerService.findByQnAId(qnaId);
        if (qnaAnswer != null){
            model.addAttribute("qnaAnswer", qnaAnswer);
        }

        return "guide/myQnADetail";
    }

//    접수된 QnA 목록 페이지 이동
    @GetMapping("/guide/qnaAnswer")
    public String qnaList(@RequestParam(defaultValue = "all") String category, Model model){
        List<QnA> qnaList;
        //        카테고리 필터링 기능
        if ("all".equals(category)){
            qnaList = qnaService.findAll();
        } else {
            qnaList = qnaService.findByCategory(category);
        }

        model.addAttribute("currentCategory", category);
        model.addAttribute("qnaList", qnaList);
        return "guide/qnaAnswer";
    }
    
//    QnA 답변 작성 페이지 이동
    @GetMapping("/guide/qnaAnswerDetail/{qnaId}")
    public String qnaAnswer(@PathVariable Integer qnaId, Model model){
        QnA qna = qnaService.findById(qnaId);
        model.addAttribute("qna", qna);
//        답변이 이미 존재하는지 확인
        QnAAnswer qnaAnswer = qnaAnswerService.findByQnAId(qnaId);
        QnAAnswerDto qnaAnswerDto;
        if (qnaAnswer != null){ //답변이 이미 존재할 경우
            qnaAnswerDto = new QnAAnswerDto(qnaAnswer);
            model.addAttribute("qnaAnswer", qnaAnswerDto); //존재하는 답변 출력
        } else { //답변이 존재하지 않을 경우
            qnaAnswerDto = new QnAAnswerDto();
            qnaAnswerDto.setQnaId(qnaId);
            model.addAttribute("qnaAnswerForm", qnaAnswerDto); //답변 입력 폼 출력
        }

        return "guide/qnaAnswerDetail";
    }
//    QnA 답변 페이지에서 첨부파일 다운로드 기능
    @GetMapping("/guide/qnaDownload/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get("C:/upload/") // 저장한 경로에 맞게 수정
                .resolve(filename)
                .normalize();

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new FileNotFoundException("파일이 존재하지 않습니다: " + filename);
        }



        String encodedFilename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .body(resource);
    }
    
//    QnA 답변 작성
    @PostMapping("/guide/qnaAnswerDetail/{qnaId}")
    public String qnaAnswerPost(@PathVariable Integer qnaId, @ModelAttribute QnAAnswerDto qnaAnswerDto){
        QnA qna = qnaService.findById(qnaId);
        qnaAnswerService.save(qnaAnswerDto, qna);
        return "redirect:/guide/qnaAnswerDetail/" + qnaId;
    }
}
