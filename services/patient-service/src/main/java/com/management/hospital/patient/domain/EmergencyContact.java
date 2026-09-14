package com.management.hospital.patient.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EmergencyContact {

    @Column(name = "emergency_contact_name")
    private String name;

    @Column(name = "emergency_contact_phone")
    private String phone;

    @Column(name = "emergency_contact_relation")
    private String relationship;

    protected EmergencyContact() {
        // JPA required
    }

    public EmergencyContact(String name, String phone, String relationship) {
        this.name = name != null ? name.trim() : null;
        this.phone = phone != null ? phone.trim() : null;
        this.relationship = relationship != null ? relationship.trim() : null;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getRelationship() {
        return relationship;
    }
}
