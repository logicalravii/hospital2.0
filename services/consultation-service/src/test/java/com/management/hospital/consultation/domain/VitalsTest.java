package com.management.hospital.consultation.domain;

import com.management.hospital.common.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VitalsTest {

    @Test
    @DisplayName("Should create Vitals when all physiological boundaries are satisfied")
    void shouldCreateValidVitals() {
        Vitals vitals = new Vitals(36.8, 120, 80, 72, 16, 98.5);

        assertThat(vitals.temperatureCelsius()).isEqualTo(36.8);
        assertThat(vitals.systolicBp()).isEqualTo(120);
        assertThat(vitals.diastolicBp()).isEqualTo(80);
        assertThat(vitals.pulseRateBpm()).isEqualTo(72);
        assertThat(vitals.respiratoryRate()).isEqualTo(16);
        assertThat(vitals.oxygenSaturationPct()).isEqualTo(98.5);
    }

    @Test
    @DisplayName("Should allow partial vitals when some measurements are absent")
    void shouldAllowPartialVitals() {
        Vitals vitals = new Vitals(37.0, null, null, 80, null, null);

        assertThat(vitals.temperatureCelsius()).isEqualTo(37.0);
        assertThat(vitals.pulseRateBpm()).isEqualTo(80);
        assertThat(vitals.systolicBp()).isNull();
    }

    @Test
    @DisplayName("Should reject temperature below 30.0°C")
    void shouldRejectLowTemperature() {
        assertThatThrownBy(() -> new Vitals(29.9, 120, 80, 70, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Temperature 29.9°C is outside valid physiological boundary");
    }

    @Test
    @DisplayName("Should reject temperature above 45.0°C")
    void shouldRejectHighTemperature() {
        assertThatThrownBy(() -> new Vitals(45.5, 120, 80, 70, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Temperature 45.5°C is outside valid physiological boundary");
    }

    @Test
    @DisplayName("Should reject systolic BP below 50 mmHg")
    void shouldRejectLowSystolicBp() {
        assertThatThrownBy(() -> new Vitals(37.0, 45, 40, 70, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Systolic BP 45 mmHg is outside valid physiological boundary");
    }

    @Test
    @DisplayName("Should reject systolic BP above 260 mmHg")
    void shouldRejectHighSystolicBp() {
        assertThatThrownBy(() -> new Vitals(37.0, 270, 90, 70, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Systolic BP 270 mmHg is outside valid physiological boundary");
    }

    @Test
    @DisplayName("Should reject diastolic BP below 30 mmHg")
    void shouldRejectLowDiastolicBp() {
        assertThatThrownBy(() -> new Vitals(37.0, 110, 25, 70, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Diastolic BP 25 mmHg is outside valid physiological boundary");
    }

    @Test
    @DisplayName("Should reject diastolic BP above 180 mmHg")
    void shouldRejectHighDiastolicBp() {
        assertThatThrownBy(() -> new Vitals(37.0, 200, 185, 70, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Diastolic BP 185 mmHg is outside valid physiological boundary");
    }

    @Test
    @DisplayName("Should reject when systolic BP is less than or equal to diastolic BP")
    void shouldRejectInvertedBloodPressure() {
        assertThatThrownBy(() -> new Vitals(37.0, 80, 80, 70, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Systolic BP (80 mmHg) must be greater than diastolic BP (80 mmHg)");

        assertThatThrownBy(() -> new Vitals(37.0, 70, 80, 70, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Systolic BP (70 mmHg) must be greater than diastolic BP (80 mmHg)");
    }

    @Test
    @DisplayName("Should reject pulse rate outside [30 - 250 bpm]")
    void shouldRejectInvalidPulseRate() {
        assertThatThrownBy(() -> new Vitals(37.0, 120, 80, 25, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Pulse rate 25 bpm is outside valid physiological boundary");

        assertThatThrownBy(() -> new Vitals(37.0, 120, 80, 260, 16, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Pulse rate 260 bpm is outside valid physiological boundary");
    }

    @Test
    @DisplayName("Should reject respiratory rate outside [6 - 60 breaths/min]")
    void shouldRejectInvalidRespiratoryRate() {
        assertThatThrownBy(() -> new Vitals(37.0, 120, 80, 72, 4, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Respiratory rate 4 is outside valid physiological boundary");

        assertThatThrownBy(() -> new Vitals(37.0, 120, 80, 72, 65, 98.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Respiratory rate 65 is outside valid physiological boundary");
    }

    @Test
    @DisplayName("Should reject oxygen saturation outside [50.0% - 100.0%]")
    void shouldRejectInvalidOxygenSaturation() {
        assertThatThrownBy(() -> new Vitals(37.0, 120, 80, 72, 16, 45.0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Oxygen saturation 45.0% is outside valid physiological boundary");

        assertThatThrownBy(() -> new Vitals(37.0, 120, 80, 72, 16, 101.5))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Oxygen saturation 101.5% is outside valid physiological boundary");
    }
}
