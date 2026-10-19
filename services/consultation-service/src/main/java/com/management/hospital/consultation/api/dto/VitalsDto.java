package com.management.hospital.consultation.api.dto;

import com.management.hospital.consultation.domain.Vitals;

public record VitalsDto(
    Double temperatureCelsius,
    Integer systolicBp,
    Integer diastolicBp,
    Integer pulseRateBpm,
    Integer respiratoryRate,
    Double oxygenSaturationPct
) {
    public Vitals toDomain() {
        return new Vitals(
            temperatureCelsius,
            systolicBp,
            diastolicBp,
            pulseRateBpm,
            respiratoryRate,
            oxygenSaturationPct
        );
    }

    public static VitalsDto fromDomain(Vitals vitals) {
        if (vitals == null) {
            return null;
        }
        return new VitalsDto(
            vitals.temperatureCelsius(),
            vitals.systolicBp(),
            vitals.diastolicBp(),
            vitals.pulseRateBpm(),
            vitals.respiratoryRate(),
            vitals.oxygenSaturationPct()
        );
    }
}
