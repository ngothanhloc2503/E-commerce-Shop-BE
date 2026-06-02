package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.RegisterRequest;
import com.store.ecommerce.entity.Role;
import com.store.ecommerce.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void testToUserDTO_AllFieldsMapped() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoto("profile.jpg");
        user.setPhoneNumber("+1234567890");
        user.setAddressLine1("123 Main St");
        user.setAddressLine2("Apt 4B");
        user.setCity("New York");
        user.setState("NY");
        user.setCountry("USA");
        user.setPostalCode("10001");
        user.setEnabled(true);
        user.setBirthOfDate(new Date());

        Set<Role> roles = new HashSet<>();
        Role role = new Role();
        role.setId(1);
        role.setName("USER");
        roles.add(role);
        user.setRoles(roles);

        UserDTO userDTO = userMapper.toUserDTO(user);

        assertNotNull(userDTO);
        assertEquals(1L, userDTO.getId());
        assertEquals("test@example.com", userDTO.getEmail());
        assertEquals("John", userDTO.getFirstName());
        assertEquals("Doe", userDTO.getLastName());
        assertEquals("profile.jpg", userDTO.getPhoto());
        assertEquals("+1234567890", userDTO.getPhoneNumber());
        assertEquals("123 Main St", userDTO.getAddressLine1());
        assertEquals("Apt 4B", userDTO.getAddressLine2());
        assertEquals("New York", userDTO.getCity());
        assertEquals("NY", userDTO.getState());
        assertEquals("USA", userDTO.getCountry());
        assertEquals("10001", userDTO.getPostalCode());
        assertTrue(userDTO.isEnabled());
        assertEquals(1, userDTO.getRoles().size()); // Giả sử DTO có field roles
        assertEquals("USER", userDTO.getRoles().iterator().next().getName());
    }

    @Test
    void testToUserDTO_NullInput() {
        UserDTO userDTO = userMapper.toUserDTO(null);
        assertNull(userDTO);
    }

    @Test
    void testToUserDTO_NullFields() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        UserDTO userDTO = userMapper.toUserDTO(user);

        assertNotNull(userDTO);
        assertEquals(1L, userDTO.getId());
        assertEquals("test@example.com", userDTO.getEmail());
        assertNull(userDTO.getFirstName());
        assertNull(userDTO.getLastName());
        assertNull(userDTO.getPhoto());
    }

    @Test
    void testToUser_FromUserDTO() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setEmail("test@example.com");
        userDTO.setFirstName("Jane");
        userDTO.setLastName("Smith");
        userDTO.setPhoto("avatar.png");
        userDTO.setPhoneNumber("+0987654321");
        userDTO.setAddressLine1("456 Oak Ave");
        userDTO.setAddressLine2("Suite 100");
        userDTO.setCity("Los Angeles");
        userDTO.setState("CA");
        userDTO.setCountry("USA");
        userDTO.setPostalCode("90001");
        userDTO.setEnabled(true);
        userDTO.setBirthOfDate(new Date());

        User user = userMapper.toUser(userDTO);

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("avatar.png", user.getPhoto());
        assertEquals("+0987654321", user.getPhoneNumber());
        assertEquals("456 Oak Ave", user.getAddressLine1());
        assertEquals("Suite 100", user.getAddressLine2());
        assertEquals("Los Angeles", user.getCity());
        assertEquals("CA", user.getState());
        assertEquals("USA", user.getCountry());
        assertEquals("90001", user.getPostalCode());
        assertTrue(user.isEnabled());
    }

    @Test
    void testUpdateUserFromRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("Alice");
        request.setLastName("Johnson");
        request.setPhoneNumber("+1122334455");
        request.setAddressLine1("789 Pine Rd");
        request.setCity("Chicago");
        request.setState("IL");
        request.setCountry("USA");
        request.setPostalCode("60601");
        request.setBirthOfDate(new Date());

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");
        existingUser.setPassword("oldPassword");

        userMapper.updateUserFromRequest(request, existingUser);

        assertEquals("newuser@example.com", existingUser.getEmail());
        assertEquals("oldPassword", existingUser.getPassword()); // Password không bị ghi đè
        assertEquals("Alice", existingUser.getFirstName());
        assertEquals("Johnson", existingUser.getLastName());
        assertEquals("+1122334455", existingUser.getPhoneNumber());
        assertEquals("789 Pine Rd", existingUser.getAddressLine1());
        assertEquals("Chicago", existingUser.getCity());
        assertEquals("IL", existingUser.getState());
        assertEquals("USA", existingUser.getCountry());
        assertEquals("60601", existingUser.getPostalCode());
    }

    @Test
    void testToUserDTO_ListMapping() {
        List<User> users = new ArrayList<>();
        User user1 = new User(); user1.setId(1L); user1.setEmail("user1@example.com"); user1.setFirstName("User"); user1.setLastName("One"); users.add(user1);
        User user2 = new User(); user2.setId(2L); user2.setEmail("user2@example.com"); user2.setFirstName("User"); user2.setLastName("Two"); users.add(user2);

        List<UserDTO> userDTOs = userMapper.toUserDTOList(users);

        assertNotNull(userDTOs);
        assertEquals(2, userDTOs.size());
        assertEquals("user1@example.com", userDTOs.get(0).getEmail());
        assertEquals("user2@example.com", userDTOs.get(1).getEmail());
    }

    @Test
    void testToUserDTO_EmptyList() {
        List<User> users = new ArrayList<>();
        List<UserDTO> userDTOs = userMapper.toUserDTOList(users);

        assertNotNull(userDTOs);
        assertTrue(userDTOs.isEmpty());
    }

    @Test
    void testToUserDTO_WithNullRoles() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRoles(null);

        // When
        UserDTO userDTO = userMapper.toUserDTO(user);

        // Then
        assertNotNull(userDTO);
        assertEquals(1L, userDTO.getId());
        assertEquals("test@example.com", userDTO.getEmail());

        assertNotNull(userDTO.getRoles());
        assertTrue(userDTO.getRoles().isEmpty());

    }
}