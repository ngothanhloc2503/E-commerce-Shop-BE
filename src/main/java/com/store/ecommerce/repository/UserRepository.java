package com.store.ecommerce.repository;

import com.store.ecommerce.entity.User;
import com.store.ecommerce.enums.AuthenticationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByVerificationCode(String verificationCode);

    Optional<User> findByResetPasswordToken(String token);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE CONCAT(u.id, ' ', u.email, ' ', u.firstName, ' ', u.lastName) LIKE %:keyword%")
    List<User> searchByKeyword(@Param("keyword") String keyword, Sort sort);

    @Query("SELECT u FROM User u WHERE CONCAT(u.id, ' ', u.email, ' ', u.firstName, ' ', u.lastName) LIKE %:keyword%")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.enabled = true WHERE u.id = :id")
    void enableUserByID(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE users SET authentication_type = CAST(:authenticationType AS users_authentication_type) WHERE id = :id", nativeQuery = true)
    void updateAuthenticationType(@Param("id") Long id, @Param("authenticationType") String authenticationType);

    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :id")
    void updateUserEnabledStatus(@Param("id") Long id, @Param("enabled") boolean enabled);
}