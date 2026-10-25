package com.smartcollege.demo.repository;

import com.smartcollege.demo.entity.Resource;
import com.smartcollege.demo.entity.ResourceCategory;
import com.smartcollege.demo.entity.ResourceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    Optional<Resource> findByResourceCode(String resourceCode);

    boolean existsByResourceCode(String resourceCode);

    boolean existsByResourceCodeAndIdNot(String resourceCode, Long id);

    List<Resource> findByCategory(ResourceCategory category);

    List<Resource> findByStatus(ResourceStatus status);

    long countByStatus(ResourceStatus status);

    List<Resource> findByActiveTrue();

    long countByActiveTrue();

    List<Resource> findByActiveTrueAndCategory(ResourceCategory category);

    List<Resource> findByCategoryAndStatus(ResourceCategory category, ResourceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdWithLock(@Param("id") Long id);
}
