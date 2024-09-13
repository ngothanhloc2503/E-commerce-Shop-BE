package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Role;
import com.store.ecommerce.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
public class UserRepositoryTests {
    @Autowired
    private UserRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testCreateFirstUser() {
        User user = new User();
        user.setEmail("admin@admin.com");

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "password";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(encodedPassword);

        user.setFirstName("admin");
        user.setLastName("admin");
        user.setEnabled(true);
        user.setPhoto("");
        user.setPhoneNumber("0000000000");
        user.setBirthOfDate(new Date());
        user.setCreatedTime(new Date());

        Role admin = entityManager.find(Role.class, 1);
        user.addRole(admin);

        User savedUser = repository.save(user);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isGreaterThan(0);
    }

    @Test
    public void testGetListAllUser() {
        List<User> listUsers = repository.findAll();

        assertThat(listUsers.size()).isGreaterThan(0);

        for (User user : listUsers) {
            System.out.println(user);
        }
    }

    @Test
    public void testFindByEmail() {
        User user = repository.findByEmail("test@test.com").get();

        assertThat(user.getId()).isGreaterThan(0);
        assertThat(user.getEmail()).isEqualTo("test@test.com");
    }
}
