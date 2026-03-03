package com.supportops.api.modules.product.controller;

import com.supportops.api.common.dto.ApiResponse;
import com.supportops.api.common.security.CurrentUser;
import com.supportops.api.modules.product.dto.BulkDeleteProductsRequest;
import com.supportops.api.modules.product.dto.BulkDeleteProductsResponse;
import com.supportops.api.modules.product.dto.CreateProductRequest;
import com.supportops.api.modules.product.dto.ProductImageResponse;
import com.supportops.api.modules.product.dto.ProductListResponse;
import com.supportops.api.modules.product.dto.ProductResponse;
import com.supportops.api.modules.product.dto.ReorderProductImagesRequest;
import com.supportops.api.modules.product.dto.UpdateProductRequest;
import com.supportops.api.modules.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<ProductListResponse>> list(
        @AuthenticationPrincipal CurrentUser currentUser,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String category
    ) {
        ProductService.ProductPage result = productService.list(currentUser.tenantId(), page, size, search, category);
        return ApiResponse.of(result.data(), result.meta());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable UUID id
    ) {
        return ApiResponse.of(productService.getById(currentUser.tenantId(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ProductResponse> create(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody CreateProductRequest request
    ) {
        return ApiResponse.of(productService.create(currentUser.tenantId(), currentUser.userId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ProductResponse> update(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.of(productService.update(currentUser.tenantId(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable UUID id
    ) {
        productService.delete(currentUser.tenantId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<BulkDeleteProductsResponse> bulkDelete(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody BulkDeleteProductsRequest request
    ) {
        return ApiResponse.of(productService.bulkDelete(currentUser.tenantId(), request));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<ProductImageResponse>> uploadImages(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable UUID id,
        @RequestParam("files") List<MultipartFile> files
    ) {
        return ApiResponse.of(productService.uploadImages(currentUser.tenantId(), id, files));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteImage(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable UUID id,
        @PathVariable UUID imageId
    ) {
        productService.deleteImage(currentUser.tenantId(), id, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/images/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<ProductImageResponse>> reorderImages(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable UUID id,
        @Valid @RequestBody ReorderProductImagesRequest request
    ) {
        return ApiResponse.of(productService.reorderImages(currentUser.tenantId(), id, request));
    }
}
