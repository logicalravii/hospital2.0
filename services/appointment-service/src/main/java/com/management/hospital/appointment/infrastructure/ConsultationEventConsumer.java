package com.management.hospital.appointment.infrastructure;

import com.management.hospital.appointment.application.AppointmentService;
import com.management.hospital.common.event.ConsultationCompletedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsultationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ConsultationEventConsumer.class);
    private final AppointmentService appointmentService;

    public ConsultationEventConsumer(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @KafkaListener(topics = ConsultationCompletedEvent.TOPIC)
    public void handleConsultationCompleted(ConsultationCompletedEvent event) {
        log.info("Received ConsultationCompletedEvent for consultationId: {}, appointmentId: {}",
                event.consultationId(), event.appointmentId());

        if (event.appointmentId() != null) {
            try {
                appointmentService.completeAppointment(event.appointmentId());
                log.info("Successfully completed appointment {} via consultation event", event.appointmentId());
            } catch (Exception ex) {
                log.warn("Failed to auto-complete appointment {} on consultation event: {}",
                        event.appointmentId(), ex.getMessage());
            }
        }
    }

}
