package com.management.hospital.doctor.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.management.hospital.common.event.AppointmentInitiatedEvent;
import com.management.hospital.common.event.DoctorSlotAvailableEvent;
import com.management.hospital.common.event.DoctorSlotOverlap;
import com.management.hospital.doctor.api.dto.AvailableSlotsResponse;
import com.management.hospital.doctor.api.dto.CreateScheduleRequest;
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

                if (!slotAvailable) {
                        log.info("Appointment {} is overlapping with existing appointment", event.appointmentId());
                        doctorEventPublisher
                                        .publishAppointmentOverlaps(
                                                        new DoctorSlotOverlap(event.appointmentId(), event.doctorId()));
                } else {
                        log.info("Appointment {} is not overlapping with existing appointment", event.appointmentId());
                        int duration = (int) java.time.Duration.between(event.startTime(), event.endTime()).toMinutes();
                        doctorService.addSchedule(event.doctorId(), new CreateScheduleRequest(
                                        event.appointmentDate().getDayOfWeek(), event.startTime(), event.endTime(),
                                        duration, 1));
                        doctorEventPublisher
                                        .publishDoctorSlotAvailable(new DoctorSlotAvailableEvent(event.appointmentId(),
                                                        event.doctorId()));
                }

        }
}
