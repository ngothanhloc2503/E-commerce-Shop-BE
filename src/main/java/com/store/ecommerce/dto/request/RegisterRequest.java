package com.store.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.store.ecommerce.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    String email;
    String password;
    String firstName;
    String lastName;
    String phoneNumber;
    String addressLine1;
    String addressLine2;
    String city;
    String state;
    String country;
    String postalCode;
    Date birthOfDate;

    @JsonIgnore
    public User toUser() {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setBirthOfDate(birthOfDate);
        user.setAddressLine1(addressLine1);
        user.setAddressLine2(addressLine2);
        user.setCity(city);
        user.setState(state);
        user.setCountry(country);
        user.setPostalCode(postalCode);
        return user;
    }
}
