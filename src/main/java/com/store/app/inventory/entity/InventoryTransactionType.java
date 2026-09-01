package com.store.app.inventory.entity;

/**
 * Why a stock level changed.
 */
public enum InventoryTransactionType {
    /** Stock received from a supplier (increases stock). */
    PURCHASE,
    /** Stock sold to a customer (decreases stock). */
    SALE,
    /** Customer return restocked (increases stock). */
    RETURN,
    /** Damaged / expired stock written off (decreases stock). */
    DAMAGE,
    /** Manual correction to an absolute level (either direction). */
    ADJUSTMENT
}
