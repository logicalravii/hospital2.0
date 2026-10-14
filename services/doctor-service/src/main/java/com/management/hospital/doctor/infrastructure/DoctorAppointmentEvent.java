package com.management.hospital.doctor.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.management.hospital.common.event.AppointmentInitiatedEvent;
import com.management.hospital.common.event.DoctorSlotAvailableEvent;
import com.management.hospital.common.event.DoctorSlotOverlap;
import com.management.hospital.doctor.api.dto.AvailableSlotsResponse;
import com.management.hospital.doctor.application.DoctorService;

@Component
public class DoctorAppointmentEvent {
        private static final Logger log = LoggerFactory.getLogger(DoctorAppointmentEvent.class);
        private final DoctorService doctorService;
        private final DoctorScheduleRepository doctorScheduleRepository;
        private final DoctorEventPublisher doctorEventPublisher;

        public DoctorAppointmentEvent(DoctorService doctorService, DoctorScheduleRepository doctorScheduleRepository,
                        DoctorEventPublisher doctorEventPublisher) {
                this.doctorService = doctorService;
                this.doctorScheduleRepository = doctorScheduleRepository;
                this.doctorEventPublisher = doctorEventPublisher;
        }

        @KafkaListener(topics = AppointmentInitiatedEvent.TOPIC)
        public void handleAppointmentEvent(AppointmentInitiatedEvent event) {
                log.info("Received AppointmentInitiatedEvent for appointmentId: {}, doctorId: {}, patientId: {}",
                                event.appointmentId(), event.doctorId(), event.patientId());

                AvailableSlotsResponse response = doctorService.getAvailableSlotsForDate(event.doctorId(),
                                event.appointmentDate());

                boolean isDoctorScheduleAvailable = doctorScheduleRepository.isDoctorHasActiveSchedule(event.doctorId(),
                                event.appointmentDate().getDayOfWeek(), event.startTime(), event.endTime());

                boolean slotAvailable = response.slots().stream()
                                .anyMatch(p -> p.startTime().equals(event.startTime())
                                                && p.endTime().equals(event.endTime()));
                String message = slotAvailable && isDoctorScheduleAvailable
                                ? "Slot found for appointmentId: {}, doctorId: {}"
                                : "No slot found for appointmentId: {}, doctorId: {}";
                log.info(message, event.appointmentId(), event.doctorId());

                if (!slotAvailable || !isDoctorScheduleAvailable) {
                        log.info("Appointment {} is overlapping or slot unavailable for doctor {}", event.appointmentId(), event.doctorId());
                        doctorEventPublisher
                                        .publishAppointmentOverlaps(
                                                        new DoctorSlotOverlap(event.appointmentId(), event.doctorId()));
                } else {
                        log.info("Appointment {} slot is available for doctor {}", event.appointmentId(), event.doctorId());
                        doctorEventPublisher
                                        .publishDoctorSlotAvailable(new DoctorSlotAvailableEvent(event.appointmentId(),
                                                        event.doctorId()));
                }
        }
}
