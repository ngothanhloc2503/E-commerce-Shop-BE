package com.store.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false)
    String name;
    @Column(nullable = false)
    String description;
    @Column(nullable = false)
    String image;
    boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    Category parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("name asc")
    Set<Category> children = new HashSet<>();

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", image='" + image + '\'' +
                ", enabled=" + enabled + '\'' +
                ", parent=" + (parent != null ? parent.getId() : "null") + '\'' +
                ", children=[");
        List<Category> list = children.stream().toList();
        for (Category category : list) {
            result.append(category.getId()).append(",");
        }
        return result.append("]").toString();
    }

    @Transient
    public Long getParentID() {
        return parent != null ? parent.getId() : 0;
    }

    public Category(Long id) {
        this.id = id;
    }

    public Category(String name) {
        this.name = name;
    }

    @Transient
    public List<String> getListParentName() {
        List<String> result = new ArrayList<>();
        Category parent = this.parent;
        while (parent != null) {
            result.add(parent.getName());
            parent = parent.getParent();
        }
        Collections.reverse(result);
        return result;
    }

    @Transient
    public String getAllParentName() {
        StringBuilder result = new StringBuilder();
        Category parent = this.parent;
        while (parent != null) {
            result.append(parent.name).append(" ");
            parent = parent.getParent();
        }
        result.append(this.name);
        return result.toString().replace(' ' , '-');
    }
}
