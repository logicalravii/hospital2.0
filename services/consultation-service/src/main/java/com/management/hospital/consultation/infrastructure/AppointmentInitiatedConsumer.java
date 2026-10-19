package com.management.hospital.consultation.infrastructure;

import com.management.hospital.common.event.AppointmentInitiatedEvent;
import com.management.hospital.consultation.domain.AppointmentProjection;
import com.management.hospital.consultation.domain.AppointmentProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentInitiatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentInitiatedConsumer.class);

    private final AppointmentProjectionRepository projectionRepository;

    public AppointmentInitiatedConsumer(AppointmentProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Transactional
    @KafkaListener(topics = AppointmentInitiatedEvent.TOPIC)
    public void handleAppointmentInitiated(AppointmentInitiatedEvent event) {
        log.info("Projecting AppointmentInitiatedEvent for appointmentId: {}, patientId: {}, doctorId: {}",
                event.appointmentId(), event.patientId(), event.doctorId());

        AppointmentProjection projection = projectionRepository.findById(event.appointmentId())
                .map(existing -> {
                    existing.updateStatus("PENDING_VALIDATION");
                    return existing;
                })
                .orElseGet(() -> new AppointmentProjection(
                        event.appointmentId(),
                        event.patientId(),
                        event.doctorId(),
                        "PENDING_VALIDATION"
                ));

        projectionRepository.save(projection);
    }
}
