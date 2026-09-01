package com.store.app.category.service.impl;

import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.dto.CategoryResponse;
import com.store.app.category.dto.CategoryTreeResponse;
import com.store.app.category.entity.Category;
import com.store.app.category.mapper.CategoryMapper;
import com.store.app.category.repository.CategoryRepository;
import com.store.app.category.service.CategoryService;
import com.store.app.exception.InvalidCategoryHierarchyException;
import com.store.app.exception.OperationNotAllowedException;
import com.store.app.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        Category parent = resolveParent(request.getParentId());
        String slug = generateUniqueSlug(request.getName(), null);

        Category category = new Category(
                request.getName().trim(),
                slug,
                normalize(request.getDescription()),
                normalize(request.getImage()),
                parent,
                request.isActive()
        );
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        Category parent = resolveParent(request.getParentId());
        assertValidParent(category, parent);

        String newName = request.getName().trim();
        if (!category.getName().equals(newName)) {
            category.setSlug(generateUniqueSlug(newName, id));
        }

        category.setName(newName);
        category.setDescription(normalize(request.getDescription()));
        category.setImage(normalize(request.getImage()));
        category.setParentCategory(parent);
        category.setActive(request.isActive());

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        if (categoryRepository.existsByParentCategoryId(id)) {
            throw new OperationNotAllowedException(
                    "Cannot delete category \"" + category.getName()
                            + "\" while it has subcategories. Delete or move them first.");
        }
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse setCategoryActive(Long id, boolean active) {
        Category category = findCategory(id);
        category.setActive(active);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getParentOptions(Long excludeId) {
        List<Category> all = categoryRepository.findAllByOrderByNameAsc();
        if (excludeId == null) {
            return all.stream().map(categoryMapper::toResponse).toList();
        }

        Set<Long> excluded = collectSubtreeIds(all, excludeId);
        return all.stream()
                .filter(category -> !excluded.contains(category.getId()))
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getActiveCategoryTree() {
        return categoryRepository.findAllByParentCategoryIsNullAndActiveTrueOrderByNameAsc()
                .stream()
                .map(categoryMapper::toTree)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryTreeResponse getActiveCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + slug));
        return categoryMapper.toTree(category);
    }

    // ------------------------------------------------------------------

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + id));
    }

    private Category resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent category not found with id: " + parentId));
    }

    /**
     * Rejects a parent that is the category itself or any of its
     * descendants — either would create a cycle and detach the subtree.
     */
    private void assertValidParent(Category category, Category parent) {
        Category current = parent;
        while (current != null) {
            if (Objects.equals(current.getId(), category.getId())) {
                throw new InvalidCategoryHierarchyException(
                        "A category cannot be its own parent or a descendant of itself");
            }
            current = current.getParentCategory();
        }
    }

    /** Ids of the given category plus all its descendants. */
    private Set<Long> collectSubtreeIds(List<Category> all, Long rootId) {
        Set<Long> subtree = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.push(rootId);
        while (!pending.isEmpty()) {
            Long currentId = pending.pop();
            if (!subtree.add(currentId)) {
                continue;
            }
            all.stream()
                    .filter(c -> c.getParentCategory() != null
                            && currentId.equals(c.getParentCategory().getId()))
                    .forEach(c -> pending.push(c.getId()));
        }
        return subtree;
    }

    private String generateUniqueSlug(String name, Long currentId) {
        String base = slugify(name);
        String candidate = base;
        int counter = 2;
        while (slugTaken(candidate, currentId)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private boolean slugTaken(String slug, Long currentId) {
        return currentId == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, currentId);
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "category" : slug;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
