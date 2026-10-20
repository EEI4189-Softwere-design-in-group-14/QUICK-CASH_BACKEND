package com.quickcash.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guarantors")
public class Guarantor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guarantorId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "loan_id",
            nullable = false,
            unique = true
    )
    private Loan loan;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, length = 12)
    private String nic;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false)
    private String address;

    private String occupation;

    public Guarantor() {
    }

    public Long getGuarantorId() {
        return guarantorId;
    }

    public void setGuarantorId(Long guarantorId) {
        this.guarantorId = guarantorId;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }
}