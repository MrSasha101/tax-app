package ua.lpnu.tax.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Benefit {
    private int id;
    private String name;
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;

    public Benefit() {}
    public Benefit(String name, String description, BigDecimal discountAmount, BigDecimal discountPercentage) {
        this.name = name;
        this.description = description;
        this.discountAmount = discountAmount;
        this.discountPercentage = discountPercentage;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }

    public BigDecimal applyTo(BigDecimal base) {
        BigDecimal result = base;
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            result = result.subtract(discountAmount);
        }
        if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = base.multiply(discountPercentage)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            result = result.subtract(discount);
        }
        return result.max(BigDecimal.ZERO);
    }
}