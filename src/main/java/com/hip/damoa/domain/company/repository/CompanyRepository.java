package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    Optional<Company> findByIdAndIsDeletedFalse(Long id);

    Optional<Company> findByOwnerAndIsDeletedFalse(User owner);

    Optional<Company> findByOwnerId(Long ownerId);

    Optional<Company> findBySlug(String slug);

    boolean existsBySlug(String slug);

    org.springframework.data.domain.Page<Company> findByStatusAndIsDeletedFalse(String status, org.springframework.data.domain.Pageable pageable);
}
