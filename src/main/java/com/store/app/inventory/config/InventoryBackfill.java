package com.store.app.inventory.config;

import com.store.app.inventory.repository.InventoryRepository;
import com.store.app.inventory.service.InventoryService;
import com.store.app.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * One-time safety net: products created before the inventory module
 * existed get an inventory row (initialized from the product's stock
 * figure, with an initial-stock transaction) at startup.
 */
@Slf4j
@Configuration
public class InventoryBackfill {

    @Bean
    @Order(3)
    public CommandLineRunner inventoryBackfillRunner(ProductRepository productRepository,
                                                     InventoryRepository inventoryRepository,
                                                     InventoryService inventoryService) {
        return args -> productRepository.findAll().forEach(product -> {
            if (!inventoryRepository.existsByProductId(product.getId())) {
                inventoryService.initializeInventory(product);
                log.info("Backfilled inventory for product {} ({})",
                        product.getId(), product.getSku());
            }
        });
    }
}
