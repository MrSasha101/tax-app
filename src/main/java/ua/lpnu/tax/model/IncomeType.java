package ua.lpnu.tax.model;

import java.math.BigDecimal;

public class IncomeType {
    private int id;
    private String name;
    private BigDecimal defaultTaxRate;

    public IncomeType() {}
    public IncomeType(String name, BigDecimal defaultTaxRate) {
        this.name = name;
        this.defaultTaxRate = defaultTaxRate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getDefaultTaxRate() { return defaultTaxRate; }
    public void setDefaultTaxRate(BigDecimal defaultTaxRate) { this.defaultTaxRate = defaultTaxRate; }

    @Override
    public String toString() {
        return name + " (" + defaultTaxRate + "%)";
    }
}