package com.management.hospital.patient.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    @Column(name = "address_line1")
    private String line1;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    protected Address() {
        // JPA required
    }

    public Address(String line1, String city, String postalCode) {
        this.line1 = line1 != null ? line1.trim() : null;
        this.city = city != null ? city.trim() : null;
        this.postalCode = postalCode != null ? postalCode.trim() : null;
    }

    public String getLine1() {
        return line1;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }
}
