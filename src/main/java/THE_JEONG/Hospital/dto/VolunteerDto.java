package THE_JEONG.Hospital.dto;

import THE_JEONG.Hospital.constant.VolunteerStatus;
import THE_JEONG.Hospital.entity.User;
import THE_JEONG.Hospital.entity.Volunteer;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerDto {
    private Long volunteerId;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title; //봉사활동 제목
    @NotBlank(message = "내용을 입력해주세요.")
    private String content; //프로그램 내용
    @NotBlank(message = "프로그램 신청 시작시간을 설정해주세요.")
    private LocalDateTime applyStartTime; //프로그램 신청 시작시간
    @NotBlank(message = "프로그램 신청 마감시간을 설정해주세요.")
    private LocalDateTime applyEndTime; //프로그램 신청 마감시간
    private VolunteerStatus status; //프로그램 진행 상태 (모집중, 모집완료, 진행중, 완료)
    private int applyCount; //신청 인원
    @NotBlank(message = "최대 인원을 설정해주세요.")
    private int maxApplyCount; //신청 최대 인원
    private List<User> applicants; //신청한 유저들
    private String filename; //파일 이름
    private MultipartFile file; //실제 파일

    public VolunteerDto(Volunteer volunteer){
        this.title = volunteer.getTitle();
        this.content = volunteer.getContent();
        this.applyStartTime = volunteer.getApplyStartTime();
        this.applyEndTime = volunteer.getApplyEndTime();
        this.status = volunteer.getStatus();
        this.applyCount = volunteer.getApplyCount();
        this.maxApplyCount = volunteer.getMaxApplyCount();
        this.applicants = volunteer.getApplicants();
        this.filename = volunteer.getFilename();
        this.title = volunteer.getTitle();
    }

    public Volunteer createVolunteer(){
        return Volunteer.builder()
                .volunteerId(this.volunteerId)
                .title(this.title)
                .content(this.content)
                .applyStartTime(this.applyStartTime)
                .applyEndTime(this.applyEndTime)
                .status(this.status != null ? this.status : VolunteerStatus.RECRUITING)
                .applyCount(this.applyCount)
                .maxApplyCount(this.maxApplyCount)
                .applicants(this.applicants != null ? this.applicants : new ArrayList<>())
                .filename(this.filename)
                .build();
    }
}
