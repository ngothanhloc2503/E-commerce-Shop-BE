package com.store.ecommerce.entity;

import com.store.ecommerce.enums.AuthenticationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends AbstractAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String password;

    String photo;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    Date birthOfDate;

    boolean enabled;
    Date createdTime;
    String verificationCode;
    String resetPasswordToken;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "users_authentication_type")
    AuthenticationType authenticationType;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    Set<Role> roles = new HashSet<>();

    @NotFound(action = NotFoundAction.IGNORE)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, optional = true)
    Cart cart;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens;

    public void addRole(Role role) {
        this.roles.add(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", roles=" + roles +
                '}';
    }

    @Transient
    public String getFullName() {
        String first = this.firstName != null ? this.firstName : "";
        String last = this.lastName != null ? this.lastName : "";
        return (first + " " + last).trim();
    }

    public void setCart(Cart cart) {
        this.cart = cart;
        if (cart != null) {
            cart.setUser(this);
        }
    }

    public boolean hasRole(String roleName) {
        if (this.roles == null) {
            return false;
        }
        for (Role role : this.roles) {
            if (role.getName() != null && role.getName().equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        return false;
    }

    @Transient
    public List<String> getListRoles() {
        if (this.roles == null) {
            return Collections.emptyList();
        }
        return this.roles.stream()
                .map(role -> role.getName() != null ? role.getName().toUpperCase() : "")
                .collect(Collectors.toList());
    }
}
