package THE_JEONG.Hospital.service;

import THE_JEONG.Hospital.entity.Department;
import THE_JEONG.Hospital.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DeepLService deepLService;

    /**
     * 한글 진료과명을 받아 자동 번역 후 영문명까지 저장
     */
    @Transactional
    public void save(String nameKo) {
        // ✅ 중복 확인
        if (departmentRepository.existsByName(nameKo)) {
            throw new IllegalArgumentException("이미 존재하는 진료과입니다: " + nameKo);
        }

        // ✅ DeepL API를 통해 영어 자동 번역
        String nameEn = deepLService.translateToEnglish(nameKo);

        // ✅ 번역 실패 시 기본값 설정
        if (nameEn == null || nameEn.isBlank()) {
            nameEn = nameKo; // 실패 시 한글명 그대로 저장
        }

        Department dept = new Department();
        dept.setName(nameKo);
        dept.setNameEn(nameEn);
        departmentRepository.save(dept);
    }

    /**
     * 이미 번역된 한글/영문명을 모두 받는 경우
     */
    @Transactional
    public void save(String nameKo, String nameEn) {
        if (departmentRepository.existsByName(nameKo)) {
            throw new IllegalArgumentException("이미 존재하는 진료과입니다: " + nameKo);
        }

        Department dept = new Department();
        dept.setName(nameKo);
        dept.setNameEn(nameEn != null ? nameEn : nameKo);
        departmentRepository.save(dept);
    }

    public List<String> getAllNames() {
        return departmentRepository.findAll()
                .stream()
                .map(Department::getName)
                .collect(Collectors.toList());
    }

    public boolean existsByName(String department) {
        return departmentRepository.existsByName(department);
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }
}
