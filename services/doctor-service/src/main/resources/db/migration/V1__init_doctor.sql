CREATE TABLE doctor (
    id UUID PRIMARY KEY,
    user_id UUID,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    consultation_fee NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    room_number VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doctor_specialty ON doctor(specialty);
CREATE INDEX idx_doctor_email ON doctor(email);
CREATE INDEX idx_doctor_active ON doctor(active);
CREATE INDEX idx_doctor_last_name ON doctor(last_name);

CREATE TABLE doctor_schedule (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctor(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INT NOT NULL DEFAULT 30,
    max_patients INT NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_times CHECK (end_time > start_time),
    CONSTRAINT chk_slot_duration CHECK (slot_duration_minutes > 0),
    CONSTRAINT chk_max_patients CHECK (max_patients > 0)
);

CREATE INDEX idx_doctor_schedule_doctor_day ON doctor_schedule(doctor_id, day_of_week);
