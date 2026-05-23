package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("RoleRepository Integration Tests")
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        persistRole("ROLE_CUSTOMER");
        persistRole("ROLE_ADMIN");
        persistRole("ROLE_SHIPPER");
    }

    // ======================== FIND BY NAME ========================

    @Test
    @DisplayName("Should find role by name when exists")
    void findByName_Found() {
        // Act
        Role result = roleRepository.findByName("ROLE_CUSTOMER");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should find ROLE_ADMIN by name")
    void findByName_AdminRole() {
        // Act
        Role result = roleRepository.findByName("ROLE_ADMIN");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should find ROLE_SHIPPER by name")
    void findByName_ShipperRole() {
        // Act
        Role result = roleRepository.findByName("ROLE_SHIPPER");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ROLE_SHIPPER");
    }

    @Test
    @DisplayName("Should return null when role name not found")
    void findByName_NotFound() {
        // Act
        Role result = roleRepository.findByName("ROLE_NONEXISTENT");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should find role by exact name case-sensitive")
    void findByName_CaseSensitive() {
        // Act
        Role result = roleRepository.findByName("role_customer"); // lowercase

        // Assert — JPA default is case-sensitive
        assertThat(result).isNull();
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve role")
    void save_AndFindById() {
        // Arrange
        Role role = new Role();
        role.setName("ROLE_EDITOR");

        // Act
        Role saved = roleRepository.save(role);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getName()).isEqualTo("ROLE_EDITOR");
    }

    @Test
    @DisplayName("Should update existing role name")
    void save_UpdateExisting() {
        // Arrange
        Role role = roleRepository.findByName("ROLE_SHIPPER");
        role.setName("ROLE_DELIVERY");

        // Act
        roleRepository.save(role);

        // Assert
        entityManager.flush();
        entityManager.clear();

        Role updated = entityManager.find(Role.class, role.getId());
        assertThat(updated.getName()).isEqualTo("ROLE_DELIVERY");
        // Old name should no longer exist
        assertThat(roleRepository.findByName("ROLE_SHIPPER")).isNull();
    }

    @Test
    @DisplayName("Should delete role by id")
    void deleteById_Success() {
        // Arrange
        Role role = roleRepository.findByName("ROLE_SHIPPER");
        Integer roleId = role.getId();

        // Act
        roleRepository.deleteById(roleId);

        // Assert
        Optional<Role> found = roleRepository.findById(roleId);
        assertThat(found).isEmpty();
        assertThat(roleRepository.findByName("ROLE_SHIPPER")).isNull();
    }

    @Test
    @DisplayName("Should count roles correctly")
    void count_Success() {
        // 3 from setUp
        assertThat(roleRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find all roles")
    void findAll_Success() {
        // Act
        List<Role> roles = roleRepository.findAll();

        // Assert
        assertThat(roles).hasSize(3);
        assertThat(roles).extracting(Role::getName)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN", "ROLE_SHIPPER");
    }

    // ======================== HELPER METHODS ========================

    private Role persistRole(String name) {
        Role role = new Role();
        role.setName(name);
        return entityManager.persistAndFlush(role);
    }
}