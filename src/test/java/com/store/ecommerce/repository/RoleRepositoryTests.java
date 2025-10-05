package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
@RequiredArgsConstructor
public class RoleRepositoryTests {
    private final RoleRepository repository;

    @Test
    public void testCreateFirstRole() {
        Role role = new Role("ADMIN");
        Role savedRole = repository.save(role);

        assertThat(savedRole.getId()).isGreaterThan(0);
    }

    @Test
    public void testGetListRole() {
        List<Role> listRoles = repository.findAll();

        assertThat(listRoles.size()).isGreaterThan(0);

        for (Role role : listRoles) {
            System.out.println(role);
        }
    }
}
