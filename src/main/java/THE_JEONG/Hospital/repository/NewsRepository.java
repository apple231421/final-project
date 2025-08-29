package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.News;
import THE_JEONG.Hospital.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByAuthors_Id(Long doctorId);
} 