package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Department findByName(String department);
    boolean existsByName(String department);

    Department findByNameEn(String department);
    Optional<Department> findByNameOrNameEn(String name, String nameEn);
}
