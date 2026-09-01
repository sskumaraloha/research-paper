package com.store.app.admin.dto;

import java.math.BigDecimal;

/**
 * One row of the dashboard's top-selling-products table, aggregated
 * from order-line snapshots (constructor used by a JPQL projection).
 */
public record TopProductRow(
        String productName,
        String sku,
        Long unitsSold,
        BigDecimal revenue
) {
}
