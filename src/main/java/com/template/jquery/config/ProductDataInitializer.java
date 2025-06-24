package com.template.jquery.config;

import com.template.jquery.entity.Product;
import com.template.jquery.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class ProductDataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            log.info("Initializing sample product data for jQuery testing...");
            initializeProductData();
            log.info("Sample product data initialization completed!");
        } else {
            log.info("Product data already exists, skipping initialization");
        }
    }

    private void initializeProductData() {
        List<Product> products = List.of(
            Product.builder()
                .name("iPhone 15 Pro")
                .description("Latest Apple smartphone with A17 Pro chip")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .subCategory("Smartphones")
                .brand("Apple")
                .sku("APL-IP15P-128")
                .stockQuantity(50)
                .active(true)
                .featured(true)
                .launchDate(LocalDateTime.now().minusMonths(2))
                .weightKg(new BigDecimal("0.187"))
                .rating(new BigDecimal("4.8"))
                .reviewCount(1250)
                .tags("smartphone, apple, 5g, camera")
                .build(),

            Product.builder()
                .name("Samsung Galaxy S24 Ultra")
                .description("Flagship Android phone with S Pen")
                .price(new BigDecimal("1199.99"))
                .category("Electronics")
                .subCategory("Smartphones")
                .brand("Samsung")
                .sku("SAM-GS24U-256")
                .stockQuantity(30)
                .active(true)
                .featured(true)
                .launchDate(LocalDateTime.now().minusMonths(3))
                .weightKg(new BigDecimal("0.232"))
                .rating(new BigDecimal("4.7"))
                .reviewCount(890)
                .tags("android, samsung, s-pen, camera")
                .build(),

            Product.builder()
                .name("MacBook Air M3")
                .description("Ultra-thin laptop with M3 chip")
                .price(new BigDecimal("1299.99"))
                .category("Electronics")
                .subCategory("Laptops")
                .brand("Apple")
                .sku("APL-MBA-M3-512")
                .stockQuantity(25)
                .active(true)
                .featured(false)
                .launchDate(LocalDateTime.now().minusMonths(4))
                .weightKg(new BigDecimal("1.24"))
                .rating(new BigDecimal("4.9"))
                .reviewCount(567)
                .tags("laptop, apple, m3, ultrabook")
                .build(),

            Product.builder()
                .name("Nike Air Max 270")
                .description("Comfortable running shoes")
                .price(new BigDecimal("150.00"))
                .category("Clothing")
                .subCategory("Shoes")
                .brand("Nike")
                .sku("NIKE-AM270-BLK-10")
                .stockQuantity(100)
                .active(true)
                .featured(true)
                .launchDate(LocalDateTime.now().minusMonths(8))
                .weightKg(new BigDecimal("0.65"))
                .rating(new BigDecimal("4.4"))
                .reviewCount(2340)
                .tags("shoes, nike, running, sports")
                .build(),

            Product.builder()
                .name("Dyson V15 Detect")
                .description("Cordless vacuum with laser detection")
                .price(new BigDecimal("749.99"))
                .category("Home & Garden")
                .subCategory("Appliances")
                .brand("Dyson")
                .sku("DYS-V15D-ABS")
                .stockQuantity(20)
                .active(true)
                .featured(true)
                .launchDate(LocalDateTime.now().minusMonths(5))
                .weightKg(new BigDecimal("3.1"))
                .rating(new BigDecimal("4.7"))
                .reviewCount(789)
                .tags("vacuum, dyson, cordless, laser")
                .build()
        );

        productRepository.saveAll(products);
        log.info("Saved {} sample products", products.size());
    }
} 