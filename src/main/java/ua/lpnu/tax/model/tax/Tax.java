package ua.lpnu.tax.model.tax;

import ua.lpnu.tax.model.income.Income;
import java.math.BigDecimal;

public abstract class Tax {
    protected int id;
    protected Income income;
    protected String taxName;
    protected BigDecimal taxAmount;
    protected BigDecimal rateAtTime;

    public abstract BigDecimal calculate(BigDecimal taxableBase);

    // геттери/сеттери
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Income getIncome() { return income; }
    public void setIncome(Income income) { this.income = income; }
    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getRateAtTime() { return rateAtTime; }
    public void setRateAtTime(BigDecimal rateAtTime) { this.rateAtTime = rateAtTime; }
}