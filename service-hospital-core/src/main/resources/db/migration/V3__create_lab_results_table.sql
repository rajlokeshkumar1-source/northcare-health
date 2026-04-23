CREATE TABLE lab_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    test_name VARCHAR(200) NOT NULL,
    test_code VARCHAR(50),               -- LOINC code
    result VARCHAR(500) NOT NULL,
    unit VARCHAR(50),
    reference_range VARCHAR(100),
    is_abnormal BOOLEAN NOT NULL DEFAULT FALSE,
    ordered_by VARCHAR(200),
    result_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_lab_results_patient ON lab_results(patient_id);
CREATE INDEX idx_lab_results_date ON lab_results(result_date);
CREATE INDEX idx_lab_results_abnormal ON lab_results(is_abnormal) WHERE is_abnormal = TRUE;
CREATE INDEX idx_lab_results_test_code ON lab_results(test_code);
