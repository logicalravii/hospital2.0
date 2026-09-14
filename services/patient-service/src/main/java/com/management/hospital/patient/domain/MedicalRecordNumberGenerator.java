package com.management.hospital.patient.domain;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MedicalRecordNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "MRN-%s-%06d".formatted(datePart, randomPart);
    }
}
