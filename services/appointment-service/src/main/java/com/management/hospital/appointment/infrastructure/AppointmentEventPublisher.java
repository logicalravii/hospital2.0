package com.management.hospital.appointment.infrastructure;

import com.management.hospital.common.event.AppointmentCancelledEvent;
import com.management.hospital.common.event.AppointmentInitiatedEvent;
import com.management.hospital.common.event.AppointmentScheduledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AppointmentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAppointmentScheduled(AppointmentScheduledEvent event) {
        log.info("Publishing AppointmentScheduledEvent for appointmentId: {}, doctorId: {}, patientId: {}",
                event.appointmentId(), event.doctorId(), event.patientId());
        kafkaTemplate.send(AppointmentScheduledEvent.TOPIC, event.appointmentId().toString(), event);
    }

    public void publishAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Publishing AppointmentCancelledEvent for appointmentId: {}, reason: {}",
                event.appointmentId(), event.reason());
        kafkaTemplate.send(AppointmentCancelledEvent.TOPIC, event.appointmentId().toString(), event);
    }

    public void publishAppointmentInitiated(AppointmentInitiatedEvent event) {
        log.info("Publishing AppointmentInitiatedEvent for appointmentId: {}, doctorId: {}, patientId: {}",
                event.appointmentId(), event.doctorId(), event.patientId());
        kafkaTemplate.send(AppointmentInitiatedEvent.TOPIC, event.appointmentId().toString(), event);
    }
}
