package com.store.app.product.controller;

import com.store.app.category.service.CategoryService;
import com.store.app.common.dto.PageResponse;
import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.FileStorageException;
import com.store.app.exception.InvalidFileException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.dto.ProductResponse;
import com.store.app.product.service.ProductService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin Thymeleaf pages for product management under /admin/products,
 * protected by ROLE_ADMIN through the web security chain.
 */
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductPageController {

    private static final String LIST_VIEW = "admin/products/list";
    private static final String FORM_VIEW = "admin/products/form";
    private static final String REDIRECT_LIST = "redirect:/admin/products";

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String listProducts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        PageResponse<ProductResponse> products =
                productService.searchProducts(search, sort, page, 10);
        model.addAttribute("products", products);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        return LIST_VIEW;
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("productRequest")) {
            model.addAttribute("productRequest", new ProductRequest());
        }
        prepareForm(model, null, "Create product", "/admin/products");
        return FORM_VIEW;
    }

    @PostMapping
    public String createProduct(
            @Valid @ModelAttribute("productRequest") ProductRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareForm(model, null, "Create product", "/admin/products");
            return FORM_VIEW;
        }

        ProductResponse created;
        try {
            created = productService.createProduct(request);
        } catch (DuplicateResourceException ex) {
            rejectBusinessError(bindingResult, ex.getField(), ex.getMessage());
            prepareForm(model, null, "Create product", "/admin/products");
            return FORM_VIEW;
        } catch (BusinessValidationException ex) {
            bindingResult.rejectValue("discountPrice", "invalid", ex.getMessage());
            prepareForm(model, null, "Create product", "/admin/products");
            return FORM_VIEW;
        } catch (ResourceNotFoundException ex) {
            bindingResult.rejectValue("categoryId", "notFound", ex.getMessage());
            prepareForm(model, null, "Create product", "/admin/products");
            return FORM_VIEW;
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Product \"" + created.productName() + "\" created. You can now add images.");
        return "redirect:/admin/products/" + created.id() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProductResponse product = productService.getProductById(id);
        if (!model.containsAttribute("productRequest")) {
            ProductRequest form = new ProductRequest();
            form.setProductName(product.productName());
            form.setDescription(product.description());
            form.setCategoryId(product.categoryId());
            form.setBrand(product.brand());
            form.setSku(product.sku());
            form.setPrice(product.price());
            form.setDiscountPrice(product.discountPrice());
            form.setCostPrice(product.costPrice());
            form.setStockQuantity(product.stockQuantity());
            form.setMinimumStockLevel(product.minimumStockLevel());
            form.setActive(product.active());
            model.addAttribute("productRequest", form);
        }
        prepareForm(model, product, "Edit product", "/admin/products/" + id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute("productRequest") ProductRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        ProductResponse product = productService.getProductById(id);
        if (bindingResult.hasErrors()) {
            prepareForm(model, product, "Edit product", "/admin/products/" + id);
            return FORM_VIEW;
        }

        try {
            ProductResponse updated = productService.updateProduct(id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product \"" + updated.productName() + "\" updated.");
        } catch (DuplicateResourceException ex) {
            rejectBusinessError(bindingResult, ex.getField(), ex.getMessage());
            prepareForm(model, product, "Edit product", "/admin/products/" + id);
            return FORM_VIEW;
        } catch (BusinessValidationException ex) {
            bindingResult.rejectValue("discountPrice", "invalid", ex.getMessage());
            prepareForm(model, product, "Edit product", "/admin/products/" + id);
            return FORM_VIEW;
        } catch (ResourceNotFoundException ex) {
            bindingResult.rejectValue("categoryId", "notFound", ex.getMessage());
            prepareForm(model, product, "Edit product", "/admin/products/" + id);
            return FORM_VIEW;
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/toggle")
    public String toggleProduct(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            ProductResponse current = productService.getProductById(id);
            ProductResponse updated = productService.setProductActive(id, !current.active());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product \"" + updated.productName() + "\" is now "
                            + (updated.active() ? "active" : "inactive") + ".");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/images")
    public String uploadImage(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            productService.addProductImage(id, file);
            redirectAttributes.addFlashAttribute("successMessage", "Image uploaded.");
        } catch (InvalidFileException | FileStorageException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id,
                              @PathVariable Long imageId,
                              RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProductImage(id, imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Image removed.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    private void prepareForm(Model model, ProductResponse editing,
                             String title, String action) {
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
        model.addAttribute("categoryOptions", categoryService.getAllCategories());
        model.addAttribute("editingProduct", editing);
    }

    private void rejectBusinessError(BindingResult bindingResult, String field, String message) {
        if (field != null) {
            bindingResult.rejectValue(field, "duplicate", message);
        } else {
            bindingResult.reject("duplicate", message);
        }
    }
}
