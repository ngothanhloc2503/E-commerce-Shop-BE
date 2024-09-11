package com.store.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.store.ecommerce.entity.Role;
import com.store.ecommerce.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRequestDTO {
    Long id;
    String email;
    String password;
    String firstName;
    String lastName;
    String photo;
    String phoneNumber;
    String addressLine1;
    String addressLine2;
    String city;
    String state;
    String country;
    String postalCode;
    boolean enabled;
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
