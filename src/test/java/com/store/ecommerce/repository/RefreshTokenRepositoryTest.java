package com.store.ecommerce.repository;

import com.store.ecommerce.entity.RefreshToken;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("RefreshTokenRepository Integration Tests")
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User john;
    private User jane;
    private RefreshToken johnToken;
    private RefreshToken janeToken;

    @BeforeEach
    void setUp() {
        // Persist users
        john = persistUser("john@example.com", "John", "Doe");
        jane = persistUser("jane@example.com", "Jane", "Smith");

        // Persist refresh tokens
        johnToken = persistRefreshToken("token-john-abc123", john);
        janeToken = persistRefreshToken("token-jane-xyz789", jane);
    }

    // ======================== FIND BY TOKEN ========================

    @Test
    @DisplayName("Should find refresh token by token string")
    void findByToken_Found() {
        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-john-abc123");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("token-john-abc123");
        assertThat(found.get().getUser().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should find Jane's token by token string")
    void findByToken_Jane() {
        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-jane-xyz789");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("Should return empty when token not found")
    void findByToken_NotFound() {
        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("non-existent-token");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find token by exact value - case sensitive")
    void findByToken_CaseSensitive() {
        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("TOKEN-JOHN-ABC123");

        // Assert
        assertThat(found).isEmpty();
    }

    // ======================== FIND BY USER ID ========================

    @Test
    @DisplayName("Should find refresh token by user id")
    void findByUserId_Found() {
        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByUserId(john.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("token-john-abc123");
        assertThat(found.get().getUser().getId()).isEqualTo(john.getId());
    }

    @Test
    @DisplayName("Should find Jane's token by user id")
    void findByUserId_Jane() {
        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByUserId(jane.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("token-jane-xyz789");
    }

    @Test
    @DisplayName("Should return empty when user has no refresh token")
    void findByUserId_NotFound() {
        // Arrange
        User newUser = persistUser("bob@example.com", "Bob", "Wilson");

        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByUserId(newUser.getId());

        // Assert
        assertThat(found).isEmpty();
    }

    // ======================== DELETE BY USER EMAIL ========================

    @Test
    @DisplayName("Should delete refresh token by user email")
    void deleteByUserEmail_Success() {
        // Arrange — verify John has a token
        Optional<RefreshToken> beforeDelete = refreshTokenRepository.findByUserId(john.getId());
        assertThat(beforeDelete).isPresent();

        // Act
        refreshTokenRepository.deleteByUserEmail(john.getEmail());

        // Assert
        entityManager.flush();
        entityManager.clear();

        Optional<RefreshToken> afterDelete = refreshTokenRepository.findByUserId(john.getId());
        assertThat(afterDelete).isEmpty();
    }

    @Test
    @DisplayName("Should not affect other users' tokens when deleting by email")
    void deleteByUserEmail_OtherUserUnaffected() {
        // Arrange — verify Jane has a token
        Optional<RefreshToken> janeTokenBefore = refreshTokenRepository.findByUserId(jane.getId());
        assertThat(janeTokenBefore).isPresent();

        // Act — delete John's token
        refreshTokenRepository.deleteByUserEmail(john.getEmail());

        // Assert — Jane's token should remain
        entityManager.flush();
        entityManager.clear();

        Optional<RefreshToken> janeTokenAfter = refreshTokenRepository.findByUserId(jane.getId());
        assertThat(janeTokenAfter).isPresent();
    }

    @Test
    @DisplayName("Should handle deleting non-existent email gracefully")
    void deleteByUserEmail_NonExistentEmail() {
        // Arrange
        long countBefore = refreshTokenRepository.count();

        // Act
        refreshTokenRepository.deleteByUserEmail("nonexistent@example.com");

        // Assert — nothing deleted
        entityManager.flush();
        assertThat(refreshTokenRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("Should allow creating new token after deletion")
    void deleteByUserEmail_ThenCreateNew() {
        // Act — delete John's token then create a new one
        refreshTokenRepository.deleteByUserEmail(john.getEmail());
        entityManager.flush();
        entityManager.clear();

        persistRefreshToken("token-john-new456", john);

        // Assert — new token should be findable
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-john-new456");
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("john@example.com");

        // Old token should be gone
        Optional<RefreshToken> oldToken = refreshTokenRepository.findByToken("token-john-abc123");
        assertThat(oldToken).isEmpty();
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve refresh token")
    void save_AndFindById() {
        // Arrange
        User user = persistUser("new@example.com", "New", "User");
        RefreshToken token = new RefreshToken();
        token.setToken("token-new-user-001");
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(86400)); // ← ADD this line

        // Act
        RefreshToken saved = refreshTokenRepository.save(token);

        // Assert
        assertThat(saved.getToken()).isEqualTo("token-new-user-001");
        assertThat(saved.getUser().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("Should delete refresh token by id")
    void deleteById_Success() {
        // Act — delete the managed entity directly
        refreshTokenRepository.delete(johnToken);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-john-abc123");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should count refresh tokens correctly")
    void count_Success() {
        // 2 from setUp
        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    // ======================== HELPER METHODS ========================

    private User persistUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setAuthenticationType(AuthenticationType.DATABASE);
        user.setAddressLine1("123 Main St");
        user.setCity("New York");
        user.setState("NY");
        user.setPostalCode("10001");
        user.setCountry("US");
        user.setPhoneNumber("2125551234");
        user.setBirthOfDate(DateUtil.toDateTime(LocalDate.of(1990, 1, 15)));
        return entityManager.persistAndFlush(user);
    }

    private RefreshToken persistRefreshToken(String token, User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(86400)); // 24 hours from now
        return entityManager.persistAndFlush(refreshToken);
    }
}