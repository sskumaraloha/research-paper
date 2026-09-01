package com.store.app.category.entity;

import com.store.app.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A product category. Categories form an unlimited-depth tree via the
 * self-referencing {@code parentCategory} relationship (adjacency list):
 * a root category has no parent, a subcategory points at its parent.
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** URL-friendly unique identifier, generated from the name. */
    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    /** Image URL or path for category tiles. */
    @Column(name = "image", length = 255)
    private String image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory")
    @OrderBy("name ASC")
    private List<Category> children = new ArrayList<>();

    @Column(name = "active", nullable = false)
    private boolean active;

    public Category(String name, String slug, String description, String image,
                    Category parentCategory, boolean active) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.image = image;
        this.parentCategory = parentCategory;
        this.active = active;
    }
}
