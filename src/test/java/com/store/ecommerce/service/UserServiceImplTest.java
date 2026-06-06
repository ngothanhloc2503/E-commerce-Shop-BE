package com.store.ecommerce.service;

import com.store.ecommerce.common.Constants;
import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.RegisterRequest;
import com.store.ecommerce.dto.request.UserRequest;
import com.store.ecommerce.entity.*;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.UserMapper;
import com.store.ecommerce.repository.CountryRepository;
import com.store.ecommerce.repository.RoleRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.impl.UserServiceImpl;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SettingService settingService;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private AWSS3Service awsS3Service;

    @Mock
    private PagingAndSortingHelper pagingAndSortingHelper;

    @Mock
    private Constants constants;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDTO testUserDTO;
    private RegisterRequest registerRequest;
    private Role customerRole;
    private Country testCountry;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(false);
        testUser.setVerificationCode("abc123");
        testUser.setAuthenticationType(AuthenticationType.DATABASE);
        testUser.setPhoto("photo.png");
        testUser.setCreatedTime(new Date());

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setFirstName("John");
        testUserDTO.setLastName("Doe");
        testUserDTO.setPhoto("photo.png");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setCountry("USA");

        customerRole = new Role();
        customerRole.setName("ROLE_CUSTOMER");

        testCountry = new Country();
        testCountry.setName("USA");
        testCountry.setCode("US");

        // Manually inject constants mock since @InjectMocks may not inject it
        // if UserServiceImpl uses field injection or @Value for constants
        ReflectionTestUtils.setField(userService, "constants", constants);
    }

    // ============================= signup =============================

    @Nested
    @DisplayName("signup - Đăng ký tài khoản mới")
    class SignupTests {

        @BeforeEach
        void setupSignup() {
            lenient().when(constants.getFeUrl()).thenReturn("http://localhost:3000");
        }

        @Test
        @DisplayName("Should register successfully when email is new and country exists")
        void shouldRegisterSuccessfully_WhenEmailIsNewAndCountryExists() throws ConflictException, IllegalArgumentException {
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(countryRepository.findByNameIgnoreCase(registerRequest.getCountry())).thenReturn(Optional.of(testCountry));
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(settingService.getEmailSettings()).thenReturn(createMockSettingBag());

            User savedUser = new User();
            savedUser.setId(1L);
            savedUser.setEmail(registerRequest.getEmail());
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);

            UserDTO result = userService.signup(registerRequest);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            verify(userRepository).save(any(User.class));
            verify(roleRepository).findByName("ROLE_CUSTOMER");
            verify(passwordEncoder).encode(anyString());
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        void shouldThrowConflictException_WhenEmailAlreadyExists() {
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.signup(registerRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Email is existing!");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw NotFoundException when country not found")
        void shouldThrowNotFoundException_WhenCountryNotFound() {
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(countryRepository.findByNameIgnoreCase(registerRequest.getCountry())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.signup(registerRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any country");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should set user disabled, verification code and DATABASE auth type")
        void shouldSetUserDisabled_VerificationCode_AndDatabaseAuthType() throws ConflictException, IllegalArgumentException {
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(countryRepository.findByNameIgnoreCase(registerRequest.getCountry())).thenReturn(Optional.of(testCountry));
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(settingService.getEmailSettings()).thenReturn(createMockSettingBag());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);

            userService.signup(registerRequest);

            verify(userRepository).save(argThat(user ->
                    !user.isEnabled()
                            && user.getVerificationCode() != null
                            && user.getVerificationCode().length() == 64
                            && user.getAuthenticationType() == AuthenticationType.DATABASE
            ));
        }

        @Test
        @DisplayName("Should assign ROLE_CUSTOMER to new user")
        void shouldAssignRoleCustomer_ToNewUser() throws ConflictException, IllegalArgumentException {
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(countryRepository.findByNameIgnoreCase(registerRequest.getCountry())).thenReturn(Optional.of(testCountry));
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(settingService.getEmailSettings()).thenReturn(createMockSettingBag());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);

            userService.signup(registerRequest);

            verify(userRepository).save(argThat(user ->
                    user.getRoles().contains(customerRole)
            ));
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePassword_BeforeSaving() throws ConflictException, IllegalArgumentException {
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(countryRepository.findByNameIgnoreCase(registerRequest.getCountry())).thenReturn(Optional.of(testCountry));
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
            when(settingService.getEmailSettings()).thenReturn(createMockSettingBag());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);

            userService.signup(registerRequest);

            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(argThat(user ->
                    "encodedPassword123".equals(user.getPassword())
            ));
        }

        @Test
        @DisplayName("Should send verification email after saving")
        void shouldSendVerificationEmail_AfterSaving() throws ConflictException, IllegalArgumentException {
            when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
            when(countryRepository.findByNameIgnoreCase(registerRequest.getCountry())).thenReturn(Optional.of(testCountry));
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(settingService.getEmailSettings()).thenReturn(createMockSettingBag());
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);

            userService.signup(registerRequest);

            verify(settingService).getEmailSettings();
        }
    }

    // ============================= verify =============================

    @Nested
    @DisplayName("verify - Xác thực mã verification")
    class VerifyTests {

        @Test
        @DisplayName("Should return true and enable user when valid code and user is disabled")
        void shouldReturnTrue_AndEnableUser_WhenValidCodeAndUserDisabled() {
            User user = new User();
            user.setId(1L);
            user.setEnabled(false);
            when(userRepository.findByVerificationCode("abc123")).thenReturn(user);

            boolean result = userService.verify("abc123");

            assertThat(result).isTrue();
            verify(userRepository).enableUserByID(1L);
        }

        @Test
        @DisplayName("Should return false when verification code is invalid")
        void shouldReturnFalse_WhenVerificationCodeIsInvalid() {
            when(userRepository.findByVerificationCode("invalid")).thenReturn(null);

            boolean result = userService.verify("invalid");

            assertThat(result).isFalse();
            verify(userRepository, never()).enableUserByID(anyLong());
        }

        @Test
        @DisplayName("Should return false when user is already enabled")
        void shouldReturnFalse_WhenUserAlreadyEnabled() {
            User user = new User();
            user.setId(1L);
            user.setEnabled(true);
            when(userRepository.findByVerificationCode("abc123")).thenReturn(user);

            boolean result = userService.verify("abc123");

            assertThat(result).isFalse();
            verify(userRepository, never()).enableUserByID(anyLong());
        }
    }

    // ============================= getAllUsers =============================

    @Nested
    @DisplayName("getAllUsers - Lấy tất cả người dùng")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return list of UserDTOs with image paths")
        void shouldReturnListOfUserDTOs_WithImagePaths() {
            when(userRepository.findAll()).thenReturn(List.of(testUser));
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            List<UserDTO> result = userService.getAllUsers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getImagePath()).isEqualTo("http://s3.url/photo.jpg");
            verify(awsS3Service).getImagePath(eq("user-photos/1"), eq("photo.png"));
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyList_WhenNoUsersExist() {
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            List<UserDTO> result = userService.getAllUsers();

            assertThat(result).isEmpty();
            verify(userMapper, never()).toUserDTO(any(User.class));
        }
    }

    // ============================= getUsersByPage =============================

    @Nested
    @DisplayName("getUsersByPage - Lấy người dùng phân trang")
    class GetUsersByPageTests {

        @Test
        @DisplayName("Should return paginated UserDTOs with image paths")
        void shouldReturnPaginatedUserDTOs_WithImagePaths() {
            Page<User> userPage = new PageImpl<>(List.of(testUser));

            when(pagingAndSortingHelper.getSortField()).thenReturn("name");
            when(pagingAndSortingHelper.getSortDir()).thenReturn("asc");
            when(pagingAndSortingHelper.getPageNum()).thenReturn(1);
            when(pagingAndSortingHelper.getPageSize()).thenReturn(10);
            when(pagingAndSortingHelper.getKeyword()).thenReturn(null);

            when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            Page<UserDTO> result = userService.getUsersByPage(pagingAndSortingHelper);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no users exist")
        void shouldReturnEmptyPage_WhenNoUsersExist() {
            Page<User> emptyPage = new PageImpl<>(Collections.emptyList());

            when(pagingAndSortingHelper.getSortField()).thenReturn("name");
            when(pagingAndSortingHelper.getSortDir()).thenReturn("asc");
            when(pagingAndSortingHelper.getPageNum()).thenReturn(1);
            when(pagingAndSortingHelper.getPageSize()).thenReturn(10);
            when(pagingAndSortingHelper.getKeyword()).thenReturn(null);

            when(userRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<UserDTO> result = userService.getUsersByPage(pagingAndSortingHelper);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ============================= getUserById =============================

    @Nested
    @DisplayName("getUserById - Lấy người dùng theo ID")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return UserDTO with image path when user exists")
        void shouldReturnUserDTO_WithImagePath_WhenUserExists() throws NotFoundException {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            UserDTO result = userService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getImagePath()).isEqualTo("http://s3.url/photo.jpg");
            verify(awsS3Service).getImagePath(eq("user-photos/1"), eq("photo.png"));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void shouldThrowNotFoundException_WhenUserDoesNotExist() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user with ID");

            verify(userMapper, never()).toUserDTO(any(User.class));
        }
    }

    // ============================= getUserByEmail =============================

    @Nested
    @DisplayName("getUserByEmail - Lấy người dùng theo email")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Should return UserDTO with image path when user exists")
        void shouldReturnUserDTO_WithImagePath_WhenUserExists() throws NotFoundException {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            UserDTO result = userService.getUserByEmail("test@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getImagePath()).isEqualTo("http://s3.url/photo.jpg");
        }

        @Test
        @DisplayName("Should throw NotFoundException when email not found")
        void shouldThrowNotFoundException_WhenEmailNotFound() {
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("notfound@example.com"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user with email: notfound@example.com");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ============================= saveUser =============================

    @Nested
    @DisplayName("saveUser - Lưu người dùng (Tạo mới)")
    class SaveNewUserTests {

        @Test
        @DisplayName("Should create new user successfully when email is unique")
        void shouldCreateNewUser_Successfully_WhenEmailIsUnique() throws ConflictException, IOException {
            UserRequest userRequest = new UserRequest();
            userRequest.setId(null);
            userRequest.setEmail("newuser@example.com");
            userRequest.setPassword("password123");
            userRequest.setFirstName("Jane");
            userRequest.setLastName("Smith");
            userRequest.setCountry("USA");

            when(countryRepository.findByNameIgnoreCase("USA")).thenReturn(Optional.of(testCountry));
            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(2L);
                return savedUser;
            });
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            UserDTO result = userService.saveUser(userRequest, null);

            assertThat(result).isNotNull();
            verify(userRepository).findByEmail("newuser@example.com");
            verify(passwordEncoder).encode(anyString());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists for new user")
        void shouldThrowConflictException_WhenEmailAlreadyExistsForNewUser() throws ConflictException, IOException {
            UserRequest userRequest = new UserRequest();
            userRequest.setId(null);
            userRequest.setEmail("test@example.com");
            userRequest.setCountry("USA");

            when(countryRepository.findByNameIgnoreCase("USA")).thenReturn(Optional.of(testCountry));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userService.saveUser(userRequest, null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Email is existing!");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when country not found for new user")
        void shouldThrowConflictException_WhenCountryNotFoundForNewUser() {
            UserRequest userRequest = new UserRequest();
            userRequest.setId(null);
            userRequest.setEmail("new@example.com");
            userRequest.setCountry("Unknown");

            when(countryRepository.findByNameIgnoreCase("Unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.saveUser(userRequest, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any country");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should upload photo to S3 when photo is provided for new user")
        void shouldUploadPhotoToS3_WhenPhotoIsProvidedForNewUser() throws ConflictException, IOException {
            UserRequest userRequest = new UserRequest();
            userRequest.setId(null);
            userRequest.setEmail("newuser@example.com");
            userRequest.setPassword("password123");
            userRequest.setCountry("USA");

            MockMultipartFile photo = new MockMultipartFile(
                    "photo", "avatar.png", "image/png", "photo-data".getBytes());

            when(countryRepository.findByNameIgnoreCase("USA")).thenReturn(Optional.of(testCountry));
            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(2L);
                return savedUser;
            });
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            userService.saveUser(userRequest, photo);

            verify(awsS3Service).removeFolder(contains("user-photos/"));
            verify(awsS3Service).uploadFile(anyString(), anyString(), any(), anyLong(), eq("image/png"));
        }

        @Test
        @DisplayName("Should set photo filename with UUID prefix when photo is provided")
        void shouldSetPhotoFilename_WithUUIDPrefix_WhenPhotoIsProvided() throws ConflictException, IOException {
            UserRequest userRequest = new UserRequest();
            userRequest.setId(null);
            userRequest.setEmail("newuser@example.com");
            userRequest.setPassword("password123");
            userRequest.setCountry("USA");

            MockMultipartFile photo = new MockMultipartFile(
                    "photo", "avatar.png", "image/png", "photo-data".getBytes());

            when(countryRepository.findByNameIgnoreCase("USA")).thenReturn(Optional.of(testCountry));
            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(2L);
                return savedUser;
            });
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            userService.saveUser(userRequest, photo);

            // Photo should be set to UUID_filename format
            assertThat(userRequest.getPhoto()).contains("avatar.png");
            assertThat(userRequest.getPhoto()).isNotEqualTo("avatar.png"); // Should have UUID prefix
        }

        @Test
        @DisplayName("Should set photo to null when no photo is provided for new user")
        void shouldSetPhotoToNull_WhenNoPhotoIsProvidedForNewUser() throws ConflictException, IOException {
            UserRequest userRequest = new UserRequest();
            userRequest.setId(null);
            userRequest.setEmail("newuser@example.com");
            userRequest.setPassword("password123");
            userRequest.setCountry("USA");

            when(countryRepository.findByNameIgnoreCase("USA")).thenReturn(Optional.of(testCountry));
            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(2L);
                return savedUser;
            });
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            userService.saveUser(userRequest, null);

            assertThat(userRequest.getPhoto()).isNull();
            verify(awsS3Service, never()).uploadFile(anyString(), anyString(), any(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("saveUser - Lưu người dùng (Cập nhật)")
    class SaveUpdateUserTests {

        @Test
        @DisplayName("Should update existing user when id is provided")
        void shouldUpdateExistingUser_WhenIdIsProvided() throws ConflictException, IOException {
            UserRequest userRequest = new UserRequest();
            userRequest.setId(1L);
            userRequest.setEmail("test@example.com");
            userRequest.setPassword(null);
            userRequest.setCountry("USA");

            when(countryRepository.findByNameIgnoreCase("USA")).thenReturn(Optional.of(testCountry));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            UserDTO result = userService.saveUser(userRequest, null);

            assertThat(result).isNotNull();
            verify(userRepository).findByEmail("test@example.com");
            verify(userRepository).save(any(User.class));
        }
    }

    // ============================= updateAccountDetails =============================

    @Nested
    @DisplayName("updateAccountDetails - Cập nhật thông tin tài khoản")
    class UpdateAccountDetailsTests {

        @Test
        @DisplayName("Should update account details successfully")
        void shouldUpdateAccountDetails_Successfully() throws NotFoundException, IOException {
            UserRequest userRequest = new UserRequest();
            userRequest.setEmail("test@example.com");
            userRequest.setPassword(null);
            userRequest.setCountry("USA");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            UserDTO result = userService.updateAccountDetails("test@example.com", userRequest, null);

            assertThat(result).isNotNull();
            verify(userRepository).findByEmail("test@example.com");
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should encode new password when password is provided")
        void shouldEncodeNewPassword_WhenPasswordIsProvided() throws NotFoundException, IOException {
            UserRequest userRequest = new UserRequest();
            userRequest.setEmail("test@example.com");
            userRequest.setPassword("newPassword456");
            userRequest.setCountry("USA");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword456")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            userService.updateAccountDetails("test@example.com", userRequest, null);

            verify(passwordEncoder).encode("newPassword456");
            verify(userRepository).save(argThat(user ->
                    "encodedNewPassword".equals(user.getPassword())
            ));
        }

        @Test
        @DisplayName("Should keep old password when password is empty")
        void shouldKeepOldPassword_WhenPasswordIsEmpty() throws NotFoundException, IOException {
            testUser.setPassword("oldEncodedPassword");

            UserRequest userRequest = new UserRequest();
            userRequest.setEmail("test@example.com");
            userRequest.setPassword("");
            userRequest.setCountry("USA");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            userService.updateAccountDetails("test@example.com", userRequest, null);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository).save(argThat(user ->
                    "oldEncodedPassword".equals(user.getPassword())
            ));
        }

        @Test
        @DisplayName("Should keep old photo when photo is null in request")
        void shouldKeepOldPhoto_WhenPhotoIsNullInRequest() throws NotFoundException, IOException {
            testUser.setPhoto("old-photo.png");

            UserRequest userRequest = new UserRequest();
            userRequest.setEmail("test@example.com");
            userRequest.setPassword(null);
            userRequest.setCountry("USA");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            userService.updateAccountDetails("test@example.com", userRequest, null);

            verify(userRepository).save(argThat(user ->
                    "old-photo.png".equals(user.getPhoto())
            ));
        }

        @Test
        @DisplayName("Should keep created time from existing user")
        void shouldKeepCreatedTime_FromExistingUser() throws NotFoundException, IOException {
            Date originalCreatedTime = new Date(1000000L);
            testUser.setCreatedTime(originalCreatedTime);

            UserRequest userRequest = new UserRequest();
            userRequest.setEmail("test@example.com");
            userRequest.setPassword(null);
            userRequest.setCountry("USA");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserDTO(any(User.class))).thenReturn(testUserDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/photo.jpg");

            userService.updateAccountDetails("test@example.com", userRequest, null);

            verify(userRepository).save(argThat(user ->
                    user.getCreatedTime().equals(originalCreatedTime)
            ));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found by email")
        void shouldThrowNotFoundException_WhenUserNotFoundByEmail() {
            UserRequest userRequest = new UserRequest();
            userRequest.setEmail("notfound@example.com");
            userRequest.setCountry("USA");

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateAccountDetails("notfound@example.com", userRequest, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user with email: notfound@example.com");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ============================= delete =============================

    @Nested
    @DisplayName("delete - Xóa người dùng")
    class DeleteTests {

        @Test
        @DisplayName("Should delete user and remove S3 folder successfully")
        void shouldDeleteUser_AndRemoveS3Folder_Successfully() throws NotFoundException {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.delete(1L);

            verify(userRepository).deleteById(1L);
            verify(awsS3Service).removeFolder("user-photos/1/");
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void shouldThrowNotFoundException_WhenUserDoesNotExist() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> userService.delete(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user with ID");

            verify(userRepository, never()).deleteById(anyLong());
            verify(awsS3Service, never()).removeFolder(anyString());
        }
    }

    // ============================= isEmailUnique =============================

    @Nested
    @DisplayName("isEmailUnique - Kiểm tra email duy nhất")
    class IsEmailUniqueTests {

        @Test
        @DisplayName("Should return true when email does not exist")
        void shouldReturnTrue_WhenEmailDoesNotExist() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

            boolean result = userService.isEmailUnique(1L, "new@example.com");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when email belongs to the same user (update)")
        void shouldReturnTrue_WhenEmailBelongsToSameUser() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            boolean result = userService.isEmailUnique(1L, "test@example.com");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when email belongs to a different user")
        void shouldReturnFalse_WhenEmailBelongsToDifferentUser() {
            User anotherUser = new User();
            anotherUser.setId(2L);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(anotherUser));

            boolean result = userService.isEmailUnique(1L, "test@example.com");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when email exists and id is null (new user)")
        void shouldReturnFalse_WhenEmailExistsAndIdIsNull() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            boolean result = userService.isEmailUnique(null, "test@example.com");

            assertThat(result).isFalse();
        }
    }

    // ============================= updateUserEnabledStatus =============================

    @Nested
    @DisplayName("updateUserEnabledStatus - Cập nhật trạng thái kích hoạt")
    class UpdateUserEnabledStatusTests {

        @Test
        @DisplayName("Should enable user successfully")
        void shouldEnableUser_Successfully() throws NotFoundException {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.updateUserEnabledStatus(1L, true);

            verify(userRepository).updateUserEnabledStatus(1L, true);
        }

        @Test
        @DisplayName("Should disable user successfully")
        void shouldDisableUser_Successfully() throws NotFoundException {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.updateUserEnabledStatus(1L, false);

            verify(userRepository).updateUserEnabledStatus(1L, false);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void shouldThrowNotFoundException_WhenUserDoesNotExist() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> userService.updateUserEnabledStatus(999L, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user with ID");

            verify(userRepository, never()).updateUserEnabledStatus(anyLong(), anyBoolean());
        }
    }

    // ============================= updateAuthenticationType =============================

    @Nested
    @DisplayName("updateAuthenticationType - Cập nhật kiểu xác thực")
    class UpdateAuthenticationTypeTests {

        @Test
        @DisplayName("Should update authentication type successfully")
        void shouldUpdateAuthenticationType_Successfully() {
            userService.updateAuthenticationType(testUserDTO, AuthenticationType.GOOGLE);

            verify(userRepository).updateAuthenticationType(1L, AuthenticationType.GOOGLE);
        }
    }

    // ============================= addNewCustomerUponOAuthLogin =============================

    @Nested
    @DisplayName("addNewCustomerUponOAuthLogin - Thêm khách hàng qua OAuth")
    class AddNewCustomerUponOAuthLoginTests {

        @Test
        @DisplayName("Should create new customer with GOOGLE authentication type")
        void shouldCreateNewCustomer_WithGoogleAuthType() {
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(countryRepository.findByCode("US")).thenReturn(Optional.of(testCountry));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.addNewCustomerUponOAuthLogin("John Doe", "oauth@example.com", "US", AuthenticationType.GOOGLE);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo("oauth@example.com");
            assertThat(savedUser.getFirstName()).isEqualTo("John");
            assertThat(savedUser.getLastName()).isEqualTo("Doe");
            assertThat(savedUser.isEnabled()).isTrue();
            assertThat(savedUser.getRoles()).contains(customerRole);
            verify(roleRepository).findByName("ROLE_CUSTOMER");
            verify(countryRepository).findByCode("US");
        }

        @Test
        @DisplayName("Should set only firstName when single name provided")
        void shouldSetOnlyFirstName_WhenSingleNameProvided() {
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(countryRepository.findByCode("US")).thenReturn(Optional.of(testCountry));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.addNewCustomerUponOAuthLogin("Madonna", "madonna@example.com", "US", AuthenticationType.GOOGLE);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getFirstName()).isEqualTo("Madonna");
            assertThat(savedUser.getLastName()).isEmpty();
        }

        @Test
        @DisplayName("Should set firstName and lastName when full name provided")
        void shouldSetFirstNameAndLastName_WhenFullNameProvided() {
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(countryRepository.findByCode("US")).thenReturn(Optional.of(testCountry));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.addNewCustomerUponOAuthLogin("John Doe Smith", "john@example.com", "US", AuthenticationType.FACEBOOK);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getFirstName()).isEqualTo("John");
            assertThat(savedUser.getLastName()).isEqualTo("Doe Smith");
        }

        @Test
        @DisplayName("Should set country name from country code")
        void shouldSetCountryName_FromCountryCode() {
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(countryRepository.findByCode("US")).thenReturn(Optional.of(testCountry));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.addNewCustomerUponOAuthLogin("John Doe", "oauth@example.com", "US", AuthenticationType.GOOGLE);

            verify(countryRepository).findByCode("US");
            verify(userRepository).save(argThat(user ->
                    "USA".equals(user.getCountry())
            ));
        }

        @Test
        @DisplayName("Should set enabled to true for OAuth customer")
        void shouldSetEnabledToTrue_ForOAuthCustomer() {
            when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(countryRepository.findByCode("US")).thenReturn(Optional.of(testCountry));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.addNewCustomerUponOAuthLogin("John Doe", "oauth@example.com", "US", AuthenticationType.GOOGLE);

            verify(userRepository).save(argThat(User::isEnabled));
        }
    }

    // ============================= updateResetPasswordToken =============================

    @Nested
    @DisplayName("updateResetPasswordToken - Cập nhật token đặt lại mật khẩu")
    class UpdateResetPasswordTokenTests {

        @Test
        @DisplayName("Should generate and save token successfully")
        void shouldGenerateAndSaveToken_Successfully() throws NotFoundException {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            String token = userService.updateResetPasswordToken("test@example.com");

            assertThat(token).isNotNull();
            assertThat(token).hasSize(30);
            verify(userRepository).save(testUser);
            assertThat(testUser.getResetPasswordToken()).isNotNull();
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found by email")
        void shouldThrowNotFoundException_WhenUserNotFoundByEmail() {
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateResetPasswordToken("notfound@example.com"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user with email");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ============================= updatePassword =============================

    @Nested
    @DisplayName("updatePassword - Cập nhật mật khẩu")
    class UpdatePasswordTests {

        @Test
        @DisplayName("Should update password and clear reset token successfully")
        void shouldUpdatePassword_AndClearResetToken_Successfully() throws NotFoundException {
            User user = new User();
            user.setId(1L);
            user.setResetPasswordToken("token123");
            when(userRepository.findByResetPasswordToken("token123")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.updatePassword("token123", "newPassword");

            verify(passwordEncoder).encode("newPassword");
            verify(userRepository).save(user);
            assertThat(user.getResetPasswordToken()).isNull();
        }

        @Test
        @DisplayName("Should throw NotFoundException when token is invalid")
        void shouldThrowNotFoundException_WhenTokenIsInvalid() {
            when(userRepository.findByResetPasswordToken("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updatePassword("invalid", "newPassword"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No user found: invalid token!");

            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ============================= Helper =============================

    private SettingBag createMockSettingBag() {
        Setting subjectSetting = new Setting();
        subjectSetting.setKey("CUSTOMER_VERIFY_SUBJECT");
        subjectSetting.setValue("Verify your account");

        Setting contentSetting = new Setting();
        contentSetting.setKey("CUSTOMER_VERIFY_CONTENT");
        contentSetting.setValue("Hello [[name]], click [[URL]] to verify your account.");

        return new SettingBag(List.of(subjectSetting, contentSetting));
    }
}