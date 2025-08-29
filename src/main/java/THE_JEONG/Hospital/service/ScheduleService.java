package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.entity.Doctor;
import THE_JEONG.Hospital.entity.Schedule;
import THE_JEONG.Hospital.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public List<Schedule> getByDoctor(Doctor doctor) {
        return scheduleRepository.findByDoctor(doctor);
    }

    public Map<Long, List<Schedule>> getAllGroupedByDoctor() {
        List<Schedule> allSchedules = scheduleRepository.findAll();
        return allSchedules.stream()
                .collect(Collectors.groupingBy(s -> s.getDoctor().getId()));
    }
}
