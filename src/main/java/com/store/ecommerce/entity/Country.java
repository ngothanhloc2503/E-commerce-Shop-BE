package com.store.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.store.ecommerce.dto.request.CountryRequest;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String code;

    @JsonIgnore
    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL)
    Set<State> states;

    public Country(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Country(CountryRequest req) {
        this.id = req.getId();
        this.name = req.getName();
        this.code = req.getCode();
    }
}
