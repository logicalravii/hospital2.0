CREATE TABLE consultation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL UNIQUE,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    chief_complaint TEXT NOT NULL,
    diagnosis TEXT NOT NULL,
    clinical_notes TEXT,
    vitals JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consultation_patient ON consultation(patient_id);
CREATE INDEX idx_consultation_doctor ON consultation(doctor_id);
CREATE INDEX idx_consultation_appointment ON consultation(appointment_id);
