package com.management.hospital.consultation.infrastructure;

import com.management.hospital.common.event.AppointmentCancelledEvent;
import com.management.hospital.common.event.AppointmentScheduledEvent;
import com.management.hospital.consultation.domain.AppointmentProjection;
import com.management.hospital.consultation.domain.AppointmentProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@KafkaListener(id = "consultation-saga-consumer", topics = "hospital.appointment.saga.events")
public class AppointmentSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSagaConsumer.class);

    private final AppointmentProjectionRepository projectionRepository;

    public AppointmentSagaConsumer(AppointmentProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Transactional
    @KafkaHandler
    public void handleScheduled(AppointmentScheduledEvent event) {
        log.info("Projecting AppointmentScheduledEvent for appointmentId: {}", event.appointmentId());

        AppointmentProjection projection = projectionRepository.findById(event.appointmentId())
                .map(existing -> {
                    existing.updateStatus("CONFIRMED");
                    return existing;
                })
                .orElseGet(() -> new AppointmentProjection(
                        event.appointmentId(),
                        event.patientId(),
                        event.doctorId(),
                        "CONFIRMED"
                ));

        projectionRepository.save(projection);
    }

    @Transactional
    @KafkaHandler
    public void handleCancelled(AppointmentCancelledEvent event) {
        log.info("Projecting AppointmentCancelledEvent for appointmentId: {}", event.appointmentId());

        projectionRepository.findById(event.appointmentId())
                .ifPresentOrElse(
                        existing -> {
                            existing.updateStatus("CANCELLED");
                            projectionRepository.save(existing);
                        },
                        () -> {
                            AppointmentProjection projection = new AppointmentProjection(
                                    event.appointmentId(),
                                    event.patientId(),
                                    event.doctorId(),
                                    "CANCELLED"
                            );
                            projectionRepository.save(projection);
                        }
                );
    }

    @KafkaHandler(isDefault = true)
    public void handleUnknown(Object object) {
        log.debug("Ignoring unrecognized message on hospital.appointment.saga.events: {}", object);
    }
}
