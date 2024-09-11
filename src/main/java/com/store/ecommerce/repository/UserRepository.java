package com.store.ecommerce.repository;

import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.AuthenticationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,
        SearchRepository<User, Long> {
    public Optional<User> findByEmail(String email);

    @Query("UPDATE User u SET u.enabled = ?2 WHERE id = ?1")
    @Modifying
    public void updateUserEnabledStatus(Long id, boolean enabled);

    public Page<User> findAll(Pageable pageable);

    @Query("SELECT u FROM User u WHERE CONCAT(u.id, ' ', u.email, ' ', u.firstName, ' ', u.lastName) LIKE %?1%")
    public List<User> findAll(String keyword, Sort sort);

    @Query("SELECT u FROM User u WHERE CONCAT(u.id, ' ', u.email, ' ', u.firstName, ' ', u.lastName) LIKE %?1%")
    public Page<User> findAll(String keyword, Pageable pageable);

    User findByVerificationCode(String verificationCode);

    @Query("UPDATE User u SET u.enabled = true WHERE u.id = ?1")
    @Modifying
    void enableUserByID(Long id);

    @Query("UPDATE User u SET u.authenticationType = ?2 WHERE u.id = ?1")
    @Modifying
    void updateAuthenticationType(Long id, AuthenticationType authenticationType);

    User findByResetPasswordToken(String token);
}
