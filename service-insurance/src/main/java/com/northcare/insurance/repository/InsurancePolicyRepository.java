package com.northcare.insurance.repository;

import com.northcare.insurance.model.InsurancePolicy;
import com.northcare.insurance.model.InsurancePolicy.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, UUID> {
    List<InsurancePolicy> findByPatientId(UUID patientId);
    List<InsurancePolicy> findByPatientIdAndStatus(UUID patientId, PolicyStatus status);
    Optional<InsurancePolicy> findByPolicyNumber(String policyNumber);
    boolean existsByPolicyNumber(String policyNumber);
}
