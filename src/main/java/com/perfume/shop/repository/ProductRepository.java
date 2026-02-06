package com.perfume.shop.repository;

import com.perfume.shop.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByActiveTrue(Pageable pageable);
    
    Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);
    
    Page<Product> findByBrandAndActiveTrue(String brand, Pageable pageable);
    
    List<Product> findByFeaturedTrueAndActiveTrue();
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                    @Param("maxPrice") BigDecimal maxPrice, 
                                    Pageable pageable);
    
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true ORDER BY p.brand")
    List<String> findDistinctBrandByActiveTrue();
    
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true ORDER BY p.category")
    List<String> findDistinctCategories();
    
    // Find products by exact name (case-insensitive)
    Optional<Product> findByNameIgnoreCaseAndActiveTrue(String name);
    
    // Find products by name containing keyword (case-insensitive)
    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY p.rating DESC LIMIT 1")
    Optional<Product> findByNameContainsIgnoreCaseAndActiveTrue(@Param("name") String name);
    
    // Admin queries - include inactive products
    Page<Product> findAll(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "(:active IS NULL OR p.active = :active)")
    Page<Product> findByActiveStatus(@Param("active") Boolean active, Pageable pageable);
    
    // Advanced filtering
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:brand IS NULL OR p.brand IN :brands) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:featured IS NULL OR p.featured = :featured) " +
           "AND (:minRating IS NULL OR p.rating >= :minRating) " +
           "AND (:inStock IS NULL OR (:inStock = true AND p.stock > 0) OR (:inStock = false))")
    Page<Product> findByFilters(
            @Param("category") String category,
            @Param("brands") List<String> brands,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("featured") Boolean featured,
            @Param("minRating") Integer minRating,
            @Param("inStock") Boolean inStock,
            Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchWithFilters(
            @Param("query") String query,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
    
    // Stock management
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock <= :threshold ORDER BY p.stock ASC")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);
    
    // Statistics
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    Long countActiveProducts();
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stock = 0")
    Long countOutOfStockProducts();
    
    // Pessimistic locking for checkout
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
    
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdWithLock(@Param("ids") List<Long> ids);
    
    // Admin dashboard methods
    Long countByActiveTrue();
    
    Long countByStockLessThanEqual(Integer stock);
    
    Page<Product> findByActiveTrue(Boolean active, Pageable pageable);
    
    // Inventory management methods
    List<Product> findByStockLessThanAndActiveTrue(Integer stock);
    
    List<Product> findByStockEqualsAndActiveTrue(Integer stock);
    
    Long countByStockLessThanAndActiveTrue(Integer stock);
    
    Long countByStockEqualsAndActiveTrue(Integer stock);
}
