package com.store.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.store.ecommerce.entity.Role;
import com.store.ecommerce.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRequest {
    Long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"
    )
    String password;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name can only contain letters and spaces")
    String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name can only contain letters and spaces")
    String lastName;

    String photo;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    String phoneNumber;

    String addressLine1;
    String addressLine2;

    @Size(max = 50, message = "City name cannot exceed 50 characters")
    String city;

    @Size(max = 50, message = "State name cannot exceed 50 characters")
    String state;

    @Size(max = 50, message = "Country name cannot exceed 50 characters")
    String country;

    @Pattern(regexp = "^[0-9]{5,10}$", message = "Invalid postal code format")
    String postalCode;

    boolean enabled;

    @Past(message = "Date of birth must be in the past")
    Date birthOfDate;

    List<Role> roles = new ArrayList<>();

    @JsonIgnore
    public User toUser() {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoto(photo);
        user.setPhoneNumber(phoneNumber);
        user.setEnabled(enabled);
        user.setBirthOfDate(birthOfDate);
        user.setAddressLine1(addressLine1);
        user.setAddressLine2(addressLine2);
        user.setCity(city);
        user.setState(state);
        user.setCountry(country);
        user.setPostalCode(postalCode);
        for (Role role : roles) {
            user.addRole(role);
        }
        return user;
    }
}
