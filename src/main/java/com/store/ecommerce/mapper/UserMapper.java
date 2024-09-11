package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "photo", target = "photo")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "addressLine1", target = "addressLine1")
    @Mapping(source = "addressLine2", target = "addressLine2")
    @Mapping(source = "city", target = "city")
    @Mapping(source = "state", target = "state")
    @Mapping(source = "country", target = "country")
    @Mapping(source = "postalCode", target = "postalCode")
    @Mapping(source = "enabled", target = "enabled")
    @Mapping(source = "birthOfDate", target = "birthOfDate", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "roles", target = "roles")
    @Mapping(source = "fullName", target = "fullName")
    UserDTO toUserDTO(User user);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "photo", target = "photo")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "addressLine1", target = "addressLine1")
    @Mapping(source = "addressLine2", target = "addressLine2")
    @Mapping(source = "city", target = "city")
    @Mapping(source = "state", target = "state")
    @Mapping(source = "country", target = "country")
    @Mapping(source = "postalCode", target = "postalCode")
    @Mapping(source = "enabled", target = "enabled")
    @Mapping(source = "birthOfDate", target = "birthOfDate", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "roles", target = "roles")
    User toUser(UserDTO userDTO);
}
