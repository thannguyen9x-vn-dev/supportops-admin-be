package com.supportops.api.modules.product.service;

import com.supportops.api.common.dto.PageMeta;
import com.supportops.api.common.exception.NotFoundException;
import com.supportops.api.common.exception.ValidationException;
import com.supportops.api.common.storage.ObjectStorageService;
import com.supportops.api.modules.product.dto.BulkDeleteProductsRequest;
import com.supportops.api.modules.product.dto.BulkDeleteProductsResponse;
import com.supportops.api.modules.product.dto.CreateProductRequest;
import com.supportops.api.modules.product.dto.ProductImageResponse;
import com.supportops.api.modules.product.dto.ProductListResponse;
import com.supportops.api.modules.product.dto.ProductResponse;
import com.supportops.api.modules.product.dto.ReorderProductImagesRequest;
import com.supportops.api.modules.product.dto.UpdateProductRequest;
import com.supportops.api.modules.product.entity.Product;
import com.supportops.api.modules.product.entity.ProductImage;
import com.supportops.api.modules.product.repository.ProductImageRepository;
import com.supportops.api.modules.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_IMAGES_PER_PRODUCT = 5;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ObjectStorageService objectStorageService;

    @Transactional(readOnly = true)
    public ProductPage list(UUID tenantId, int page, int size, String search, String category) {
        Pageable pageable = PageRequest.of(
            Math.max(0, page - 1),
            Math.max(1, size),
            Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<Product> result = productRepository.search(tenantId, normalize(search), normalize(category), pageable);

        List<ProductListResponse> data = result.getContent().stream()
            .map(this::toListResponse)
            .toList();

        PageMeta meta = new PageMeta(page, size, result.getTotalElements(), result.getTotalPages());
        return new ProductPage(data, meta);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID tenantId, UUID productId) {
        Product product = findProduct(tenantId, productId);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(UUID tenantId, UUID userId, CreateProductRequest request) {
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setCreatedBy(userId);
        applyProductData(product, request.name(), request.subtitle(), request.category(), request.brand(), request.price(), request.details());

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID tenantId, UUID productId, UpdateProductRequest request) {
        Product product = findProduct(tenantId, productId);
        applyProductData(product, request.name(), request.subtitle(), request.category(), request.brand(), request.price(), request.details());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID tenantId, UUID productId) {
        Product product = findProduct(tenantId, productId);
        for (ProductImage image : product.getImages()) {
            objectStorageService.deleteObjectByUrl(image.getUrl());
        }
        productRepository.delete(product);
    }

    @Transactional
    public BulkDeleteProductsResponse bulkDelete(UUID tenantId, BulkDeleteProductsRequest request) {
        List<Product> products = productRepository.findAllByTenantIdAndIdIn(tenantId, request.ids());
        for (Product product : products) {
            for (ProductImage image : product.getImages()) {
                objectStorageService.deleteObjectByUrl(image.getUrl());
            }
        }
        productRepository.deleteAll(products);
        return new BulkDeleteProductsResponse(products.size());
    }

    @Transactional
    public List<ProductImageResponse> uploadImages(UUID tenantId, UUID productId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ValidationException("At least one image file is required");
        }

        Product product = findProduct(tenantId, productId);
        List<ProductImage> currentImages = productImageRepository.findByProductIdAndTenantIdOrderBySortOrder(productId, tenantId);
        if (currentImages.size() + files.size() > MAX_IMAGES_PER_PRODUCT) {
            throw new ValidationException("Maximum 5 images per product");
        }

        int nextSortOrder = currentImages.stream()
            .map(ProductImage::getSortOrder)
            .max(Integer::compareTo)
            .orElse(0) + 1;

        for (MultipartFile file : files) {
            validateImageFile(file);

            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setSortOrder(nextSortOrder++);
            String objectKey = buildObjectKey(tenantId, product.getId(), file.getOriginalFilename());
            String imageUrl = uploadImageObject(objectKey, file);
            image.setUrl(imageUrl);
            productImageRepository.save(image);
        }

        return productImageRepository.findByProductIdAndTenantIdOrderBySortOrder(productId, tenantId).stream()
            .map(this::toImageResponse)
            .toList();
    }

    @Transactional
    public void deleteImage(UUID tenantId, UUID productId, UUID imageId) {
        findProduct(tenantId, productId);
        ProductImage image = productImageRepository.findScopedById(imageId, productId, tenantId)
            .orElseThrow(() -> new NotFoundException("Product image not found"));
        objectStorageService.deleteObjectByUrl(image.getUrl());
        productImageRepository.delete(image);
    }

    @Transactional
    public List<ProductImageResponse> reorderImages(UUID tenantId, UUID productId, ReorderProductImagesRequest request) {
        findProduct(tenantId, productId);
        List<ProductImage> current = productImageRepository.findByProductIdAndTenantIdOrderBySortOrder(productId, tenantId);
        if (current.isEmpty()) {
            throw new NotFoundException("Product images not found");
        }

        if (request.imageIds().size() != current.size()) {
            throw new ValidationException("imageIds must contain all existing product image ids");
        }

        Map<UUID, ProductImage> byId = new HashMap<>();
        for (ProductImage image : current) {
            byId.put(image.getId(), image);
        }

        if (request.imageIds().stream().distinct().count() != request.imageIds().size()) {
            throw new ValidationException("Duplicate image id found in reorder payload");
        }

        for (int i = 0; i < request.imageIds().size(); i++) {
            UUID imageId = request.imageIds().get(i);
            ProductImage image = byId.get(imageId);
            if (image == null) {
                throw new ValidationException("Invalid image id in reorder payload: " + imageId);
            }
            image.setSortOrder(i + 1);
        }

        productImageRepository.saveAll(current);

        return productImageRepository.findByProductIdAndTenantIdOrderBySortOrder(productId, tenantId).stream()
            .map(this::toImageResponse)
            .toList();
    }

    private Product findProduct(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantId(productId, tenantId)
            .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void applyProductData(Product product, String name, String subtitle, String category, String brand, BigDecimal price, String details) {
        product.setName(name.trim());
        product.setSubtitle(normalize(subtitle));
        product.setCategory(category.trim());
        product.setBrand(brand.trim());
        product.setPrice(price);
        product.setDetails(normalize(details));
    }

    private ProductListResponse toListResponse(Product product) {
        String thumbnail = product.getImages().isEmpty() ? null : product.getImages().getFirst().getUrl();
        return new ProductListResponse(
            product.getId(),
            product.getName(),
            product.getSubtitle(),
            product.getCategory(),
            product.getBrand(),
            product.getPrice(),
            thumbnail,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    private ProductResponse toResponse(Product product) {
        List<ProductImageResponse> images = product.getImages().stream()
            .map(this::toImageResponse)
            .toList();

        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getSubtitle(),
            product.getCategory(),
            product.getBrand(),
            product.getPrice(),
            product.getDetails(),
            images,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getUrl(), image.getSortOrder());
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Image file is empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ValidationException("Image size exceeds 5MB");
        }

        String contentType = normalize(file.getContentType());
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("Only JPG, PNG, WebP are supported");
        }
    }

    private String buildObjectKey(UUID tenantId, UUID productId, String originalFilename) {
        String fileName = normalize(originalFilename);
        if (fileName == null) {
            fileName = "image";
        }

        String safeFileName = fileName
            .replaceAll("[^a-zA-Z0-9._-]", "-")
            .replaceAll("-+", "-");

        return "products/" + tenantId + "/" + productId + "/" + UUID.randomUUID() + "-" + safeFileName;
    }

    private String uploadImageObject(String objectKey, MultipartFile file) {
        try {
            return objectStorageService.uploadPublicObject(objectKey, file.getBytes(), file.getContentType());
        } catch (IOException ex) {
            throw new ValidationException("Unable to read image content");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ProductPage(List<ProductListResponse> data, PageMeta meta) {
    }
}
