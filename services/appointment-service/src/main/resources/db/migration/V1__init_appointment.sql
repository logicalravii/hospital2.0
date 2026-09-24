CREATE TABLE appointment (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_appointment_times CHECK (end_time > start_time)
);

CREATE INDEX idx_appointment_doctor_date ON appointment(doctor_id, appointment_date);
CREATE INDEX idx_appointment_patient ON appointment(patient_id);
CREATE INDEX idx_appointment_status ON appointment(status);
