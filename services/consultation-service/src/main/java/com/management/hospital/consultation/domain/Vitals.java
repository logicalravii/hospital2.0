package com.management.hospital.consultation.domain;

import com.management.hospital.common.error.DomainException;

import java.io.Serializable;

public record Vitals(
    Double temperatureCelsius,
    Integer systolicBp,
    Integer diastolicBp,
    Integer pulseRateBpm,
    Integer respiratoryRate,
    Double oxygenSaturationPct
) implements Serializable {

    public Vitals {
        validatePhysiologicalBoundaries(
            temperatureCelsius, systolicBp, diastolicBp, pulseRateBpm, respiratoryRate, oxygenSaturationPct
        );
    }

    private static void validatePhysiologicalBoundaries(
            Double temperatureCelsius,
            Integer systolicBp,
            Integer diastolicBp,
            Integer pulseRateBpm,
            Integer respiratoryRate,
            Double oxygenSaturationPct
    ) {
        if (temperatureCelsius != null && (temperatureCelsius < 30.0 || temperatureCelsius > 45.0)) {
            throw new DomainException(
                "Temperature %.1f°C is outside valid physiological boundary [30.0°C - 45.0°C]".formatted(temperatureCelsius));
        }

        if (systolicBp != null && (systolicBp < 50 || systolicBp > 260)) {
            throw new DomainException(
                "Systolic BP %d mmHg is outside valid physiological boundary [50 - 260 mmHg]".formatted(systolicBp));
        }

        if (diastolicBp != null && (diastolicBp < 30 || diastolicBp > 180)) {
            throw new DomainException(
                "Diastolic BP %d mmHg is outside valid physiological boundary [30 - 180 mmHg]".formatted(diastolicBp));
        }

        if (systolicBp != null && diastolicBp != null && systolicBp <= diastolicBp) {
            throw new DomainException(
                "Systolic BP (%d mmHg) must be greater than diastolic BP (%d mmHg)".formatted(systolicBp, diastolicBp));
        }

        if (pulseRateBpm != null && (pulseRateBpm < 30 || pulseRateBpm > 250)) {
            throw new DomainException(
                "Pulse rate %d bpm is outside valid physiological boundary [30 - 250 bpm]".formatted(pulseRateBpm));
        }

        if (respiratoryRate != null && (respiratoryRate < 6 || respiratoryRate > 60)) {
            throw new DomainException(
                "Respiratory rate %d is outside valid physiological boundary [6 - 60 breaths/min]".formatted(respiratoryRate));
        }

        if (oxygenSaturationPct != null && (oxygenSaturationPct < 50.0 || oxygenSaturationPct > 100.0)) {
            throw new DomainException(
                "Oxygen saturation %.1f%% is outside valid physiological boundary [50.0%% - 100.0%%]".formatted(oxygenSaturationPct));
        }
    }
}
