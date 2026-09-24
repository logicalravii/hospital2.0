package com.management.hospital.doctor.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.management.hospital.common.event.DoctorSlotAvailableEvent;
import com.management.hospital.common.event.DoctorSlotOverlap;

@Component
public class DoctorEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DoctorEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DoctorEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAppointmentOverlaps(DoctorSlotOverlap event) {
        log.info("Publishing DoctorSlotOverlap for appointmentId: {}, doctorId: {}, ", event.appointmentId(),
                event.doctorId());
        kafkaTemplate.send(DoctorSlotOverlap.TOPIC, event.appointmentId().toString(), event);
    }

    public void publishDoctorSlotAvailable(DoctorSlotAvailableEvent event) {
        log.info("Publishing DoctorSlotAvailableEvent for appointmentId: {}, doctorId: {}, patientId: {}",
                event.appointmentId(), event.doctorId());
        kafkaTemplate.send(DoctorSlotAvailableEvent.TOPIC, event.appointmentId().toString(), event);
    }

}
