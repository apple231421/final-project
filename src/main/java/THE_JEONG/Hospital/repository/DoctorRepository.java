package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.Department;
import THE_JEONG.Hospital.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // ✅ 진료과 이름으로 검색 (정확히 일치)
    List<Doctor> findByDepartment_Name(String departmentName);

    // ✅ 진료과 이름 (영문) 정확히 일치
    List<Doctor> findByDepartment_NameEn(String departmentNameEn);

    // ✅ 이름 + 진료과명 포함 검색 (LIKE)
    List<Doctor> findByNameContainingAndDepartment_NameContaining(String name, String departmentName);

    // ✅ 진료과명 한글 LIKE 검색
    List<Doctor> findByDepartment_NameContaining(String name);

    // ✅ 진료과명 영문 LIKE 검색
    List<Doctor> findByDepartment_NameEnContaining(String nameEn);

    // ✅ 한글/영문 OR 조건 LIKE 검색
    List<Doctor> findByDepartment_NameContainingOrDepartment_NameEnContaining(String nameKo, String nameEn);

    // ✅ 이름 + 진료과 정확히 일치 검색 (영문/한글)
    List<Doctor> findByNameContainingAndDepartment_Name(String name, String deptKo);
    List<Doctor> findByNameContainingAndDepartment_NameEn(String name, String deptEn);

    // ✅ 이메일로 단일 검색
    Doctor findByEmail(String email);

    // ✅ 부서 엔티티 자체로 검색
    List<Doctor> findByDepartment(Department department);

    // ✅ 이름 LIKE 검색 (페이징 지원)
    Page<Doctor> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Doctor> findByName(String doctorName);

    List<Doctor> findByNameContaining(String doctorName);


//    추가좀할게요

}
