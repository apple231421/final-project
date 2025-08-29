package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.dto.VolunteerDto;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.entity.Volunteer;
import THE_JEONG.Hospital.repository.VolunteerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static THE_JEONG.Hospital.constant.VolunteerStatus.*;

@Service
@RequiredArgsConstructor
public class VolunteerService {
    private final VolunteerRepository volunteerRepository;
//    봉사활동 프로그램 추가
    public void save(@Valid VolunteerDto volunteerDto){
        Volunteer volunteer = volunteerDto.createVolunteer();
        volunteerRepository.save(volunteer);
    }

//    봉사활동 프로그램 한 개 받아오기
    public Volunteer findById(Long volunteerId) {
        return volunteerRepository.findById(volunteerId).orElse(null);
    }
//    봉사활동 프로그램 전체를 페이지 형식으로 받아오기
    public Page<Volunteer> findAll(Pageable pageable) {
        return volunteerRepository.findAll(pageable);
    }

//    봉사활동 프로그램 한 개 삭제하기
    public void deleteById(Long id) {
        volunteerRepository.deleteById(id);
    }
//    봉사활동 프로그램 삭제 부분(프로그램 종료)
    public void updateProgram(Long id) {
        Volunteer volunteer = volunteerRepository.findById(id).orElse(null);
        if (volunteer != null){
            if (volunteer.getStatus() == COMPLETED){
                volunteerRepository.deleteById(id);
            } else {
                volunteer.setStatus(COMPLETED);
                volunteerRepository.save(volunteer);
            }
        }
    }
    
//    봉사활동 신청하기
    public void applyVolunteer(Volunteer volunteer, User user){
        if (volunteer.getApplicants() == null){
            volunteer.setApplicants(new ArrayList<>());
        }
//        신청인원이 마감됐는지 확인하는 부분
        if (volunteer.getApplyCount() < volunteer.getMaxApplyCount()){
            volunteer.getApplicants().add(user);
            volunteer.setApplyCount(volunteer.getApplyCount() + 1);
//            이 신청으로 인원이 마감된 경우
            if (volunteer.getApplyCount() >= volunteer.getMaxApplyCount()){
                volunteer.setStatus(CLOSED);
            }
        }
        volunteerRepository.save(volunteer);
    }
    
//    봉사활동 취소하기
    public void cancelVolunteer(Volunteer volunteer, User user){
        volunteer.getApplicants().remove(user);
        volunteer.setApplyCount(volunteer.getApplyCount() - 1);
        if (volunteer.getApplyCount() < volunteer.getMaxApplyCount()){
            volunteer.setStatus(RECRUITING);
        }
        volunteerRepository.save(volunteer);
    }
//    봉사활동 수정하기
    public void updateVolunteer(VolunteerDto volunteerDto, Long id) {
        Volunteer volunteer = findById(id);
        volunteer.setTitle(volunteerDto.getTitle());
        volunteer.setContent(volunteerDto.getContent());
        volunteer.setApplyStartTime(volunteerDto.getApplyStartTime());
        volunteer.setApplyEndTime(volunteerDto.getApplyEndTime());
        volunteer.setStatus(volunteerDto.getStatus());
        volunteer.setMaxApplyCount(volunteerDto.getMaxApplyCount());

        if (volunteerDto.getFile() != null && !volunteerDto.getFile().isEmpty()) {
            String originalFilename = saveFile(volunteerDto.getFile());
            volunteer.setFilename(originalFilename); // DB에는 원본 이름 저장
        }
        volunteerRepository.save(volunteer);
    }
//    봉사활동 수정 중 파일 관련
    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new IOException("파일 이름이 존재하지 않습니다.");
            }

            String uploadDir = "C:/upload/volunteer/";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 원본 파일명 그대로 사용
            Path savePath = Paths.get(uploadDir, originalFilename);

            file.transferTo(savePath.toFile());

            // 원본 파일 이름을 반환 (DB에는 원본 이름 저장)
            return originalFilename;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
//    봉사활동 수정에 필요한 엔티티 -> DTO 변환 부분
    public VolunteerDto convertToDto(Volunteer volunteer) {
        return VolunteerDto.builder()
                .volunteerId(volunteer.getVolunteerId())
                .title(volunteer.getTitle())
                .content(volunteer.getContent())
                .applyStartTime(volunteer.getApplyStartTime())
                .applyEndTime(volunteer.getApplyEndTime())
                .status(volunteer.getStatus())
                .applyCount(volunteer.getApplyCount())
                .maxApplyCount(volunteer.getMaxApplyCount())
                .filename(volunteer.getFilename())
                .build();
    }
//    내가 신청한 봉사활동 목록 받아오는 부분
    public List<Volunteer> findByApplicant(User user) {
        return volunteerRepository.findAllByApplicantsContaining(user);
    }
    //    제목으로 봉사활동 검색하는 부분
    public Page<Volunteer> findByTitle(Pageable pageable, String title) {
        return volunteerRepository.findByTitleContaining(pageable, title);
    }
}
