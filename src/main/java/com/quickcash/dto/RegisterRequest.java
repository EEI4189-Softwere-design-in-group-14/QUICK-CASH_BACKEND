package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RegisterRequest {

    @NotBlank(message = "Password is required")
    @Size(
            min = 8,
            max = 72,
            message = "Password must contain at least 8 characters"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;


    // PHONE VALIDATION
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(?:\\+94|0)\\d{9}$",
            message = "Enter a valid Sri Lankan phone number"
    )
    private String phone;


    // NIC VALIDATION
    @NotBlank(message = "NIC is required")
    @Pattern(
            regexp = "^(\\d{12}|\\d{9}[VvXx])$",
            message = "Enter a valid Sri Lankan NIC"
    )
    private String nic;


    @NotBlank(message = "Address is required")
    private String address;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String gender;

    private String occupation;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Monthly income cannot be negative"
    )
    private BigDecimal monthlyIncome;


    public RegisterRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}