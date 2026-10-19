CREATE TABLE appointment_projection (
    appointment_id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appointment_projection_patient ON appointment_projection(patient_id);
CREATE INDEX idx_appointment_projection_doctor ON appointment_projection(doctor_id);
