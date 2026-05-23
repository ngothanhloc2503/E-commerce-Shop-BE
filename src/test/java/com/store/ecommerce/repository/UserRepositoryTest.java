package com.store.ecommerce.repository;

import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createAndPersistUser("john@example.com", "John", "Doe");
    }

    // ======================== FIND BY EMAIL ========================

    @Nested
    @DisplayName("FindByEmail Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by email when exists")
        void findByEmail_Found() {
            // Act
            Optional<User> found = userRepository.findByEmail("john@example.com");

            // Assert
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("john@example.com");
            assertThat(found.get().getFirstName()).isEqualTo("John");
            assertThat(found.get().getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should return empty when email not found")
        void findByEmail_NotFound() {
            // Act
            Optional<User> found = userRepository.findByEmail("notexist@example.com");

            // Assert
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should find user by email case-sensitive")
        void findByEmail_CaseSensitive() {
            // Act
            Optional<User> found = userRepository.findByEmail("JOHN@EXAMPLE.COM");

            // Assert — JPA default is case-sensitive
            assertThat(found).isEmpty();
        }
    }

    // ======================== UPDATE USER ENABLED STATUS ========================

    @Nested
    @DisplayName("UpdateUserEnabledStatus Tests")
    class UpdateUserEnabledStatusTests {

        @Test
        @DisplayName("Should enable user by id")
        void updateUserEnabledStatus_Enable() {
            // Arrange
            testUser.setEnabled(false);
            entityManager.persistAndFlush(testUser);
            assertThat(testUser.isEnabled()).isFalse();

            // Act
            userRepository.updateUserEnabledStatus(testUser.getId(), true);

            // Assert — must flush and clear to get fresh data from DB
            entityManager.flush();
            entityManager.clear();

            User updated = entityManager.find(User.class, testUser.getId());
            assertThat(updated.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should disable user by id")
        void updateUserEnabledStatus_Disable() {
            // Arrange
            testUser.setEnabled(true);
            entityManager.persistAndFlush(testUser);

            // Act
            userRepository.updateUserEnabledStatus(testUser.getId(), false);

            // Assert
            entityManager.flush();
            entityManager.clear();

            User updated = entityManager.find(User.class, testUser.getId());
            assertThat(updated.isEnabled()).isFalse();
        }
    }

    // ======================== FIND ALL (PAGEABLE) ========================

    @Nested
    @DisplayName("FindAll Pageable Tests")
    class FindAllPageableTests {

        @Test
        @DisplayName("Should return paginated users")
        void findAll_Pageable() {
            // Arrange
            createAndPersistUser("user2@example.com", "Jane", "Smith");
            createAndPersistUser("user3@example.com", "Bob", "Wilson");
            createAndPersistUser("user4@example.com", "Alice", "Brown");

            PageRequest pageable = PageRequest.of(0, 2);

            // Act
            Page<User> page = userRepository.findAll(pageable);

            // Assert
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(4); // 3 + testUser from setUp
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.isFirst()).isTrue();
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("Should return second page of users")
        void findAll_Pageable_SecondPage() {
            // Arrange
            createAndPersistUser("user2@example.com", "Jane", "Smith");
            createAndPersistUser("user3@example.com", "Bob", "Wilson");
            createAndPersistUser("user4@example.com", "Alice", "Brown");

            PageRequest pageable = PageRequest.of(1, 2);

            // Act
            Page<User> page = userRepository.findAll(pageable);

            // Assert
            assertThat(page.getContent()).hasSize(2); // page 2 of 4 total
            assertThat(page.getTotalElements()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should return empty page when page exceeds total")
        void findAll_Pageable_ExceedsTotal() {
            // Arrange
            PageRequest pageable = PageRequest.of(10, 5);

            // Act
            Page<User> page = userRepository.findAll(pageable);

            // Assert
            assertThat(page.getContent()).isEmpty();
        }
    }

    // ======================== FIND ALL BY KEYWORD (SORT) ========================

    @Nested
    @DisplayName("FindAll Keyword + Sort Tests")
    class FindAllKeywordSortTests {

        @Test
        @DisplayName("Should find users by keyword in email")
        void findAll_KeywordSort_SearchByEmail() {
            // Arrange
            Sort sort = Sort.by("firstName").ascending();

            // Act
            List<User> result = userRepository.findAll("john@example.com", sort);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).anyMatch(u -> u.getEmail().equals("john@example.com"));
        }

        @Test
        @DisplayName("Should find users by keyword in firstName")
        void findAll_KeywordSort_SearchByFirstName() {
            // Arrange
            createAndPersistUser("jane@example.com", "Jane", "Smith");
            Sort sort = Sort.by("firstName").ascending();

            // Act
            List<User> result = userRepository.findAll("Jane", sort);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).anyMatch(u -> u.getFirstName().equals("Jane"));
        }

        @Test
        @DisplayName("Should find users by keyword in lastName")
        void findAll_KeywordSort_SearchByLastName() {
            // Arrange
            Sort sort = Sort.by("firstName").ascending();

            // Act
            List<User> result = userRepository.findAll("Doe", sort);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).anyMatch(u -> u.getLastName().equals("Doe"));
        }

        @Test
        @DisplayName("Should find users by keyword in id")
        void findAll_KeywordSort_SearchById() {
            // Arrange
            Sort sort = Sort.by("id").ascending();
            String keyword = String.valueOf(testUser.getId());

            // Act
            List<User> result = userRepository.findAll(keyword, sort);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).anyMatch(u -> u.getId().equals(testUser.getId()));
        }

        @Test
        @DisplayName("Should return empty list when keyword matches nothing")
        void findAll_KeywordSort_NoMatch() {
            // Arrange
            Sort sort = Sort.by("firstName").ascending();

            // Act
            List<User> result = userRepository.findAll("XYZNonExistent", sort);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find users with partial keyword match")
        void findAll_KeywordSort_PartialMatch() {
            // Arrange
            createAndPersistUser("jane@example.com", "Jane", "Smith");
            Sort sort = Sort.by("firstName").ascending();

            // Act — "Joh" should match "John"
            List<User> result = userRepository.findAll("Joh", sort);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Should sort results ascending by firstName")
        void findAll_KeywordSort_AscendingSort() {
            // Arrange
            createAndPersistUser("alice@example.com", "Alice", "Brown");
            createAndPersistUser("bob@example.com", "Bob", "Wilson");

            Sort sort = Sort.by("firstName").ascending();

            // Act — keyword "o" matches John, Bob (in firstName/lastName/email)
            List<User> result = userRepository.findAll("o", sort);

            // Assert — verify results are sorted
            assertThat(result.size()).isGreaterThan(0);
            List<String> names = result.stream().map(User::getFirstName).toList();
            assertThat(names).isSorted();
        }
    }

    // ======================== FIND ALL BY KEYWORD (PAGEABLE) ========================

    @Nested
    @DisplayName("FindAll Keyword + Pageable Tests")
    class FindAllKeywordPageableTests {

        @Test
        @DisplayName("Should find users by keyword with pagination")
        void findAll_KeywordPageable_Found() {
            // Arrange
            createAndPersistUser("jane@example.com", "Jane", "Smith");
            createAndPersistUser("bob@example.com", "Bob", "Wilson");

            PageRequest pageable = PageRequest.of(0, 10);

            // Act
            Page<User> result = userRepository.findAll("John", pageable);

            // Assert
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent()).anyMatch(u -> u.getFirstName().equals("John"));
        }

        @Test
        @DisplayName("Should return paginated keyword search results")
        void findAll_KeywordPageable_Pagination() {
            // Arrange — create multiple users matching keyword "o"
            createAndPersistUser("jane@example.com", "Jane", "Smith");
            createAndPersistUser("bob@example.com", "Bob", "Wilson");
            createAndPersistUser("alice@example.com", "Alice", "Brown");
            createAndPersistUser("tom@example.com", "Tom", "Jones");

            PageRequest pageable = PageRequest.of(0, 2);

            // Act
            Page<User> result = userRepository.findAll("o", pageable);

            // Assert
            assertThat(result.getContent().size()).isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty page when keyword matches nothing")
        void findAll_KeywordPageable_NoMatch() {
            // Arrange
            PageRequest pageable = PageRequest.of(0, 10);

            // Act
            Page<User> result = userRepository.findAll("XYZNonExistent", pageable);

            // Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // ======================== FIND BY VERIFICATION CODE ========================

    @Nested
    @DisplayName("FindByVerificationCode Tests")
    class FindByVerificationCodeTests {

        @Test
        @DisplayName("Should find user by verification code")
        void findByVerificationCode_Found() {
            // Arrange
            testUser.setVerificationCode("VERIFY_ABC123");
            entityManager.persistAndFlush(testUser);

            // Act
            User found = userRepository.findByVerificationCode("VERIFY_ABC123");

            // Assert
            assertThat(found).isNotNull();
            assertThat(found.getEmail()).isEqualTo("john@example.com");
            assertThat(found.getVerificationCode()).isEqualTo("VERIFY_ABC123");
        }

        @Test
        @DisplayName("Should return null when verification code not found")
        void findByVerificationCode_NotFound() {
            // Act
            User found = userRepository.findByVerificationCode("INVALID_CODE");

            // Assert
            assertThat(found).isNull();
        }
    }

    // ======================== ENABLE USER BY ID ========================

    @Nested
    @DisplayName("EnableUserByID Tests")
    class EnableUserByIDTests {

        @Test
        @DisplayName("Should enable disabled user by id")
        void enableUserByID_Success() {
            // Arrange
            testUser.setEnabled(false);
            entityManager.persistAndFlush(testUser);

            // Act
            userRepository.enableUserByID(testUser.getId());

            // Assert
            entityManager.flush();
            entityManager.clear();

            User updated = entityManager.find(User.class, testUser.getId());
            assertThat(updated.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should keep user enabled if already enabled")
        void enableUserByID_AlreadyEnabled() {
            // Arrange
            testUser.setEnabled(true);
            entityManager.persistAndFlush(testUser);

            // Act
            userRepository.enableUserByID(testUser.getId());

            // Assert
            entityManager.flush();
            entityManager.clear();

            User updated = entityManager.find(User.class, testUser.getId());
            assertThat(updated.isEnabled()).isTrue();
        }
    }

    // ======================== UPDATE AUTHENTICATION TYPE ========================

    @Nested
    @DisplayName("UpdateAuthenticationType Tests")
    class UpdateAuthenticationTypeTests {

        @Test
        @DisplayName("Should update authentication type to GOOGLE")
        void updateAuthenticationType_ToGoogle() {
            // Arrange
            testUser.setAuthenticationType(AuthenticationType.DATABASE);
            entityManager.persistAndFlush(testUser);

            // Act
            userRepository.updateAuthenticationType(testUser.getId(), AuthenticationType.GOOGLE);

            // Assert
            entityManager.flush();
            entityManager.clear();

            User updated = entityManager.find(User.class, testUser.getId());
            assertThat(updated.getAuthenticationType()).isEqualTo(AuthenticationType.GOOGLE);
        }

        @Test
        @DisplayName("Should update authentication type to FACEBOOK")
        void updateAuthenticationType_ToFacebook() {
            // Arrange
            testUser.setAuthenticationType(AuthenticationType.DATABASE);
            entityManager.persistAndFlush(testUser);

            // Act
            userRepository.updateAuthenticationType(testUser.getId(), AuthenticationType.FACEBOOK);

            // Assert
            entityManager.flush();
            entityManager.clear();

            User updated = entityManager.find(User.class, testUser.getId());
            assertThat(updated.getAuthenticationType()).isEqualTo(AuthenticationType.FACEBOOK);
        }

        @Test
        @DisplayName("Should revert authentication type back to DATABASE")
        void updateAuthenticationType_RevertToDatabase() {
            // Arrange
            testUser.setAuthenticationType(AuthenticationType.GOOGLE);
            entityManager.persistAndFlush(testUser);

            // Act
            userRepository.updateAuthenticationType(testUser.getId(), AuthenticationType.DATABASE);

            // Assert
            entityManager.flush();
            entityManager.clear();

            User updated = entityManager.find(User.class, testUser.getId());
            assertThat(updated.getAuthenticationType()).isEqualTo(AuthenticationType.DATABASE);
        }
    }

    // ======================== FIND BY RESET PASSWORD TOKEN ========================

    @Nested
    @DisplayName("FindByResetPasswordToken Tests")
    class FindByResetPasswordTokenTests {

        @Test
        @DisplayName("Should find user by reset password token")
        void findByResetPasswordToken_Found() {
            // Arrange
            testUser.setResetPasswordToken("RESET_TOKEN_XYZ789");
            entityManager.persistAndFlush(testUser);

            // Act
            User found = userRepository.findByResetPasswordToken("RESET_TOKEN_XYZ789");

            // Assert
            assertThat(found).isNotNull();
            assertThat(found.getEmail()).isEqualTo("john@example.com");
            assertThat(found.getResetPasswordToken()).isEqualTo("RESET_TOKEN_XYZ789");
        }

        @Test
        @DisplayName("Should return null when reset password token not found")
        void findByResetPasswordToken_NotFound() {
            // Act
            User found = userRepository.findByResetPasswordToken("INVALID_TOKEN");

            // Assert
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("Should find user by new reset token after update")
        void findByResetPasswordToken_AfterTokenChange() {
            // Arrange
            testUser.setResetPasswordToken("OLD_TOKEN");
            entityManager.persistAndFlush(testUser);

            // Update token
            testUser.setResetPasswordToken("NEW_TOKEN");
            entityManager.persistAndFlush(testUser);

            // Act
            User foundByOld = userRepository.findByResetPasswordToken("OLD_TOKEN");
            User foundByNew = userRepository.findByResetPasswordToken("NEW_TOKEN");

            // Assert
            assertThat(foundByOld).isNull();
            assertThat(foundByNew).isNotNull();
            assertThat(foundByNew.getEmail()).isEqualTo("john@example.com");
        }
    }

    // ======================== CRUD BASICS ========================

    @Nested
    @DisplayName("CRUD Basic Tests")
    class CrudBasicTests {

        @Test
        @DisplayName("Should save and retrieve user")
        void save_AndFindById() {
            // Arrange
            User newUser = new User();
            newUser.setEmail("newuser@example.com");
            newUser.setFirstName("New");
            newUser.setLastName("User");
            newUser.setPassword("encodedPassword");
            newUser.setEnabled(true);
            newUser.setAuthenticationType(AuthenticationType.DATABASE);
            newUser.setBirthOfDate(sampleDate());
            newUser.setAddressLine1("456 Oak Ave");
            newUser.setCity("Los Angeles");
            newUser.setState("CA");
            newUser.setPostalCode("90001");
            newUser.setCountry("US");
            newUser.setPhoneNumber("555-5678");
            newUser.setBirthOfDate(DateUtil.toDateTime(LocalDate.of(1990, 1, 1)));

            // Act
            User saved = userRepository.save(newUser);

            // Assert
            assertThat(saved.getId()).isGreaterThan(0);
            Optional<User> found = userRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("newuser@example.com");
        }

        @Test
        @DisplayName("Should delete user by id")
        void deleteById_Success() {
            // Arrange
            Long userId = testUser.getId();

            // Act
            userRepository.deleteById(userId);

            // Assert
            Optional<User> found = userRepository.findById(userId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should count users correctly")
        void count_Success() {
            // Arrange
            createAndPersistUser("user2@example.com", "Jane", "Smith");
            createAndPersistUser("user3@example.com", "Bob", "Wilson");

            // Act
            long count = userRepository.count();

            // Assert — 2 new + 1 from setUp
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find all users")
        void findAll_NoParam() {
            // Arrange
            createAndPersistUser("user2@example.com", "Jane", "Smith");

            // Act
            List<User> users = userRepository.findAll();

            // Assert
            assertThat(users).hasSize(2); // 1 from setUp + 1 new
        }
    }

    // ======================== HELPER METHODS ========================

    private User createAndPersistUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setAuthenticationType(AuthenticationType.DATABASE);
        user.setBirthOfDate(sampleDate());
        user.setAddressLine1("123 Main St");
        user.setCity("New York");
        user.setState("NY");
        user.setPostalCode("10001");
        user.setCountry("US");
        user.setPhoneNumber("555-1234");
        user.setBirthOfDate(DateUtil.toDateTime(LocalDate.of(1990, 1, 1)));
        return entityManager.persistAndFlush(user);
    }

    private Date sampleDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(1995, Calendar.JUNE, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}