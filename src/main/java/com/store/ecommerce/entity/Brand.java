package com.store.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<Long> listCategoryIDs = new ArrayList<>();
        categories.forEach(category -> {
            listCategoryIDs.add(category.getId());
        });
        return listCategoryIDs;
    }

    @Transient
    public String getNameOfCategories() {
        StringBuilder result = new StringBuilder();
        categories.forEach(category -> {
            result.append(category.getName()).append(", ");
        });
        return result.substring(0, result.length() - 2);
    }
}
