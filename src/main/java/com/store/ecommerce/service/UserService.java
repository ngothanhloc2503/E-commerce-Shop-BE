package com.store.ecommerce.service;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.RegisterDTO;
import com.store.ecommerce.dto.request.UserRequestDTO;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    UserDTO signup(RegisterDTO registerUserDto) throws Exception;

    boolean verify(String verificationCode);

    List<UserDTO> getAllUsers();

    List<UserDTO> getAllUsers(String keyword, String sortField, String sortDir);

    UserDTO getUserById(Long id) throws NotFoundException;

    UserDTO getUserByEmail(String email) throws NotFoundException;

    UserDTO saveUser(UserRequestDTO user) throws Exception;

    void delete(Long id) throws NotFoundException;

    boolean isEmailUnique(Long id, String email);

    void updateUserEnabledStatus(Long id, boolean enabled) throws NotFoundException;

    Page<UserDTO> getUsersByPage(PagingAndSortingHelper helper);

    UserDTO updateAccountDetails(String email, UserRequestDTO userDTO) throws NotFoundException;

    void updateAuthenticationType(UserDTO user, AuthenticationType authenticationType);

    void addNewCustomerUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType);

    String updateResetPasswordToken(String email) throws NotFoundException;

    void updatePassword(String token, String password) throws NotFoundException;
}
