package THE_JEONG.Hospital.controller;

import THE_JEONG.Hospital.entity.News;
import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.service.NewsService;
import THE_JEONG.Hospital.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board/news")
public class NewsController {
    private final NewsService newsService;
    private final DoctorService doctorService;

    @GetMapping("")
    public String list(Model model) {
        model.addAttribute("newsList", newsService.findAll());
        return "board/news";
    }

    @GetMapping("/write")
    public String writeForm(Model model) {
        model.addAttribute("doctors", doctorService.findAll());
        return "board/news_write";
    }

    @PostMapping("/write")
    public String write(@RequestParam String title, @RequestParam List<Long> authorIds, @RequestParam String content,
                        @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) throws IOException {
        News news = new News();
        news.setTitle(title);
        news.setContent(content);
        
        // 선택된 의사들을 작성자로 설정
        List<Doctor> allDoctors = doctorService.findAll();
        List<Doctor> authors = authorIds.stream()
                .map(authorId -> allDoctors.stream()
                        .filter(doctor -> doctor.getId().equals(authorId))
                        .findFirst()
                        .orElse(null))
                .filter(doctor -> doctor != null)
                .collect(Collectors.toList());
        news.setAuthors(authors);
        
        newsService.save(news, imageFile);
        return "redirect:/board/news";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        News news = newsService.findById(id).orElseThrow();
        model.addAttribute("news", news);
        
        // 작성자 정보 추가
        model.addAttribute("authors", news.getAuthors());
        return "board/news_view";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        News news = newsService.findById(id).orElseThrow();
        model.addAttribute("news", news);
        model.addAttribute("doctors", doctorService.findAll());
        return "board/news_edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @RequestParam String title, @RequestParam List<Long> authorIds, @RequestParam String content,
                      @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                      @RequestParam(value = "deleteImage", required = false) String deleteImage) throws IOException {
        News updatedNews = new News();
        updatedNews.setTitle(title);
        updatedNews.setContent(content);
        // 선택된 의사들 설정
        List<Doctor> allDoctors = doctorService.findAll();
        List<Doctor> authors = authorIds.stream()
                .map(authorId -> allDoctors.stream()
                        .filter(doctor -> doctor.getId().equals(authorId))
                        .findFirst()
                        .orElse(null))
                .filter(doctor -> doctor != null)
                .collect(Collectors.toList());
        updatedNews.setAuthors(authors);
        newsService.update(id, updatedNews, imageFile, deleteImage != null);
        return "redirect:/board/news/" + id;
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        newsService.deleteById(id);
        return "redirect:/board/news";
    }
} 