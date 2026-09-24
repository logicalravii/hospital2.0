package com.management.hospital.appointment.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.management.hospital.appointment.api.dto.CancelAppointmentRequest;
import com.management.hospital.appointment.application.AppointmentService;
import com.management.hospital.common.event.DoctorSlotAvailableEvent;
import com.management.hospital.common.event.DoctorSlotOverlap;

@Component
public class AppointmentSagaGroup {

    private final AppointmentService appointmentService;
    private static final Logger log = LoggerFactory.getLogger(AppointmentSagaGroup.class);

    public AppointmentSagaGroup(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @KafkaListener(topics = DoctorSlotOverlap.TOPIC, groupId = "appointment-service-group")
    public void handleDoctorSlotOverlap(DoctorSlotOverlap event) {
        log.info("Received DoctorSlotOverlap for appointmentId: {}, doctorId: {}",
                event.appointmentId(), event.doctorId());

        if (event.appointmentId() != null) {
            try {
                appointmentService.cancelAppointment(event.appointmentId(),
                        new CancelAppointmentRequest("Appointment cancelled due to doctor slot overlap"));
                log.info("Successfully cancelled appointment {} via doctor slot overlap event", event.appointmentId());
            } catch (Exception ex) {
                log.warn("Failed to auto-cancel appointment {} on doctor slot overlap event: {}",
                        event.appointmentId(), ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = DoctorSlotAvailableEvent.TOPIC, groupId = "appointment-service-group")
    public void handleDoctorSlotAvailable(DoctorSlotAvailableEvent event) {
        log.info("Received DoctorSlotAvailable for appointmentId: {}, doctorId: {}",
                event.appointmentId(), event.doctorId());

        if (event.appointmentId() != null) {
            try {
                appointmentService.confirmAppointment(event.appointmentId());
                log.info("Successfully confirmed appointment {} via doctor slot available event",
                        event.appointmentId());
            } catch (Exception ex) {
                log.warn("Failed to auto-confirm appointment {} on doctor slot available event: {}",
                        event.appointmentId(), ex.getMessage());
            }
        }
    }
}
