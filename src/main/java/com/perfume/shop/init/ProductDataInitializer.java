package com.perfume.shop.init;

import com.perfume.shop.entity.Product;
import com.perfume.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Initializes product data on application startup if no products exist.
 * 
 * DEMO PROFILE ONLY: This creates 20 sample perfume products for testing.
 * In production, products should be added through admin panel or database migration.
 */
@Component
@Profile("!production")  // Only run when NOT in production profile
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ProductDataInitializer implements CommandLineRunner {
    
    private final ProductRepository productRepository;
    
    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("✓ Products already exist in database ({}), skipping initialization", productRepository.count());
            return;
        }
        
        log.info("📦 Initializing sample product data...");
        
        List<Product> products = Arrays.asList(
            // Luxury Women's Fragrances
            createProduct("Chanel No. 5", "Chanel", "The iconic timeless classic. A floral bouquet with top notes of neroli and ylang-ylang.", 165.00, "Women", "Eau de Parfum", 50, 45, 4.8, 245, true, "https://images.unsplash.com/photo-1596178065887-cf38d2e3e02f?w=500"),
            createProduct("Dior J'adore", "Dior", "Luxurious floral fragrance with notes of jasmine, rose, and orange blossom.", 150.00, "Women", "Eau de Parfum", 50, 52, 4.7, 189, true, "https://images.unsplash.com/photo-1562181286-d3fee7d55364?w=500"),
            createProduct("Gucci Bloom", "Gucci", "Fresh floral composition with gardenia, tuberose, and jasmine.", 148.00, "Women", "Eau de Parfum", 50, 38, 4.6, 156, false, "https://images.unsplash.com/photo-1595777712802-372adc3dd05e?w=500"),
            createProduct("Tom Ford Black Orchid", "Tom Ford", "Luxurious and sensual fragrance blending black orchid with dark chocolate.", 210.00, "Women", "Eau de Parfum", 50, 28, 4.9, 312, true, "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=500"),
            createProduct("YSL Mon Paris", "Yves Saint Laurent", "Romantic floral fragrance with notes of rose, peony, and blackcurrant.", 135.00, "Women", "Eau de Parfum", 50, 55, 4.6, 178, false, "https://images.unsplash.com/photo-1588405748507-ca7b328e4e1d?w=500"),
            
            // Premium Men's Fragrances
            createProduct("Dior Sauvage", "Dior", "Fresh and spicy fragrance with ambroxan, pepper, and ambrette seed.", 140.00, "Men", "Eau de Toilette", 100, 67, 4.8, 356, true, "https://images.unsplash.com/photo-1592078615290-033ee584e267?w=500"),
            createProduct("Bleu de Chanel", "Chanel", "Sophisticated fragrance with notes of citrus, ginger, and cedar.", 145.00, "Men", "Eau de Parfum", 50, 58, 4.8, 298, true, "https://images.unsplash.com/photo-1613467489881-a59c0cda0b2b?w=500"),
            createProduct("Creed Aventus", "Creed", "Legendary fragrance with pineapple, blackcurrant, and oakmoss.", 380.00, "Men", "Eau de Parfum", 50, 15, 4.9, 421, true, "https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=500"),
            createProduct("Versace Eros", "Versace", "Energetic and seductive fragrance with mint, green apple, and tonka bean.", 125.00, "Men", "Eau de Toilette", 100, 42, 4.6, 224, false, "https://images.unsplash.com/photo-1577875729629-ed5fdc1d3dc3?w=500"),
            createProduct("Giorgio Armani Code", "Giorgio Armani", "Sophisticated and warm fragrance with iris, ambroxan, and leather.", 130.00, "Men", "Eau de Toilette", 100, 48, 4.7, 256, false, "https://images.unsplash.com/photo-1595836374141-a96df00e5f30?w=500"),
            
            // Unisex Fragrances
            createProduct("Marc Jacobs Daisy Love", "Marc Jacobs", "Fresh and flirty unisex fragrance with wildflower, white raspberry, and vanilla.", 95.00, "Unisex", "Eau de Toilette", 100, 72, 4.3, 98, false, "https://images.unsplash.com/photo-1548695207-9b3f53c4c82f?w=500"),
            createProduct("Jo Malone Wood Sage & Sea Salt", "Jo Malone", "Fresh woody cologne perfect for layering.", 185.00, "Unisex", "Cologne", 100, 45, 4.6, 167, true, "https://images.unsplash.com/photo-1570538108519-280658a16dda?w=500"),
            createProduct("Acqua di Parma Blu Mediterraneo", "Acqua di Parma", "Fresh Mediterranean fragrance with lemon and Sicilian citrus.", 155.00, "Unisex", "Eau de Toilette", 100, 39, 4.5, 145, false, "https://images.unsplash.com/photo-1588405748507-ca7b328e4e1d?w=500"),
            
            // Limited Edition
            createProduct("Roja Parfums Elixir", "Roja Parfums", "Exquisite luxury fragrance with precious ingredients. Ultra-rare.", 450.00, "Limited Edition", "Parfum", 50, 8, 4.9, 89, true, "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=500"),
            createProduct("Creed Royal Oud", "Creed", "Precious oud-based fragrance with rose and jasmine.", 420.00, "Limited Edition", "Eau de Parfum", 50, 12, 4.8, 134, true, "https://images.unsplash.com/photo-1550355291-bbee04a92027?w=500"),
            
            // Best Sellers
            createProduct("Lancôme La Vie Est Belle", "Lancôme", "Best-selling fragrance with praline, iris, and patchouli.", 98.00, "Women", "Eau de Parfum", 50, 95, 4.7, 201, true, "https://images.unsplash.com/photo-1569505254788-205b4f368f5f?w=500"),
            createProduct("Hugo Boss Bottled", "Hugo Boss", "Best-selling mens fragrance with apple, cinnamon, and leather.", 92.00, "Men", "Eau de Toilette", 100, 81, 4.5, 189, false, "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=500"),
            createProduct("Flowerbomb", "Viktor & Rolf", "Explosive floral fragrance with sambac jasmine and centifolia rose.", 165.00, "Women", "Eau de Parfum", 50, 29, 4.8, 278, true, "https://images.unsplash.com/photo-1577875729629-ed5fdc1d3dc3?w=500"),
            createProduct("Coco Mademoiselle", "Chanel", "Elegant modern floral for the sophisticated woman.", 155.00, "Women", "Eau de Parfum", 50, 20, 4.8, 334, true, "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=500"),
            createProduct("Jean Paul Gaultier Le Male", "Jean Paul Gaultier", "Legendary masculine fragrance with mint, jasmine, and amber.", 105.00, "Men", "Eau de Toilette", 125, 64, 4.6, 267, false, "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=500")
        );
        
        productRepository.saveAll(products);
        log.info("✅ Initialized {} sample products for demo/testing", products.size());
    }
    
    private Product createProduct(String name, String brand, String description, double price, 
            String category, String type, int volume, int stock, double rating, int reviewCount, 
            boolean featured, String imageUrl) {
        return Product.builder()
                .name(name)
                .brand(brand)
                .description(description)
                .price(BigDecimal.valueOf(price))
                .category(category)
                .type(type)
                .volume(volume)
                .stock(stock)
                .rating(rating)
                .reviewCount(reviewCount)
                .featured(featured)
                .active(true)
                .imageUrl(imageUrl)
                .build();
    }
}
