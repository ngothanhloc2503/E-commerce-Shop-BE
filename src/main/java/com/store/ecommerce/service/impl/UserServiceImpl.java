package com.store.ecommerce.service.impl;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.RegisterDTO;
import com.store.ecommerce.dto.request.UserRequestDTO;
import com.store.ecommerce.entity.Role;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.AuthenticationType;
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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SettingService settingService;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private AWSS3Service awsS3Service;

    @Override
    public UserDTO signup(RegisterDTO registerUserDto) throws Exception {
        if(userRepository.findByEmail(registerUserDto.getEmail()).isPresent()) {
            throw new ConflictException("Email is existing!");
        };
        if (countryRepository.findByNameIgnoreCase(registerUserDto.getCountry()).isEmpty()) {
            throw new Exception("Could not find any country " + registerUserDto.getCountry());
        }

        User user = registerUserDto.toUser();

        Role roleCustomer = roleRepository.findByName("ROLE_CUSTOMER");
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

      void sendVerificationEmail(User user)
            throws MessagingException, UnsupportedEncodingException {
        SettingBag emailSettings = settingService.getEmailSettings();
        JavaMailSenderImpl mailSender = MailUtil.prepareMailSender(emailSettings);

        String toAddress = user.getEmail();
        String subject = emailSettings.getValue("CUSTOMER_VERIFY_SUBJECT");
        String content = emailSettings.getValue("CUSTOMER_VERIFY_CONTENT");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(emailSettings.getValue("MAIL_FROM"), emailSettings. getValue("MAIL_SENDER_NAME"));
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", user.getFullName());
        String verifyURL = "http://localhost:4200/verify?code=" + user.getVerificationCode();
        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        mailSender.send(message);
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

        List<UserDTO> listUserDTO = userRepository.findAll(keyword, sort)
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
        Page<User> pageUsers = (Page<User>) helper.getPageEntities(userRepository);

        Page<UserDTO> pageUserDTO = pageUsers.map(userMapper::toUserDTO);
        pageUserDTO.map(this::setImagePathForUser);
        return pageUserDTO;
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
    public UserDTO saveUser(UserRequestDTO userDTO) throws Exception {
        if (countryRepository.findByNameIgnoreCase(userDTO.getCountry()).isEmpty()) {
            throw new Exception("Could not find any country " + userDTO.getCountry());
        }
        User user = userDTO.toUser();
        boolean isUpdatingUser = (user.getId() != null);

        if (isUpdatingUser) {
            User existingUser = userRepository.findById(user.getId()).orElseThrow();
            user.setCreatedTime(existingUser.getCreatedTime());
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword(existingUser.getPassword());
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            if (user.getPhoto() == null) {
                user.setPhoto(existingUser.getPhoto());
            }
        } else {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new Exception("Email is existing!");
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setCreatedTime(new Date());
        }

        return userMapper.toUserDTO(userRepository.save(user));
    }

    @Override
    public void delete(Long id) throws NotFoundException {
        if (userRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Could not find any user with ID " + id);
        }

        userRepository.deleteById(id);
    }

    @Override
    public boolean isEmailUnique(Long id, String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isEmpty() || Objects.equals(user.get().getId(), id);
    }

    @Override
    public void updateUserEnabledStatus(Long id, boolean enabled) throws NotFoundException {
        if (userRepository.findById(id).isEmpty()) {
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
    public UserDTO updateAccountDetails(String email, UserRequestDTO userDTO) throws NotFoundException {
        User existingUser = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        User user = userDTO.toUser();
        user.setCreatedTime(existingUser.getCreatedTime());
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(existingUser.getPassword());
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        if (user.getPhoto() == null) {
            user.setPhoto(existingUser.getPhoto());
        }
        user.setRoles(existingUser.getRoles());
        user.setEnabled(existingUser.isEnabled());

        return setImagePathForUser(userMapper.toUserDTO(userRepository.save(user)));
    }

    @Override
    public void updateAuthenticationType(UserDTO user, AuthenticationType authenticationType) {
        userRepository.updateAuthenticationType(user.getId(), authenticationType);
    }

    @Override
    public void addNewCustomerUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType) {
        User customer = new User();

        Role roleCustomer = roleRepository.findByName("ROLE_CUSTOMER");
        customer.addRole(roleCustomer);

        customer.setEmail(email);
        setName(customer, name); //set firstName and lastName
        customer.setLastName("");
        customer.setEnabled(true);
        customer.setCreatedTime(new Date());
        customer.setAuthenticationType(authenticationType);
        customer.setPassword("");
        customer.setAddressLine1("");
        customer.setCity("");
        customer.setState("");
        customer.setPhoneNumber("");
        customer.setPostalCode("");
        customer.setCountry(countryRepository.findByCode(countryCode).get().getName());

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
        User user = userRepository.findByResetPasswordToken(token);
        if (user != null) {
            user.setPassword(password);
            user.setPassword(passwordEncoder.encode(password));
            user.setResetPasswordToken(null);
            userRepository.save(user);
        } else {
            throw new NotFoundException("No user found: invalid token!");
        }
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

        StringBuffer randomString = new StringBuffer(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(alphanumericCharacters.length());
            char randomChar = alphanumericCharacters.charAt(randomIndex);
            randomString.append(randomChar);
        }

        return randomString.toString();
    }
}
