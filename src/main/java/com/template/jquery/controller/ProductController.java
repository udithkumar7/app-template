package com.template.jquery.controller;

import com.template.jquery.dto.DataTableRequest;
import com.template.jquery.dto.DataTableResponse;
import com.template.jquery.dto.ProductFilterRequest;
import com.template.jquery.entity.Product;
import com.template.jquery.repository.ProductRepository;
import com.template.jquery.service.ProductQueryService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/jquery/products")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ProductController {

    private final ProductQueryService productQueryService;
    private final ProductRepository productRepository;

    /**
     * DataTables server-side processing endpoint
     * This endpoint is specifically designed for jQuery DataTables integration
     */
    @PostMapping("/datatable")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<DataTableResponse<Product>> getProductsDataTable(@Valid @RequestBody DataTableRequest request) {
        log.info("DataTable request received: draw={}, start={}, length={}, search={}", 
                request.getDraw(), request.getStart(), request.getLength(), request.getSearchValue());
        
        DataTableResponse<Product> response = productQueryService.getProductsDataTable(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Advanced filtering endpoint with custom criteria
     */
    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<Page<Product>> getProductsWithFilters(
            @Valid @RequestBody ProductFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.info("Advanced filter request: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Product> products = productQueryService.findWithAdvancedFilters(filter, pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Get all products with pagination and sorting
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Product> products = productRepository.findAll(pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Get product by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search products by global search term
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String q) {
        List<Product> products = productRepository.findByGlobalSearch(q);
        return ResponseEntity.ok(products);
    }

    /**
     * Get products by category
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productRepository.findByCategory(category);
        return ResponseEntity.ok(products);
    }

    /**
     * Get products by brand
     */
    @GetMapping("/brand/{brand}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<Product>> getProductsByBrand(@PathVariable String brand) {
        List<Product> products = productRepository.findByBrand(brand);
        return ResponseEntity.ok(products);
    }

    /**
     * Get products in price range
     */
    @GetMapping("/price-range")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<Product>> getProductsByPriceRange(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        
        List<Product> products;
        if (minPrice != null && maxPrice != null) {
            products = productRepository.findByPriceBetween(minPrice, maxPrice);
        } else if (minPrice != null) {
            products = productRepository.findByPriceGreaterThanEqual(minPrice);
        } else if (maxPrice != null) {
            products = productRepository.findByPriceLessThanEqual(maxPrice);
        } else {
            products = productRepository.findAll();
        }
        
        return ResponseEntity.ok(products);
    }

    /**
     * Get active products only
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<Product>> getActiveProducts() {
        List<Product> products = productRepository.findByActiveTrue();
        return ResponseEntity.ok(products);
    }

    /**
     * Get featured products only
     */
    @GetMapping("/featured")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<Product>> getFeaturedProducts() {
        List<Product> products = productRepository.findByFeaturedTrue();
        return ResponseEntity.ok(products);
    }

    /**
     * Get product statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        Map<String, Object> stats = Map.of(
            "totalProducts", productRepository.count(),
            "activeProducts", productRepository.countByActiveTrue(),
            "featuredProducts", productRepository.countByFeaturedTrue(),
            "averagePrice", productRepository.getAveragePrice(),
            "totalStock", productRepository.getTotalStock(),
            "categoryCount", productRepository.getActiveCategoryCount(),
            "brandCount", productRepository.getActiveBrandCount(),
            "lowStockProducts", productRepository.countLowStockProducts(10)
        );
        return ResponseEntity.ok(stats);
    }

    /**
     * Create new product
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product savedProduct = productRepository.save(product);
        return ResponseEntity.ok(savedProduct);
    }

    /**
     * Update existing product
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        product.setId(id);
        Product updatedProduct = productRepository.save(product);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Delete product
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk operations endpoint for jQuery integration
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkOperations(@RequestBody Map<String, Object> request) {
        String operation = (String) request.get("operation");
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) request.get("ids");
        
        int affectedRows = 0;
        
        switch (operation.toLowerCase()) {
            case "activate":
                for (Long id : ids) {
                    productRepository.findById(id).ifPresent(product -> {
                        product.setActive(true);
                        productRepository.save(product);
                    });
                }
                affectedRows = ids.size();
                break;
                
            case "deactivate":
                for (Long id : ids) {
                    productRepository.findById(id).ifPresent(product -> {
                        product.setActive(false);
                        productRepository.save(product);
                    });
                }
                affectedRows = ids.size();
                break;
                
            case "delete":
                productRepository.deleteAllById(ids);
                affectedRows = ids.size();
                break;
                
            default:
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid operation"));
        }
        
        return ResponseEntity.ok(Map.of(
            "operation", operation,
            "affectedRows", affectedRows,
            "success", true
        ));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'USER')")
    public void exportProductsToExcel(
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir,
        @RequestParam(required = false) String globalSearch,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String brand,
        HttpServletResponse response
    ) throws IOException {
        // Build filter object
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setGlobalSearch("undefined".equals(globalSearch) ? null : globalSearch);
        filter.setCategory("undefined".equals(category) ? null : category);
        filter.setBrand("undefined".equals(brand) ? null : brand);

        // Build sort
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);

        // Fetch all filtered products (no pagination)
        List<Product> products = productQueryService.findAllWithAdvancedFilters(filter, sort);

        // Generate Excel file using Apache POI
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");
        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        String[] columns = {"ID", "Name", "Description", "Category", "Brand", "Price", "Stock", "Active", "Featured"};
        for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);

        for (Product p : products) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getName());
            row.createCell(2).setCellValue(p.getDescription());
            row.createCell(3).setCellValue(p.getCategory());
            row.createCell(4).setCellValue(p.getBrand());
            row.createCell(5).setCellValue(p.getPrice() != null ? p.getPrice().doubleValue() : 0);
            row.createCell(6).setCellValue(p.getStockQuantity());
            row.createCell(7).setCellValue(p.getActive());
            row.createCell(8).setCellValue(p.getFeatured());
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=products.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
} 