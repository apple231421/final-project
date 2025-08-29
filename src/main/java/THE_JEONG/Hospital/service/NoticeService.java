package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.NoticeDto;
import THE_JEONG.Hospital.entity.Notice;
import THE_JEONG.Hospital.repository.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service("noticeService")
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final String uploadDir = "C:/upload/notice/";
    private final DeepLService deepLService;

    public NoticeService(NoticeRepository articleRepository, DeepLService deepLService) {
        this.noticeRepository = articleRepository;
        this.deepLService = deepLService;
    }

    public void saveNotice(Notice notice, List<MultipartFile> files, List<MultipartFile> filesEn) throws IOException {
        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        List<String> originalFilenames = new ArrayList<>();
        List<String> storedFilenames = new ArrayList<>();
        List<String> filePaths = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String storedFilename = UUID.randomUUID() + "_" + originalFilename;
                    File dest = new File(directory, storedFilename);
                    file.transferTo(dest);

                    originalFilenames.add(originalFilename);
                    storedFilenames.add(storedFilename);
                    filePaths.add("/images/notice/" + storedFilename);
                }
            }
        }
        // 영문 첨부파일 저장
        List<String> originalFilenamesEn = new ArrayList<>();
        List<String> storedFilenamesEn = new ArrayList<>();
        List<String> filePathsEn = new ArrayList<>();
        if (filesEn != null && !filesEn.isEmpty()) {
            for (MultipartFile file : filesEn) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String storedFilename = UUID.randomUUID() + "_" + originalFilename;
                    File dest = new File(directory, storedFilename);
                    file.transferTo(dest);
                    originalFilenamesEn.add(originalFilename);
                    storedFilenamesEn.add(storedFilename);
                    filePathsEn.add("/images/notice/" + storedFilename);
                }
            }
        }
        notice.setFilenameEn(String.join(",", originalFilenamesEn));
        notice.setStoredFilenameEn(String.join(",", storedFilenamesEn));
        notice.setFilePathEn(String.join(",", filePathsEn));

        String title = notice.getTitle();
        String titleEn = deepLService.translateToEnglish(title);
        notice.setTitleEn(titleEn);

        String content = notice.getContent();
        String contentEn = deepLService.translateToEnglish(content);
        notice.setContentEn(contentEn);

        notice.setFilename(String.join(",", originalFilenames));
        notice.setStoredFilename(String.join(",", storedFilenames));
        notice.setFilePath(String.join(",", filePaths));

        if (!filePathsEn.isEmpty()) {
            notice.setFilePathEn(String.join(",", filePathsEn));
        }

        noticeRepository.save(notice);
    }

    // Optional: 파일 사이즈 표시용 (KB)
    public String getFileSizeDisplay(String storedFileName) {
        File file = new File(uploadDir, storedFileName);
        if (file.exists()) {
            long size = file.length();
            return size / 1024 + "KB";
        }
        return "-";
    }



    // 전체 조회 (관리자)
    public Page<NoticeDto> getPagedNotice(Pageable pageable) {
        return noticeRepository.findAllByOrderByCreatedDateDesc(pageable)
                .map(NoticeDto::fromEntity);
    }

    // 삭제된 글 안보이게 조회 (모든 사용자)
    public Page<NoticeDto> getAllNotice(Pageable pageable) {
        return noticeRepository.findByDeletedFalse(pageable)
                .map(Notice::fromEntity);
    }


    public void markAsDeleted(Long id) {
        Notice notice = noticeRepository.findById(id).orElseThrow();
        notice.setDeleted(true);
        noticeRepository.save(notice);
    }

    public void restoreDeleted(Long id) {
        Notice notice = noticeRepository.findById(id).orElseThrow();
        notice.setDeleted(false);
        noticeRepository.save(notice);
    }

    // 휴지통 복구 관련 메서드
    public Page<NoticeDto> getDeletedNotice(Pageable pageable) {
        return noticeRepository.findByDeletedTrueOrderByCreatedDateDesc(pageable)
                .map(NoticeDto::fromEntity);
    }

    // 언론보도 수정
    public NoticeDto findById(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보도자료입니다."));
        return NoticeDto.fromEntity(notice);
    }

    public Page<NoticeDto> searchNotice(String searchType, String keyword, Pageable pageable, boolean isAdmin) {
        Page<Notice> result;

        switch (searchType) {
            case "category":
                result = isAdmin
                        ? noticeRepository.findBycategoryContainingIgnoreCaseOrderByCreatedDateDesc(keyword, pageable)
                        : noticeRepository.findBycategoryContainingIgnoreCaseAndDeletedFalseOrderByCreatedDateDesc(keyword, pageable);
                break;
            case "title":
                result = isAdmin
                        ? noticeRepository.findByTitleContainingIgnoreCaseOrderByCreatedDateDesc(keyword, pageable)
                        : noticeRepository.findByTitleContainingIgnoreCaseAndDeletedFalseOrderByCreatedDateDesc(keyword, pageable);
                break;
            default:
                result = noticeRepository.findByDeletedFalse(pageable); // fallback
        }

        return result.map(NoticeDto::fromEntity);
    }

    public Notice getNoticeById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지사항이 존재하지 않습니다. id=" + id));
    }

    // 다음글 조회 (관리자용)
    public NoticeDto getNextNotice(Long currentId) {
        List<Notice> notices = noticeRepository.findNextNotices(currentId);
        return notices.isEmpty() ? null : NoticeDto.fromEntity(notices.get(0));
    }

    // 이전글 조회 (관리자용)
    public NoticeDto getPrevNotice(Long currentId) {
        List<Notice> notices = noticeRepository.findPrevNotices(currentId);
        return notices.isEmpty() ? null : NoticeDto.fromEntity(notices.get(0));
    }

    // 다음글 조회 (일반사용자용)
    public NoticeDto getNextNoticeForUser(Long currentId) {
        List<Notice> notices = noticeRepository.findNextNoticesForUser(currentId);
        return notices.isEmpty() ? null : NoticeDto.fromEntity(notices.get(0));
    }

    // 이전글 조회 (일반사용자용)
    public NoticeDto getPrevNoticeForUser(Long currentId) {
        List<Notice> notices = noticeRepository.findPrevNoticesForUser(currentId);
        return notices.isEmpty() ? null : NoticeDto.fromEntity(notices.get(0));
    }

    // 공지사항 수정 (기존 파일 유지/삭제/추가)
    public void updateNotice(Long id, NoticeDto noticeDto, List<MultipartFile> files, List<MultipartFile> filesEn,
                            List<String> deleteFiles, List<String> deleteFilesEn) throws IOException {
        Notice notice = noticeRepository.findById(id).orElseThrow();
        // 기본 필드 갱신
        notice.setCategory(noticeDto.getCategory());
        notice.setTitle(noticeDto.getTitle());
        notice.setTitleEn(noticeDto.getTitleEn());
        notice.setContent(noticeDto.getContent());
        notice.setContentEn(noticeDto.getContentEn());

        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        // 기존 파일 목록 분리
        List<String> originalFilenames = new ArrayList<>();
        List<String> storedFilenames = new ArrayList<>();
        List<String> filePaths = new ArrayList<>();
        if (notice.getFilename() != null && !notice.getFilename().isEmpty())
            originalFilenames = new ArrayList<>(List.of(notice.getFilename().split(",")));
        if (notice.getStoredFilename() != null && !notice.getStoredFilename().isEmpty())
            storedFilenames = new ArrayList<>(List.of(notice.getStoredFilename().split(",")));
        if (notice.getFilePath() != null && !notice.getFilePath().isEmpty())
            filePaths = new ArrayList<>(List.of(notice.getFilePath().split(",")));

        // 파일 삭제 처리
        if (deleteFiles != null) {
            for (String del : deleteFiles) {
                int idx = storedFilenames.indexOf(del);
                if (idx >= 0) {
                    storedFilenames.remove(idx);
                    if (originalFilenames.size() > idx) originalFilenames.remove(idx);
                    if (filePaths.size() > idx) filePaths.remove(idx);
                    // 실제 파일도 삭제(옵션)
                    File f = new File(directory, del);
                    if (f.exists()) f.delete();
                }
            }
        }
        // 새 파일 추가
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String storedFilename = UUID.randomUUID() + "_" + originalFilename;
                    File dest = new File(directory, storedFilename);
                    file.transferTo(dest);
                    originalFilenames.add(originalFilename);
                    storedFilenames.add(storedFilename);
                    filePaths.add("/images/notice/" + storedFilename);
                }
            }
        }
        notice.setFilename(String.join(",", originalFilenames));
        notice.setStoredFilename(String.join(",", storedFilenames));
        notice.setFilePath(String.join(",", filePaths));

        // 영문 파일 처리
        List<String> originalFilenamesEn = new ArrayList<>();
        List<String> storedFilenamesEn = new ArrayList<>();
        List<String> filePathsEn = new ArrayList<>();
        if (notice.getFilenameEn() != null && !notice.getFilenameEn().isEmpty())
            originalFilenamesEn = new ArrayList<>(List.of(notice.getFilenameEn().split(",")));
        if (notice.getStoredFilenameEn() != null && !notice.getStoredFilenameEn().isEmpty())
            storedFilenamesEn = new ArrayList<>(List.of(notice.getStoredFilenameEn().split(",")));
        if (notice.getFilePathEn() != null && !notice.getFilePathEn().isEmpty())
            filePathsEn = new ArrayList<>(List.of(notice.getFilePathEn().split(",")));

        if (deleteFilesEn != null) {
            for (String del : deleteFilesEn) {
                int idx = storedFilenamesEn.indexOf(del);
                if (idx >= 0) {
                    storedFilenamesEn.remove(idx);
                    if (originalFilenamesEn.size() > idx) originalFilenamesEn.remove(idx);
                    if (filePathsEn.size() > idx) filePathsEn.remove(idx);
                    File f = new File(directory, del);
                    if (f.exists()) f.delete();
                }
            }
        }
        if (filesEn != null && !filesEn.isEmpty()) {
            for (MultipartFile file : filesEn) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String storedFilename = UUID.randomUUID() + "_" + originalFilename;
                    File dest = new File(directory, storedFilename);
                    file.transferTo(dest);
                    originalFilenamesEn.add(originalFilename);
                    storedFilenamesEn.add(storedFilename);
                    filePathsEn.add("/images/notice/" + storedFilename);
                }
            }
        }
        notice.setFilenameEn(String.join(",", originalFilenamesEn));
        notice.setStoredFilenameEn(String.join(",", storedFilenamesEn));
        notice.setFilePathEn(String.join(",", filePathsEn));

        if (!filePathsEn.isEmpty()) {
            notice.setFilePathEn(String.join(",", filePathsEn));
        }


        noticeRepository.save(notice);
    }

}
