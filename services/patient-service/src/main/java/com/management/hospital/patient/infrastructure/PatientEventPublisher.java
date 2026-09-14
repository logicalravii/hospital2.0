package com.management.hospital.patient.infrastructure;

import com.management.hospital.common.event.PatientRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PatientEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PatientEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PatientEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPatientRegistered(PatientRegisteredEvent event) {
        log.info("Publishing PatientRegisteredEvent for patientId: {}, MRN: {}", event.patientId(), event.mrn());
        kafkaTemplate.send(PatientRegisteredEvent.TOPIC, event.patientId().toString(), event);
    }
}
