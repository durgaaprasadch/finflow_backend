package com.finflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public class PersonalDetailsRequest {

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]{2,100}$", message = "Full name must contain only alphabets and spaces")
    private String fullName;
    
    private java.time.LocalDate dob;
    private String gender;
    private String maritalStatus;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}\\d{4}[A-Z]$", message = "Invalid PAN card format (e.g. ABCDE1234F)")
    private String panNumber;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^[2-9]\\d{11}$", message = "Invalid Aadhaar number (must be 12 digits and not start with 0 or 1)")
    private String aadhaarNumber;

    private AddressRequest address;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public AddressRequest getAddress() {
        return address;
    }

    public void setAddress(AddressRequest address) {
        this.address = address;
    }

    public static class AddressRequest {
        private String line1;
        private String city;
        private String state;
        private String pincode;

        public String getLine1() {
            return line1;
        }

        public void setLine1(String line1) {
            this.line1 = line1;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPincode() {
            return pincode;
        }

        public void setPincode(String pincode) {
            this.pincode = pincode;
        }
    }
}
