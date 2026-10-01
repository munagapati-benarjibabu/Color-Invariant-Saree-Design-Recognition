package com.saree.search.repository;
import com.saree.search.entity.Saree;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SareeRepository extends JpaRepository<Saree, Long> { Optional<Saree> findByImageName(String imageName); }
