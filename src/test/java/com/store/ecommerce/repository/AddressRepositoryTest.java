package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Address;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("AddressRepository Integration Tests")
class AddressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AddressRepository addressRepository;

    private User john;
    private User jane;

    @BeforeEach
    void setUp() {
        // Persist users
        john = persistUser("john@example.com", "John", "Doe");
        jane = persistUser("jane@example.com", "Jane", "Smith");

        // John's addresses
        persistAddress(john, "123 Main St", "Apt 4", "New York", "NY", "10001", "USA", true);
        persistAddress(john, "456 Oak Ave", null, "Los Angeles", "CA", "90001", "USA", false);
        persistAddress(john, "789 Pine Rd", "Suite 100", "Chicago", "IL", "60601", "USA", false);

        // Jane's addresses
        persistAddress(jane, "321 Elm St", null, "Houston", "TX", "77001", "USA", true);
        persistAddress(jane, "654 Maple Dr", "Unit B", "Phoenix", "AZ", "85001", "USA", false);
    }

    // ======================== FIND BY USER ========================

    @Test
    @DisplayName("Should find all addresses by user")
    void findByUser_John() {
        // Act
        List<Address> addresses = addressRepository.findByUser(john);

        // Assert
        assertThat(addresses).hasSize(3);
        assertThat(addresses).allMatch(a -> a.getUser().getId().equals(john.getId()));
    }

    @Test
    @DisplayName("Should find Jane's addresses")
    void findByUser_Jane() {
        // Act
        List<Address> addresses = addressRepository.findByUser(jane);

        // Assert
        assertThat(addresses).hasSize(2);
        assertThat(addresses).allMatch(a -> a.getUser().getId().equals(jane.getId()));
    }

    @Test
    @DisplayName("Should return empty list for user with no addresses")
    void findByUser_NoAddresses() {
        // Arrange
        User newUser = persistUser("bob@example.com", "Bob", "Wilson");

        // Act
        List<Address> addresses = addressRepository.findByUser(newUser);

        // Assert
        assertThat(addresses).isEmpty();
    }

    @Test
    @DisplayName("Should not mix addresses between users")
    void findByUser_NoCrossUser() {
        // Act
        List<Address> johnAddresses = addressRepository.findByUser(john);
        List<Address> janeAddresses = addressRepository.findByUser(jane);

        // Assert — no overlap
        assertThat(johnAddresses).noneMatch(a -> a.getUser().getId().equals(jane.getId()));
        assertThat(janeAddresses).noneMatch(a -> a.getUser().getId().equals(john.getId()));
    }

    // ======================== FIND BY ID AND USER ID ========================

    @Test
    @DisplayName("Should find address by id and user id")
    void findByIdAndUserId_Found() {
        // Arrange
        List<Address> johnAddresses = addressRepository.findByUser(john);
        Long addressId = johnAddresses.get(0).getId();

        // Act
        Address result = addressRepository.findByIdAndUserId(addressId, john.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(addressId);
        assertThat(result.getUser().getId()).isEqualTo(john.getId());
    }

    @Test
    @DisplayName("Should return null when address does not belong to user")
    void findByIdAndUserId_WrongUser() {
        // Arrange — get John's address but query with Jane's userId
        List<Address> johnAddresses = addressRepository.findByUser(john);
        Long addressId = johnAddresses.get(0).getId();

        // Act
        Address result = addressRepository.findByIdAndUserId(addressId, jane.getId());

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when address id does not exist")
    void findByIdAndUserId_NotFound() {
        // Act
        Address result = addressRepository.findByIdAndUserId(99999L, john.getId());

        // Assert
        assertThat(result).isNull();
    }

    // ======================== DELETE BY ID AND USER ID ========================

    @Test
    @DisplayName("Should delete address by id and user id")
    void deleteByIdAndUserId_Success() {
        // Arrange — delete John's non-default address
        List<Address> johnAddresses = addressRepository.findByUser(john);
        Long targetId = johnAddresses.stream()
                .filter(a -> !a.isDefaultForShipping())
                .findFirst()
                .orElseThrow()
                .getId();

        // Act
        addressRepository.deleteByIdAndUserId(targetId, john.getId());

        // Assert
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Address.class, targetId)).isNull();
        assertThat(addressRepository.findByUser(john)).hasSize(2);
    }

    @Test
    @DisplayName("Should not delete address belonging to another user")
    void deleteByIdAndUserId_WrongUser() {
        // Arrange — try to delete John's address with Jane's userId
        List<Address> johnAddresses = addressRepository.findByUser(john);
        Long johnAddressId = johnAddresses.get(0).getId();
        long countBefore = addressRepository.count();

        // Act
        addressRepository.deleteByIdAndUserId(johnAddressId, jane.getId());

        // Assert — nothing deleted
        entityManager.flush();

        assertThat(entityManager.find(Address.class, johnAddressId)).isNotNull();
        assertThat(addressRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("Should not delete with non-existent address id")
    void deleteByIdAndUserId_NonExistentId() {
        // Arrange
        long countBefore = addressRepository.count();

        // Act
        addressRepository.deleteByIdAndUserId(99999L, john.getId());

        // Assert — nothing deleted
        entityManager.flush();
        assertThat(addressRepository.count()).isEqualTo(countBefore);
    }

    // ======================== SET DEFAULT ADDRESS ========================

    @Test
    @DisplayName("Should set address as default for shipping")
    void setDefaultAddress_Success() {
        // Arrange — set John's second address as default
        List<Address> johnAddresses = addressRepository.findByUser(john);
        Address targetAddress = johnAddresses.stream()
                .filter(a -> !a.isDefaultForShipping())
                .findFirst()
                .orElseThrow();

        // Act
        addressRepository.setDefaultAddress(targetAddress.getId());

        // Assert
        entityManager.flush();
        entityManager.clear();

        Address updated = entityManager.find(Address.class, targetAddress.getId());
        assertThat(updated.isDefaultForShipping()).isTrue();
    }

    @Test
    @DisplayName("Should set already-default address as default again")
    void setDefaultAddress_AlreadyDefault() {
        // Arrange — find John's current default address
        Address defaultAddr = addressRepository.findDefaultByUserId(john.getId());
        assertThat(defaultAddr).isNotNull();
        assertThat(defaultAddr.isDefaultForShipping()).isTrue();

        // Act — set it as default again
        addressRepository.setDefaultAddress(defaultAddr.getId());

        // Assert — still default
        entityManager.flush();
        entityManager.clear();

        Address updated = entityManager.find(Address.class, defaultAddr.getId());
        assertThat(updated.isDefaultForShipping()).isTrue();
    }

    // ======================== SET NON DEFAULT FOR OTHERS ========================

    @Test
    @DisplayName("Should set other addresses as non-default when new default is set")
    void setNonDefaultForOthers_Success() {
        // Arrange — John has 1 default + 2 non-default addresses
        // Set John's second address as default first
        List<Address> johnAddresses = addressRepository.findByUser(john);
        Address newDefault = johnAddresses.stream()
                .filter(a -> !a.isDefaultForShipping())
                .findFirst()
                .orElseThrow();

        addressRepository.setDefaultAddress(newDefault.getId());

        // Act — now set all OTHER John's addresses to non-default
        addressRepository.setNonDefaultForOthers(newDefault.getId(), john.getEmail());

        // Assert
        entityManager.flush();
        entityManager.clear();

        List<Address> updatedAddresses = addressRepository.findByUser(john);
        long defaultCount = updatedAddresses.stream()
                .filter(Address::isDefaultForShipping)
                .count();

        // Only the new default should be true
        assertThat(defaultCount).isEqualTo(1);
        assertThat(updatedAddresses.stream()
                .filter(a -> a.getId().equals(newDefault.getId()))
                .findFirst()
                .orElseThrow()
                .isDefaultForShipping()).isTrue();
    }

    @Test
    @DisplayName("Should not affect other users' default addresses")
    void setNonDefaultForOthers_OtherUserUnaffected() {
        // Arrange — verify Jane has a default address
        Address janeDefault = addressRepository.findDefaultByUserId(jane.getId());
        assertThat(janeDefault).isNotNull();
        assertThat(janeDefault.isDefaultForShipping()).isTrue();

        // Act — change John's non-default settings
        List<Address> johnAddresses = addressRepository.findByUser(john);
        Address johnDefault = johnAddresses.stream()
                .filter(Address::isDefaultForShipping)
                .findFirst()
                .orElseThrow();

        addressRepository.setNonDefaultForOthers(johnDefault.getId(), john.getEmail());

        // Assert — Jane's default should remain unchanged
        entityManager.flush();
        entityManager.clear();

        Address janeDefaultAfter = entityManager.find(Address.class, janeDefault.getId());
        assertThat(janeDefaultAfter.isDefaultForShipping()).isTrue();
    }

    @Test
    @DisplayName("Should handle setting non-default when user has only one address")
    void setNonDefaultForOthers_SingleAddress() {
        // Arrange — create a user with only 1 address
        User solo = persistUser("solo@example.com", "Solo", "User");
        Address soloAddr = persistAddress(solo, "1 Solo St", null, "Denver", "CO", "80201", "USA", true);

        // Act — set non-default for others (there are no others)
        addressRepository.setNonDefaultForOthers(soloAddr.getId(), solo.getEmail());

        // Assert — solo's address should still be default (no others to change)
        entityManager.flush();
        entityManager.clear();

        Address updated = entityManager.find(Address.class, soloAddr.getId());
        assertThat(updated.isDefaultForShipping()).isTrue();
    }

    // ======================== FIND DEFAULT BY USER ID ========================

    @Test
    @DisplayName("Should find default address for John")
    void findDefaultByUserId_Found() {
        // Act
        Address defaultAddr = addressRepository.findDefaultByUserId(john.getId());

        // Assert
        assertThat(defaultAddr).isNotNull();
        assertThat(defaultAddr.isDefaultForShipping()).isTrue();
        assertThat(defaultAddr.getUser().getId()).isEqualTo(john.getId());
        assertThat(defaultAddr.getCity()).isEqualTo("New York");
    }

    @Test
    @DisplayName("Should find default address for Jane")
    void findDefaultByUserId_Jane() {
        // Act
        Address defaultAddr = addressRepository.findDefaultByUserId(jane.getId());

        // Assert
        assertThat(defaultAddr).isNotNull();
        assertThat(defaultAddr.isDefaultForShipping()).isTrue();
        assertThat(defaultAddr.getCity()).isEqualTo("Houston");
    }

    @Test
    @DisplayName("Should return null when user has no default address")
    void findDefaultByUserId_NoDefault() {
        // Arrange — create user with non-default address only
        User newUser = persistUser("nodefault@example.com", "No", "Default");
        persistAddress(newUser, "999 Test St", null, "Boston", "MA", "02101", "USA", false);

        // Act
        Address result = addressRepository.findDefaultByUserId(newUser.getId());

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when user has no addresses")
    void findDefaultByUserId_NoAddresses() {
        // Arrange
        User newUser = persistUser("empty@example.com", "Empty", "User");

        // Act
        Address result = addressRepository.findDefaultByUserId(newUser.getId());

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return only one default address per user")
    void findDefaultByUserId_OnlyOneDefault() {
        // Act — set a new default and ensure only 1 exists
        List<Address> johnAddresses = addressRepository.findByUser(john);
        Address newDefault = johnAddresses.stream()
                .filter(a -> !a.isDefaultForShipping())
                .findFirst()
                .orElseThrow();

        addressRepository.setDefaultAddress(newDefault.getId());
        addressRepository.setNonDefaultForOthers(newDefault.getId(), john.getEmail());
        entityManager.flush();
        entityManager.clear();

        // Assert
        Address defaultAddr = addressRepository.findDefaultByUserId(john.getId());
        assertThat(defaultAddr).isNotNull();
        assertThat(defaultAddr.getId()).isEqualTo(newDefault.getId());

        // Verify only 1 default in total for John
        List<Address> updated = addressRepository.findByUser(john);
        long defaultCount = updated.stream().filter(Address::isDefaultForShipping).count();
        assertThat(defaultCount).isEqualTo(1);
    }

    // ======================== FULL DEFAULT ADDRESS WORKFLOW ========================

    @Test
    @DisplayName("Should handle full default address change workflow")
    void defaultAddressWorkflow_ChangeDefault() {
        // Step 1: Verify initial state — John's default is "New York"
        Address initialDefault = addressRepository.findDefaultByUserId(john.getId());
        assertThat(initialDefault.getCity()).isEqualTo("New York");

        // Step 2: Set "Los Angeles" as new default
        List<Address> addresses = addressRepository.findByUser(john);
        Address laAddress = addresses.stream()
                .filter(a -> a.getCity().equals("Los Angeles"))
                .findFirst()
                .orElseThrow();

        addressRepository.setDefaultAddress(laAddress.getId());
        addressRepository.setNonDefaultForOthers(laAddress.getId(), john.getEmail());
        entityManager.flush();
        entityManager.clear();

        // Step 3: Verify "Los Angeles" is now the only default
        Address newDefault = addressRepository.findDefaultByUserId(john.getId());
        assertThat(newDefault.getCity()).isEqualTo("Los Angeles");
        assertThat(newDefault.isDefaultForShipping()).isTrue();

        // Step 4: Verify old default is no longer default
        Address oldDefault = entityManager.find(Address.class, initialDefault.getId());
        assertThat(oldDefault.isDefaultForShipping()).isFalse();
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve address")
    void save_AndFindById() {
        // Arrange
        Address address = new Address();
        address.setUser(john);
        address.setFirstName(john.getFirstName());
        address.setLastName(john.getLastName());
        address.setPhoneNumber(john.getPhoneNumber());
        address.setAddressLine1("100 New St");
        address.setAddressLine2("");
        address.setCity("Miami");
        address.setState("FL");
        address.setPostalCode("33101");
        address.setCountry("USA");
        address.setDefaultForShipping(false);

        // Act
        Address saved = addressRepository.save(address);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getCity()).isEqualTo("Miami");
    }

    @Test
    @DisplayName("Should delete address by id")
    void deleteById_Success() {
        // Arrange
        List<Address> addresses = addressRepository.findByUser(john);
        Long addressId = addresses.get(0).getId();

        // Act
        addressRepository.deleteById(addressId);

        // Assert
        assertThat(addressRepository.findById(addressId)).isEmpty();
    }

    @Test
    @DisplayName("Should count addresses correctly")
    void count_Success() {
        // 3 (John) + 2 (Jane) = 5
        assertThat(addressRepository.count()).isEqualTo(5);
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
        user.setPhoneNumber("000-000-0000");
        user.setAddressLine1("123 Default St");
        user.setCity("Default City");
        user.setState("DS");
        user.setPostalCode("00000");
        user.setCountry("US");

        user.setBirthOfDate(DateUtil.toDateTime(LocalDate.of(1990, 1, 1)));

        return entityManager.persistAndFlush(user);
    }

    private Address persistAddress(User user, String addressLine1, String addressLine2,
                                   String city, String state, String postalCode,
                                   String country, boolean defaultForShipping) {
        Address address = new Address();
        address.setFirstName(user.getFirstName());
        address.setLastName(user.getLastName());
        address.setPhoneNumber(user.getPhoneNumber());
        address.setUser(user);
        address.setAddressLine1(addressLine1);
        address.setAddressLine2(addressLine2);
        address.setCity(city);
        address.setState(state);
        address.setPostalCode(postalCode);
        address.setCountry(country);
        address.setDefaultForShipping(defaultForShipping);
        return entityManager.persistAndFlush(address);
    }
}