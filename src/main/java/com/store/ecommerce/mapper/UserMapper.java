package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.RegisterRequest;
import com.store.ecommerce.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface UserMapper {

    @Mapping(source = "birthOfDate", target = "birthOfDate", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "fullName", ignore = true)
    UserDTO toUserDTO(User user);

    @Mapping(source = "birthOfDate", target = "birthOfDate", dateFormat = "yyyy-MM-dd")
    User toUser(UserDTO userDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdTime", ignore = true)
    void updateUserFromRequest(RegisterRequest request, @MappingTarget User user);

    List<UserDTO> toUserDTOList(List<User> users);
}
