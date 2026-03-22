package com.store.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String name;

    @Column(nullable = false)
    String logo;

    @ManyToMany
    @JoinTable(
            name = "brands_categories",
            joinColumns = @JoinColumn(name = "brand_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    Set<Category> categories = new HashSet<>();

    public Brand(Long id) {
        this.id = id;
    }

    public void addCategory(Category category) {
        categories.add(category);
    }

    @Transient
    public List<Long> getListCategoryIDs() {
        return categories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());
    }

    @Transient
    public String getNameOfCategories() {
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));
    }
}
