package com.store.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.store.ecommerce.config.AWSS3Config;
import com.store.ecommerce.entity.Country;
import com.store.ecommerce.entity.Role;
import com.store.ecommerce.service.AWSS3Service;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDTO {
    Long id;
    String email;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    Date birthOfDate;
    List<Role> roles = new ArrayList<>();
    String fullName;
    String imagePath;

    @JsonIgnore
    public List<String> getListRoles() {
        List<String> listRoles = new ArrayList<>();

        for (Role role : roles) {
            listRoles.add(role.getName().toUpperCase());
        }
        return listRoles;
    }
}
