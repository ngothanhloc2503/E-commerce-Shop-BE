package com.store.ecommerce.service;

import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.AddressRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressServiceImpl Unit Tests")
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User testUser;
    private Address defaultAddress;
    private Address nonDefaultAddress;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("john@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        defaultAddress = new Address();
        defaultAddress.setId(1L);
        defaultAddress.setUser(testUser);
        defaultAddress.setAddressLine1("123 Main St");
        defaultAddress.setCity("New York");
        defaultAddress.setState("NY");
        defaultAddress.setPostalCode("10001");
        defaultAddress.setCountry("USA");
        defaultAddress.setDefaultForShipping(true);

        nonDefaultAddress = new Address();
        nonDefaultAddress.setId(2L);
        nonDefaultAddress.setUser(testUser);
        nonDefaultAddress.setAddressLine1("456 Oak Ave");
        nonDefaultAddress.setCity("Los Angeles");
        nonDefaultAddress.setState("CA");
        nonDefaultAddress.setPostalCode("90001");
        nonDefaultAddress.setCountry("USA");
        nonDefaultAddress.setDefaultForShipping(false);
    }

    // ======================== LIST ADDRESS BOOK ========================

    @Nested
    @DisplayName("listAddressBook() Tests")
    class ListAddressBookTests {

        @Test
        @DisplayName("Should return list of addresses for user")
        void listAddressBook_Success() throws NotFoundException {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUser(testUser)).thenReturn(List.of(defaultAddress, nonDefaultAddress));

            // Act
            List<Address> result = addressService.listAddressBook("john@example.com");

            // Assert
            assertThat(result).hasSize(2);
            verify(userRepository).findByEmail("john@example.com");
            verify(addressRepository).findByUser(testUser);
        }

        @Test
        @DisplayName("Should return empty list when user has no addresses")
        void listAddressBook_EmptyList() throws NotFoundException {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUser(testUser)).thenReturn(List.of());

            // Act
            List<Address> result = addressService.listAddressBook("john@example.com");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void listAddressBook_UserNotFound() {
            // Arrange
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> addressService.listAddressBook("notfound@example.com"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user have email: notfound@example.com");
            verify(addressRepository, never()).findByUser(any());
        }
    }

    // ======================== SAVE ========================

    @Nested
    @DisplayName("save() Tests")
    class SaveTests {

        @Test
        @DisplayName("Should create new address when id is null")
        void save_NewAddress_IdNull() throws NotFoundException {
            // Arrange
            Address newAddress = new Address();
            newAddress.setAddressLine1("789 Pine Rd");
            newAddress.setCity("Chicago");
            newAddress.setState("IL");
            newAddress.setCountry("USA");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Address result = addressService.save("john@example.com", newAddress);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.isDefaultForShipping()).isFalse();
            verify(addressRepository).save(any(Address.class));
        }

        @Test
        @DisplayName("Should create new address when id is 0 (treated as new)")
        void save_NewAddress_IdZero() throws NotFoundException {
            // Arrange
            Address newAddress = new Address();
            newAddress.setId(0L);
            newAddress.setAddressLine1("789 Pine Rd");
            newAddress.setCity("Chicago");
            newAddress.setState("IL");
            newAddress.setCountry("USA");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Address result = addressService.save("john@example.com", newAddress);

            // Assert
            assertThat(result.isDefaultForShipping()).isFalse();
            assertThat(result.getId()).isNull(); // id was set to null because <= 0
            verify(addressRepository).save(any(Address.class));
        }

        @Test
        @DisplayName("Should create new address when id is negative (treated as new)")
        void save_NewAddress_IdNegative() throws NotFoundException {
            // Arrange
            Address newAddress = new Address();
            newAddress.setId(-1L);
            newAddress.setAddressLine1("789 Pine Rd");
            newAddress.setCity("Chicago");
            newAddress.setState("IL");
            newAddress.setCountry("USA");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Address result = addressService.save("john@example.com", newAddress);

            // Assert
            assertThat(result.isDefaultForShipping()).isFalse();
            assertThat(result.getId()).isNull(); // id was set to null because <= 0
        }

        @Test
        @DisplayName("Should update existing address preserving defaultForShipping")
        void save_UpdateExisting() throws NotFoundException {
            // Arrange
            Address updateData = new Address();
            updateData.setId(1L);
            updateData.setAddressLine1("999 Updated St");
            updateData.setCity("Boston");
            updateData.setState("MA");
            updateData.setCountry("USA");

            when(addressRepository.findById(1L)).thenReturn(Optional.of(defaultAddress));
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Address result = addressService.save("john@example.com", updateData);

            // Assert
            assertThat(result.isDefaultForShipping()).isTrue(); // preserved from DB
            assertThat(result.getUser()).isEqualTo(testUser);
            verify(addressRepository).save(any(Address.class));
        }

        @Test
        @DisplayName("Should preserve non-default status when updating non-default address")
        void save_UpdateExisting_NonDefault() throws NotFoundException {
            // Arrange
            Address updateData = new Address();
            updateData.setId(2L);
            updateData.setAddressLine1("999 Updated St");
            updateData.setCity("Boston");
            updateData.setState("MA");
            updateData.setCountry("USA");

            when(addressRepository.findById(2L)).thenReturn(Optional.of(nonDefaultAddress));
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Address result = addressService.save("john@example.com", updateData);

            // Assert
            assertThat(result.isDefaultForShipping()).isFalse(); // preserved from DB
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found on save")
        void save_UserNotFound() {
            // Arrange
            Address newAddress = new Address();
            newAddress.setAddressLine1("789 Pine Rd");

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> addressService.save("notfound@example.com", newAddress))
                    .isInstanceOf(NotFoundException.class);
            verify(addressRepository, never()).save(any());
        }
    }

    // ======================== GET BY ID AND USER EMAIL ========================

    @Nested
    @DisplayName("getByIdAndUserEmail() Tests")
    class GetByIdAndUserEmailTests {

        @Test
        @DisplayName("Should return address when it belongs to user")
        void getByIdAndUserEmail_Success() throws NotFoundException, ConflictException {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(1L)).thenReturn(Optional.of(defaultAddress));

            // Act
            Address result = addressService.getByIdAndUserEmail(1L, "john@example.com");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw NotFoundException when address not found")
        void getByIdAndUserEmail_AddressNotFound() {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> addressService.getByIdAndUserEmail(999L, "john@example.com"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any address with id");
        }

        @Test
        @DisplayName("Should throw ConflictException when address does not belong to user")
        void getByIdAndUserEmail_NotBelongToUser() {
            // Arrange
            User otherUser = new User();
            otherUser.setId(2L);
            otherUser.setEmail("other@example.com");

            Address otherAddress = new Address();
            otherAddress.setId(1L);
            otherAddress.setUser(otherUser);

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(1L)).thenReturn(Optional.of(otherAddress));

            // Act & Assert
            assertThatThrownBy(() -> addressService.getByIdAndUserEmail(1L, "john@example.com"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("does not belong to the user");
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void getByIdAndUserEmail_UserNotFound() {
            // Arrange
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> addressService.getByIdAndUserEmail(1L, "notfound@example.com"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ======================== DELETE ========================

    @Nested
    @DisplayName("delete() Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete non-default address successfully")
        void delete_Success() throws NotFoundException, ConflictException {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(2L)).thenReturn(Optional.of(nonDefaultAddress));

            // Act
            addressService.delete(2L, "john@example.com");

            // Assert
            verify(addressRepository).deleteByIdAndUserId(nonDefaultAddress.getId(), testUser.getId());
        }

        @Test
        @DisplayName("Should throw ConflictException when deleting default address")
        void delete_DefaultAddress_ThrowsConflict() {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(1L)).thenReturn(Optional.of(defaultAddress));

            // Act & Assert
            assertThatThrownBy(() -> addressService.delete(1L, "john@example.com"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("being set as default so it cannot be deleted");
            verify(addressRepository, never()).deleteByIdAndUserId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should throw ConflictException when address does not belong to user")
        void delete_NotBelongToUser_ThrowsConflict() {
            // Arrange
            User otherUser = new User();
            otherUser.setId(2L);
            otherUser.setEmail("other@example.com");

            Address otherAddress = new Address();
            otherAddress.setId(3L);
            otherAddress.setUser(otherUser);
            otherAddress.setDefaultForShipping(false);

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(3L)).thenReturn(Optional.of(otherAddress));

            // Act & Assert
            assertThatThrownBy(() -> addressService.delete(3L, "john@example.com"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("does not belong to the user");
            verify(addressRepository, never()).deleteByIdAndUserId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should throw NotFoundException when address not found on delete")
        void delete_AddressNotFound() {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> addressService.delete(999L, "john@example.com"))
                    .isInstanceOf(NotFoundException.class);
            verify(addressRepository, never()).deleteByIdAndUserId(anyLong(), anyLong());
        }
    }

    // ======================== SET DEFAULT ========================

    @Nested
    @DisplayName("setDefault() Tests")
    class SetDefaultTests {

        @Test
        @DisplayName("Should set default address and clear others")
        void setDefault_Success() throws NotFoundException {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(2L)).thenReturn(Optional.of(nonDefaultAddress));

            // Act
            addressService.setDefault(2L, "john@example.com");

            // Assert
            verify(addressRepository).setDefaultAddress(2L);
            verify(addressRepository).setNonDefaultForOthers(2L, "john@example.com");
        }

        @Test
        @DisplayName("Should verify address ownership before setting default")
        void setDefault_VerifyOwnership() throws NotFoundException {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(2L)).thenReturn(Optional.of(nonDefaultAddress));

            // Act
            addressService.setDefault(2L, "john@example.com");

            // Assert — getByIdAndUserEmail was called (verified by findById mock)
            verify(addressRepository).findById(2L);
        }

        @Test
        @DisplayName("Should throw ConflictException when setting default for address not belonging to user")
        void setDefault_NotBelongToUser() {
            // Arrange
            User otherUser = new User();
            otherUser.setId(2L);
            otherUser.setEmail("other@example.com");

            Address otherAddress = new Address();
            otherAddress.setId(3L);
            otherAddress.setUser(otherUser);

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(3L)).thenReturn(Optional.of(otherAddress));

            // Act & Assert
            assertThatThrownBy(() -> addressService.setDefault(3L, "john@example.com"))
                    .isInstanceOf(ConflictException.class);
            verify(addressRepository, never()).setDefaultAddress(anyLong());
            verify(addressRepository, never()).setNonDefaultForOthers(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should still call setNonDefaultForOthers when addressId <= 0")
        void setDefault_ZeroOrNegativeId() throws NotFoundException {
            // Act — addressId = 0, skips the > 0 block but still calls setNonDefaultForOthers
            addressService.setDefault(0L, "john@example.com");

            // Assert
            verify(addressRepository, never()).setDefaultAddress(anyLong());
            verify(addressRepository).setNonDefaultForOthers(0L, "john@example.com");
        }

        @Test
        @DisplayName("Should call setNonDefaultForOthers even with negative id")
        void setDefault_NegativeId() throws NotFoundException {
            // Act
            addressService.setDefault(-1L, "john@example.com");

            // Assert
            verify(addressRepository, never()).setDefaultAddress(anyLong());
            verify(addressRepository).setNonDefaultForOthers(-1L, "john@example.com");
        }
    }

    // ======================== GET DEFAULT ADDRESS ========================

    @Nested
    @DisplayName("getDefaultAddress() Tests")
    class GetDefaultAddressTests {

        @Test
        @DisplayName("Should return default address when exists")
        void getDefaultAddress_Found() throws NotFoundException {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findDefaultByUserId(1L)).thenReturn(defaultAddress);

            // Act
            Address result = addressService.getDefaultAddress("john@example.com");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isDefaultForShipping()).isTrue();
            assertThat(result.getCity()).isEqualTo("New York");
        }

        @Test
        @DisplayName("Should return empty Address with user when no default exists")
        void getDefaultAddress_NoDefault_ReturnsEmptyAddress() throws NotFoundException {
            // Arrange
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(addressRepository.findDefaultByUserId(1L)).thenReturn(null);

            // Act
            Address result = addressService.getDefaultAddress("john@example.com");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.isDefaultForShipping()).isFalse();
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void getDefaultAddress_UserNotFound() {
            // Arrange
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> addressService.getDefaultAddress("notfound@example.com"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user have email: notfound@example.com");
        }
    }
}