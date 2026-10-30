package com.quickcash.dto;

import com.quickcash.entity.Borrower;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BorrowerResponse {

    private Long borrowerId;
    private String borrowerCode;

    private Long userId;
    private String fullName;
    private String email;
    private String phone;

    private String nic;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
    private String occupation;
    private BigDecimal monthlyIncome;

    private String status;

    public BorrowerResponse(Borrower borrower) {

        this.borrowerId = borrower.getBorrowerId();
        this.borrowerCode = borrower.getBorrowerCode();

        this.userId = borrower.getUser().getUserId();
        this.fullName = borrower.getUser().getFullName();
        this.email = borrower.getUser().getEmail();
        this.phone = borrower.getUser().getPhone();

        this.nic = borrower.getNic();
        this.address = borrower.getAddress();
        this.dateOfBirth = borrower.getDateOfBirth();
        this.gender = borrower.getGender();
        this.occupation = borrower.getOccupation();
        this.monthlyIncome = borrower.getMonthlyIncome();

        this.status = borrower.getStatus().name();
    }

    public Long getBorrowerId() {
        return borrowerId;
    }

    public String getBorrowerCode() {
        return borrowerCode;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getNic() {
        return nic;
    }

    public String getAddress() {
        return address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getOccupation() {
        return occupation;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public String getStatus() {
        return status;
    }
}