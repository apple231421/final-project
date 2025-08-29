package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.entity.News;
import THE_JEONG.Hospital.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsRepository newsRepository;
    private final String uploadDir = "C:/upload/news/";

    public List<News> findAll() {
        return newsRepository.findAll();
    }

    public Optional<News> findById(Long id) {
        return newsRepository.findById(id);
    }

    @Transactional
    public News save(News news, MultipartFile imageFile) throws IOException {
        // 파일 업로드 처리
        if (imageFile != null && !imageFile.isEmpty()) {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            String originalFilename = imageFile.getOriginalFilename();
            String storedFilename = UUID.randomUUID() + "_" + originalFilename;
            File dest = new File(directory, storedFilename);
            imageFile.transferTo(dest);
            
            news.setFilename(originalFilename);
            news.setStoredFilename(storedFilename);
            news.setFilePath("/images/news/" + storedFilename);
        }
        
        return newsRepository.save(news);
    }

    @Transactional
    public News update(Long id, News updatedNews, MultipartFile imageFile, boolean deleteImage) throws IOException {
        News news = newsRepository.findById(id).orElseThrow();
        news.setTitle(updatedNews.getTitle());
        news.setAuthors(updatedNews.getAuthors());
        news.setContent(updatedNews.getContent());

        // 이미지 삭제 체크 시 기존 파일 삭제 및 정보 비움
        if (deleteImage) {
            if (news.getStoredFilename() != null) {
                File file = new File(uploadDir, news.getStoredFilename());
                if (file.exists()) file.delete();
            }
            news.setFilename(null);
            news.setStoredFilename(null);
            news.setFilePath(null);
        }

        // 새로운 이미지 업로드 시
        if (imageFile != null && !imageFile.isEmpty()) {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String originalFilename = imageFile.getOriginalFilename();
            String storedFilename = UUID.randomUUID() + "_" + originalFilename;
            File dest = new File(directory, storedFilename);
            imageFile.transferTo(dest);
            news.setFilename(originalFilename);
            news.setStoredFilename(storedFilename);
            news.setFilePath("/images/news/" + storedFilename);
        }
        return newsRepository.save(news);
    }

    @Transactional
    public void deleteById(Long id) {
        newsRepository.deleteById(id);
    }
} 