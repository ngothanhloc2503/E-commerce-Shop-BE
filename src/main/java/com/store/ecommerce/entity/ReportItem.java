package com.store.ecommerce.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportItem {
    String identifier;  // Time for sales by Date, Category name for sales by Category, Product name for sales by product
    float grossSales;
    float netSales;
    long ordersCount;   // For sales by date
    long productsCount; // For sales by product

    public ReportItem(String identifier) {
        this.identifier = identifier;
        this.grossSales = 0;
        this.netSales = 0;
        this.ordersCount = 0;
        this.productsCount = 0;
    }

    public ReportItem(String identifier, float grossSales, float netSales, long productsCount) {
        this.identifier = identifier;
        this.grossSales = grossSales;
        this.netSales = netSales;
        this.ordersCount = 0;
        this.productsCount = productsCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportItem that = (ReportItem) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    public void addGrossSales(float grossSales) {
        this.grossSales += grossSales;
    }

    public void addNetSales(float netSales) {
        this.netSales += netSales;
    }

    public void increaseOrdersCount() {
        this.ordersCount++;
    }

    public void increaseProductsCount(int quantity) {
        this.productsCount += quantity;
    }
}
