package com.store.ecommerce.service.impl;

import com.store.ecommerce.common.Constants;
import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.RegisterRequest;
import com.store.ecommerce.dto.request.UserRequest;
import com.store.ecommerce.entity.Role;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.UserMapper;
import com.store.ecommerce.repository.CountryRepository;
import com.store.ecommerce.repository.RoleRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.SettingService;
import com.store.ecommerce.service.UserService;
import com.store.ecommerce.util.MailUtil;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom; // ✅ Import SecureRandom
import java.util.*;
import java.util.stream.Collectors;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final SettingService settingService;
    private final CountryRepository countryRepository;
    private final AWSS3Service awsS3Service;
    private final Constants constants;

    @Override
    public UserDTO signup(RegisterRequest registerUserDto) throws ConflictException, IllegalArgumentException {
        if (userRepository.findByEmail(registerUserDto.getEmail()).isPresent()) {
            throw new ConflictException("Email is existing!");
        }

        if (countryRepository.findByNameIgnoreCase(registerUserDto.getCountry()).isEmpty()) {
            throw new NotFoundException("Could not find any country " + registerUserDto.getCountry());
        }

        User user = registerUserDto.toUser();

        Role roleCustomer = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Role ROLE_CUSTOMER is not configured in the system"));

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.addRole(roleCustomer);
        user.setEnabled(false);
        user.setCreatedTime(new Date());

        String randomCode = randomAlphanumericString(64);
        user.setVerificationCode(randomCode);
        user.setAuthenticationType(AuthenticationType.DATABASE);

        User savedUser = userRepository.save(user);
        sendVerificationEmail(user);

        return userMapper.toUserDTO(savedUser);
    }

    @Override
    public boolean verify(String verificationCode) {
        User customer = userRepository.findByVerificationCode(verificationCode);
        if (customer == null || customer.isEnabled()) {
            return false;
        } else {
            userRepository.enableUserByID(customer.getId());
            return true;
        }
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<UserDTO> listUserDTO = userRepository.findAll().stream().map(userMapper::toUserDTO).collect(Collectors.toList());
        listUserDTO.forEach(this::setImagePathForUser);
        return listUserDTO;
    }

    @Override
    public List<UserDTO> getAllUsers(String keyword, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        List<UserDTO> listUserDTO = userRepository.searchByKeyword(keyword, sort)
                .stream().map(userMapper::toUserDTO).collect(Collectors.toList());
        listUserDTO.forEach(this::setImagePathForUser);

        return listUserDTO;
    }

    private UserDTO setImagePathForUser(UserDTO userDTO) {
        userDTO.setImagePath(awsS3Service.getImagePath("user-photos/" + userDTO.getId(), userDTO.getPhoto()));
        return userDTO;
    }

    @Override
    public Page<UserDTO> getUsersByPage(PagingAndSortingHelper helper) {
        Sort sort = Sort.by(helper.getSortField());
        sort = helper.getSortDir().equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();
        Pageable pageable = PageRequest.of(helper.getPageNum() - 1, helper.getPageSize(), sort);

        Page<User> pageUsers;
        if (helper.getKeyword() != null && !helper.getKeyword().isBlank()) {
            pageUsers = userRepository.searchByKeyword(helper.getKeyword(), pageable);
        } else {
            pageUsers = userRepository.findAll(pageable);
        }

        return pageUsers.map(user -> {
            UserDTO dto = userMapper.toUserDTO(user);
            return setImagePathForUser(dto);
        });
    }

    @Override
    public UserDTO getUserById(Long id) throws NotFoundException {
        User user = userRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Could not find any user with ID: " + id));

        UserDTO userDTO = userMapper.toUserDTO(user);
        setImagePathForUser(userDTO);
        return userDTO;
    }

    @Override
    public UserDTO saveUser(UserRequest userDTO, MultipartFile photo) throws ConflictException, IOException {
        if (countryRepository.findByNameIgnoreCase(userDTO.getCountry()).isEmpty()) {
            throw new NotFoundException("Could not find any country " + userDTO.getCountry());
        }

        boolean isUpdatingUser = (userDTO.getId() != null);

        if (isUpdatingUser) {
            return updateAccountDetails(userDTO.getEmail(), userDTO, photo);
        } else {
            if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
                throw new ConflictException("Email is existing!");
            }

            handlePhoto(userDTO, photo);

            User user = userDTO.toUser();
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setCreatedTime(new Date());

            UserDTO savedUser = userMapper.toUserDTO(userRepository.save(user));
            uploadPhoto(savedUser, photo);

            return setImagePathForUser(savedUser);
        }
    }

    @Override
    public void delete(Long id) throws NotFoundException {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Could not find any user with ID " + id);
        }

        userRepository.deleteById(id);
        String userDir = "user-photos/" + id;
        awsS3Service.removeFolder(userDir + "/");
    }

    @Override
    public boolean isEmailUnique(Long id, String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isEmpty() || Objects.equals(user.get().getId(), id);
    }

    @Override
    public void updateUserEnabledStatus(Long id, boolean enabled) throws NotFoundException {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Could not find any user with ID " + id);
        }
        userRepository.updateUserEnabledStatus(id, enabled);
    }

    @Override
    public UserDTO getUserByEmail(String email) throws NotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        UserDTO userDTO = userMapper.toUserDTO(user);
        setImagePathForUser(userDTO);
        return userDTO;
    }

    @Override
    public UserDTO updateAccountDetails(String email, UserRequest userDTO, MultipartFile photo)
            throws NotFoundException, IOException {

        User existingUser = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        existingUser.setFirstName(userDTO.getFirstName());
        existingUser.setLastName(userDTO.getLastName());
        existingUser.setBirthOfDate(userDTO.getBirthOfDate());
        existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        existingUser.setAddressLine1(userDTO.getAddressLine1());
        existingUser.setAddressLine2(userDTO.getAddressLine2());
        existingUser.setCity(userDTO.getCity());
        existingUser.setState(userDTO.getState());
        existingUser.setPostalCode(userDTO.getPostalCode());
        existingUser.setCountry(userDTO.getCountry());

        // Handle password
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Handle photo
        if (!isFileNullOrEmpty(photo)) {
            String fileName = UUID.randomUUID() + "_" + Objects.requireNonNull(photo.getOriginalFilename());
            existingUser.setPhoto(fileName);
        }

        UserDTO savedUser = userMapper.toUserDTO(userRepository.save(existingUser));

        // Upload photo to S3 if a new one was provided
        if (!isFileNullOrEmpty(photo)) {
            uploadPhoto(savedUser, photo);
        }

        return setImagePathForUser(savedUser);
    }

    @Override
    public void updateAuthenticationType(UserDTO user, AuthenticationType authenticationType) {
        userRepository.updateAuthenticationType(user.getId(), authenticationType.name());
    }

    @Override
    public void addNewCustomerUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType) {
        User customer = new User();

        Role roleCustomer = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Role ROLE_CUSTOMER is not configured"));
        customer.addRole(roleCustomer);

        customer.setEmail(email);
        setName(customer, name);
        customer.setBirthOfDate(new Date());
        customer.setEnabled(true);
        customer.setCreatedTime(new Date());
        customer.setAuthenticationType(authenticationType);
        customer.setPassword("");
        customer.setAddressLine1("");
        customer.setCity("");
        customer.setState("");
        customer.setPhoneNumber("");
        customer.setPostalCode("");

        String countryName = countryRepository.findByCode(countryCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid country code: " + countryCode))
                .getName();
        customer.setCountry(countryName);

        userRepository.save(customer);
    }

    @Override
    public String updateResetPasswordToken(String email) throws NotFoundException {
        User savedUser = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        String token = randomAlphanumericString(30);
        savedUser.setResetPasswordToken(token);
        userRepository.save(savedUser);
        return token;
    }

    @Override
    public void updatePassword(String token, String password) throws NotFoundException {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new NotFoundException("No user found: invalid token!"));

        user.setPassword(passwordEncoder.encode(password));
        user.setResetPasswordToken(null);
        userRepository.save(user);
    }

    // ====== HANDLER ======
    private void handlePhoto(UserRequest userDTO, MultipartFile photo) {
        if (!isFileNullOrEmpty(photo)) {
            String originalName = photo.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalName;
            userDTO.setPhoto(fileName);
        } else {
            userDTO.setPhoto(null);
        }
    }

    private void uploadPhoto(UserDTO savedUser, MultipartFile photo) throws IOException {
        if (!isFileNullOrEmpty(photo)) {
            String uploadDir = "user-photos/" + savedUser.getId();
            awsS3Service.removeFolder(uploadDir + "/");
            awsS3Service.uploadFile(uploadDir, savedUser.getPhoto(),
                    photo.getInputStream(), photo.getSize(), photo.getContentType());
        }
    }

    private void sendVerificationEmail(User user) {
        SettingBag emailSettings = settingService.getEmailSettings();

        String toAddress = user.getEmail();
        String subject = emailSettings.getValue("CUSTOMER_VERIFY_SUBJECT");
        String content = emailSettings.getValue("CUSTOMER_VERIFY_CONTENT");

        content = content.replace("[[name]]", user.getFullName());
        String verifyURL = constants.getFeUrl() + "/verify?code=" + user.getVerificationCode();
        content = content.replace("[[URL]]", verifyURL);

        MailUtil.sendEmail(emailSettings, toAddress, subject, content);
    }

    private void setName(User user, String name) {
        String[] nameArray = name.split(" ");
        if (nameArray.length < 2) {
            user.setFirstName(name);
            user.setLastName("");
        } else {
            String firstName = nameArray[0];
            user.setFirstName(firstName);
            String lastName = name.substring(firstName.length() + 1);
            user.setLastName(lastName);
        }
    }

    private String randomAlphanumericString(int length) {
        String alphanumericCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuv";
        SecureRandom random = new SecureRandom();
        StringBuilder randomString = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(alphanumericCharacters.length());
            randomString.append(alphanumericCharacters.charAt(randomIndex));
        }

        return randomString.toString();
    }
}