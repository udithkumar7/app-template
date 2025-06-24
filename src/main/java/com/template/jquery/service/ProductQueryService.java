package com.template.jquery.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.template.jquery.dto.DataTableRequest;
import com.template.jquery.dto.DataTableResponse;
import com.template.jquery.dto.ProductFilterRequest;
import com.template.jquery.entity.Product;
import com.template.jquery.entity.QProduct;
import com.template.jquery.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductQueryService {

    private final JPAQueryFactory queryFactory;
    private final ProductRepository productRepository;
    private final QProduct qProduct = QProduct.product;

    /**
     * Get products with DataTable support (pagination, sorting, filtering)
     */
    public DataTableResponse<Product> getProductsDataTable(DataTableRequest request) {
        try {
            // Build base query
            JPAQuery<Product> query = queryFactory.selectFrom(qProduct);
            
            // Apply global search if present
            if (StringUtils.isNotBlank(request.getSearchValue())) {
                BooleanExpression globalSearch = buildGlobalSearchExpression(request.getSearchValue());
                query.where(globalSearch);
            }
            
            // Apply column-specific searches
            BooleanBuilder columnFilters = buildColumnFilters(request);
            if (columnFilters.hasValue()) {
                query.where(columnFilters);
            }
            
            // Get total count for filtering
            long filteredCount = query.fetchCount();
            
            // Apply sorting
            OrderSpecifier<?>[] orderSpecifiers = buildOrderSpecifiers(request);
            if (orderSpecifiers.length > 0) {
                query.orderBy(orderSpecifiers);
            }
            
            // Apply pagination
            Pageable pageable = request.toPageable();
            query.offset(pageable.getOffset()).limit(pageable.getPageSize());
            
            // Execute query
            List<Product> products = query.fetch();
            
            // Get total count (without filtering)
            long totalCount = productRepository.count();
            
            // Create page object
            Page<Product> page = new PageImpl<>(products, pageable, filteredCount);
            
            return DataTableResponse.of(page, request.getDraw(), totalCount);
            
        } catch (Exception e) {
            log.error("Error fetching products for DataTable", e);
            return DataTableResponse.error(request.getDraw(), "Error fetching products: " + e.getMessage());
        }
    }

    /**
     * Advanced filtering with custom criteria
     */
    public Page<Product> findWithAdvancedFilters(ProductFilterRequest filter, Pageable pageable) {
        JPAQuery<Product> query = queryFactory.selectFrom(qProduct);
        
        BooleanBuilder whereClause = buildAdvancedFilterExpression(filter);
        if (whereClause.hasValue()) {
            query.where(whereClause);
        }
        
        // Get total count
        long total = query.fetchCount();
        
        // Apply pagination and sorting
        query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        
        // Apply sorting from Pageable
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(order.getProperty(), order.getDirection().name());
                if (orderSpecifier != null) {
                    query.orderBy(orderSpecifier);
                }
            });
        }
        
        List<Product> products = query.fetch();
        
        return new PageImpl<>(products, pageable, total);
    }

    /**
     * Build global search expression (searches across multiple fields)
     */
    private BooleanExpression buildGlobalSearchExpression(String searchTerm) {
        String searchValue = "%" + searchTerm.toLowerCase() + "%";
        
        return qProduct.name.lower().like(searchValue)
                .or(qProduct.description.lower().like(searchValue))
                .or(qProduct.category.lower().like(searchValue))
                .or(qProduct.subCategory.lower().like(searchValue))
                .or(qProduct.brand.lower().like(searchValue))
                .or(qProduct.sku.lower().like(searchValue))
                .or(qProduct.tags.lower().like(searchValue));
    }

    /**
     * Build column-specific filters from DataTable request
     */
    private BooleanBuilder buildColumnFilters(DataTableRequest request) {
        BooleanBuilder builder = new BooleanBuilder();
        
        // Name filter
        String nameFilter = request.getColumnSearchValue("name");
        if (StringUtils.isNotBlank(nameFilter)) {
            builder.and(qProduct.name.lower().like("%" + nameFilter.toLowerCase() + "%"));
        }
        
        // Category filter
        String categoryFilter = request.getColumnSearchValue("category");
        if (StringUtils.isNotBlank(categoryFilter)) {
            builder.and(qProduct.category.lower().like("%" + categoryFilter.toLowerCase() + "%"));
        }
        
        // Brand filter
        String brandFilter = request.getColumnSearchValue("brand");
        if (StringUtils.isNotBlank(brandFilter)) {
            builder.and(qProduct.brand.lower().like("%" + brandFilter.toLowerCase() + "%"));
        }
        
        // Active filter
        String activeFilter = request.getColumnSearchValue("active");
        if (StringUtils.isNotBlank(activeFilter)) {
            boolean isActive = "true".equalsIgnoreCase(activeFilter) || "1".equals(activeFilter);
            builder.and(qProduct.active.eq(isActive));
        }
        
        return builder;
    }

    /**
     * Build advanced filter expression from ProductFilterRequest
     */
    private BooleanBuilder buildAdvancedFilterExpression(ProductFilterRequest filter) {
        BooleanBuilder builder = new BooleanBuilder();
        
        // Global search
        if (filter.hasGlobalSearchFilter()) {
            builder.and(buildGlobalSearchExpression(filter.getGlobalSearch()));
        }
        
        // Name filter
        if (filter.hasNameFilter()) {
            builder.and(qProduct.name.lower().like("%" + filter.getName().toLowerCase() + "%"));
        }
        
        // Description filter
        if (filter.hasDescriptionFilter()) {
            builder.and(qProduct.description.lower().like("%" + filter.getDescription().toLowerCase() + "%"));
        }
        
        // Category filter
        if (filter.hasCategoryFilter()) {
            builder.and(qProduct.category.lower().eq(filter.getCategory().toLowerCase()));
        }
        
        // Sub-category filter
        if (filter.hasSubCategoryFilter()) {
            builder.and(qProduct.subCategory.lower().eq(filter.getSubCategory().toLowerCase()));
        }
        
        // Brand filter
        if (filter.hasBrandFilter()) {
            builder.and(qProduct.brand.lower().eq(filter.getBrand().toLowerCase()));
        }
        
        // SKU filter
        if (filter.hasSkuFilter()) {
            builder.and(qProduct.sku.lower().like("%" + filter.getSku().toLowerCase() + "%"));
        }
        
        // Price range filter
        if (filter.hasPriceRangeFilter()) {
            if (filter.getMinPrice() != null) {
                builder.and(qProduct.price.goe(filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                builder.and(qProduct.price.loe(filter.getMaxPrice()));
            }
        }
        
        // Stock range filter
        if (filter.hasStockRangeFilter()) {
            if (filter.getMinStock() != null) {
                builder.and(qProduct.stockQuantity.goe(filter.getMinStock()));
            }
            if (filter.getMaxStock() != null) {
                builder.and(qProduct.stockQuantity.loe(filter.getMaxStock()));
            }
        }
        
        // Launch date range filter
        if (filter.hasLaunchDateRangeFilter()) {
            if (filter.getLaunchDateFrom() != null) {
                builder.and(qProduct.launchDate.goe(filter.getLaunchDateFrom()));
            }
            if (filter.getLaunchDateTo() != null) {
                builder.and(qProduct.launchDate.loe(filter.getLaunchDateTo()));
            }
        }
        
        // Rating range filter
        if (filter.hasRatingRangeFilter()) {
            if (filter.getMinRating() != null) {
                builder.and(qProduct.rating.goe(filter.getMinRating()));
            }
            if (filter.getMaxRating() != null) {
                builder.and(qProduct.rating.loe(filter.getMaxRating()));
            }
        }
        
        // Active filter
        if (filter.hasActiveFilter()) {
            builder.and(qProduct.active.eq(filter.getActive()));
        }
        
        // Featured filter
        if (filter.hasFeaturedFilter()) {
            builder.and(qProduct.featured.eq(filter.getFeatured()));
        }
        
        // Tags filter
        if (filter.hasTagsFilter()) {
            if (StringUtils.isNotBlank(filter.getTags())) {
                builder.and(qProduct.tags.lower().like("%" + filter.getTags().toLowerCase() + "%"));
            }
        }
        
        return builder;
    }

    /**
     * Build order specifiers from DataTable request
     */
    private OrderSpecifier<?>[] buildOrderSpecifiers(DataTableRequest request) {
        if (request.getOrder() == null || request.getOrder().isEmpty()) {
            return new OrderSpecifier[0];
        }
        
        return request.getOrder().stream()
                .map(order -> {
                    String columnName = request.getColumns().get(order.getColumn()).getData();
                    return buildOrderSpecifier(columnName, order.getDir());
                })
                .filter(java.util.Objects::nonNull)
                .toArray(OrderSpecifier[]::new);
    }

    /**
     * Build individual order specifier
     */
    private OrderSpecifier<?> buildOrderSpecifier(String columnName, String direction) {
        boolean isAsc = !"desc".equalsIgnoreCase(direction);
        
        return switch (columnName.toLowerCase()) {
            case "name" -> isAsc ? qProduct.name.asc() : qProduct.name.desc();
            case "category" -> isAsc ? qProduct.category.asc() : qProduct.category.desc();
            case "brand" -> isAsc ? qProduct.brand.asc() : qProduct.brand.desc();
            case "price" -> isAsc ? qProduct.price.asc() : qProduct.price.desc();
            case "stockquantity", "stock" -> isAsc ? qProduct.stockQuantity.asc() : qProduct.stockQuantity.desc();
            case "launchdate" -> isAsc ? qProduct.launchDate.asc() : qProduct.launchDate.desc();
            case "rating" -> isAsc ? qProduct.rating.asc() : qProduct.rating.desc();
            case "active" -> isAsc ? qProduct.active.asc() : qProduct.active.desc();
            case "featured" -> isAsc ? qProduct.featured.asc() : qProduct.featured.desc();
            case "createdat" -> isAsc ? qProduct.createdAt.asc() : qProduct.createdAt.desc();
            case "updatedat" -> isAsc ? qProduct.updatedAt.asc() : qProduct.updatedAt.desc();
            default -> null;
        };
    }
} 