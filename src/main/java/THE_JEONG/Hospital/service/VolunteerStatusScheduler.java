package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.constant.VolunteerStatus;
import THE_JEONG.Hospital.entity.Volunteer;
import THE_JEONG.Hospital.repository.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VolunteerStatusScheduler {
    private final VolunteerRepository volunteerRepository;

//    매 시 정각마다 상태 체크해서 모집시간이 지나면 모집상태 변경
    @Scheduled(cron = "0 0 * * * *")
    public void updateVolunteerStatus(){
        List<Volunteer> volunteers = volunteerRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Volunteer volunteer : volunteers){
            if (volunteer.getApplyEndTime().isBefore(now)){
                if (volunteer.getStatus() == VolunteerStatus.RECRUITING){
                    volunteer.setStatus(VolunteerStatus.CLOSED);
                    volunteerRepository.save(volunteer);
                }
            }
        }
    }
}
