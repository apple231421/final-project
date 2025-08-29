package THE_JEONG.Hospital.repository;

import THE_JEONG.Hospital.entity.FaQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaQRepository extends JpaRepository<FaQ, Integer> {
    Page<FaQ> findByFaqCategory(String category, Pageable pageable);
}
