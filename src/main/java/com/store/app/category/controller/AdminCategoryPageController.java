package com.store.app.category.controller;

import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.dto.CategoryResponse;
import com.store.app.category.service.CategoryService;
import com.store.app.exception.InvalidCategoryHierarchyException;
import com.store.app.exception.OperationNotAllowedException;
import com.store.app.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin Thymeleaf pages for category management under /admin/categories,
 * protected by ROLE_ADMIN through the web security chain.
 */
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryPageController {

    private static final String LIST_VIEW = "admin/categories/list";
    private static final String FORM_VIEW = "admin/categories/form";
    private static final String REDIRECT_LIST = "redirect:/admin/categories";

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("categoryRequest")) {
            model.addAttribute("categoryRequest", new CategoryRequest());
        }
        prepareForm(model, null, "Create category", "/admin/categories");
        return FORM_VIEW;
    }

    @PostMapping
    public String createCategory(
            @Valid @ModelAttribute("categoryRequest") CategoryRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareForm(model, null, "Create category", "/admin/categories");
            return FORM_VIEW;
        }

        try {
            CategoryResponse created = categoryService.createCategory(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category \"" + created.name() + "\" created.");
        } catch (ResourceNotFoundException ex) {
            bindingResult.rejectValue("parentId", "notFound", ex.getMessage());
            prepareForm(model, null, "Create category", "/admin/categories");
            return FORM_VIEW;
        }
        return REDIRECT_LIST;
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("categoryRequest")) {
            CategoryResponse category = categoryService.getCategoryById(id);
            CategoryRequest form = new CategoryRequest();
            form.setName(category.name());
            form.setDescription(category.description());
            form.setImage(category.image());
            form.setParentId(category.parentId());
            form.setActive(category.active());
            model.addAttribute("categoryRequest", form);
        }
        prepareForm(model, id, "Edit category", "/admin/categories/" + id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryRequest") CategoryRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareForm(model, id, "Edit category", "/admin/categories/" + id);
            return FORM_VIEW;
        }

        try {
            CategoryResponse updated = categoryService.updateCategory(id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category \"" + updated.name() + "\" updated.");
        } catch (InvalidCategoryHierarchyException ex) {
            bindingResult.rejectValue("parentId", "invalidHierarchy", ex.getMessage());
            prepareForm(model, id, "Edit category", "/admin/categories/" + id);
            return FORM_VIEW;
        } catch (ResourceNotFoundException ex) {
            bindingResult.reject("notFound", ex.getMessage());
            prepareForm(model, id, "Edit category", "/admin/categories/" + id);
            return FORM_VIEW;
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted.");
        } catch (OperationNotAllowedException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/toggle")
    public String toggleCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            CategoryResponse current = categoryService.getCategoryById(id);
            CategoryResponse updated =
                    categoryService.setCategoryActive(id, !current.active());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category \"" + updated.name() + "\" is now "
                            + (updated.active() ? "active" : "inactive") + ".");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_LIST;
    }

    private void prepareForm(Model model, Long editingId, String title, String action) {
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
        model.addAttribute("parentOptions", categoryService.getParentOptions(editingId));
    }
}
