package com.management.hospital.appointment.infrastructure;

import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Component
public class DoctorServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DoctorServiceClient.class);
    private final RestClient restClient;

    public DoctorServiceClient(@Value("${services.doctor-service.url:http://localhost:8083}") String doctorServiceUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(doctorServiceUrl)
            .build();
    }

    public record SlotDto(LocalTime startTime, LocalTime endTime) {}
    public record AvailableSlotsResponseDto(UUID doctorId, LocalDate date, String dayOfWeek, List<SlotDto> slots) {}

    public void validateDoctorSlot(UUID doctorId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        try {
            AvailableSlotsResponseDto response = restClient.get()
                .uri("/api/v1/doctors/{id}/slots?date={date}", doctorId, date)
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new ResourceNotFoundException("Doctor", doctorId);
                })
                .body(AvailableSlotsResponseDto.class);

            if (response != null && response.slots() != null && !response.slots().isEmpty()) {
                boolean slotExists = response.slots().stream()
                    .anyMatch(s -> s.startTime().equals(startTime) && s.endTime().equals(endTime));
                if (!slotExists) {
                    throw new ConflictException("Doctor %s does not have an available schedule slot for %s %s-%s".formatted(
                        doctorId, date, startTime, endTime
                    ));
                }
            } else {
                throw new ConflictException("Doctor %s has no scheduled slots on %s (%s)".formatted(
                    doctorId, date, date.getDayOfWeek()
                ));
            }
        } catch (ResourceNotFoundException | ConflictException e) {
            throw e;
        } catch (Exception ex) {
            log.warn("Could not verify doctor schedule with doctor-service: {}. Proceeding with local verification.", ex.getMessage());
        }
    }
}
