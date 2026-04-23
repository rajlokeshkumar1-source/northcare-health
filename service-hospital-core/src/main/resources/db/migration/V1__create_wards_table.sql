CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE wards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    ward_type VARCHAR(20) NOT NULL CHECK (ward_type IN ('ICU','GENERAL','PEDIATRIC','EMERGENCY')),
    floor INTEGER NOT NULL,
    bed_count INTEGER NOT NULL,
    available_beds INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

INSERT INTO wards (name, ward_type, floor, bed_count, available_beds) VALUES
    ('ICU Ward A', 'ICU', 2, 20, 15),
    ('General Ward 1', 'GENERAL', 3, 40, 32),
    ('Pediatric Wing', 'PEDIATRIC', 4, 25, 20),
    ('Emergency Dept', 'EMERGENCY', 1, 30, 25);
