package com.store.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.store.ecommerce.dto.StateDTO;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class State {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "country_id")
    Country country;

    public State(String name, Country country) {
        this.name = name;
        this.country = country;
    }

    public StateDTO toStateDTO() {
        return new StateDTO(this.id, this.name, this.country.getId());
    }
}
