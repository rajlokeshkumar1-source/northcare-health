package com.northcare.insurance.repository;

import com.northcare.insurance.model.Claim;
import com.northcare.insurance.model.Claim.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    Page<Claim> findByPolicyId(UUID policyId, Pageable pageable);
    Page<Claim> findByPatientId(UUID patientId, Pageable pageable);
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);
    Optional<Claim> findByClaimNumber(String claimNumber);
    long countByStatus(ClaimStatus status);
}
