CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    ssn_last4_encrypted VARCHAR(255),
    blood_type VARCHAR(5),
    allergies JSONB DEFAULT '[]',
    diagnosis_codes JSONB DEFAULT '[]',
    medications JSONB DEFAULT '[]',
    ward_id UUID REFERENCES wards(id),
    admission_date TIMESTAMP WITH TIME ZONE,
    discharge_date TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_patients_active ON patients(is_active);
CREATE INDEX idx_patients_ward ON patients(ward_id);
CREATE INDEX idx_patients_dob ON patients(date_of_birth);
CREATE INDEX idx_patients_name ON patients(last_name, first_name);
