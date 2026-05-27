package ua.lpnu.tax.model.income;

import ua.lpnu.tax.model.Benefit;
import ua.lpnu.tax.model.IncomeType;
import ua.lpnu.tax.model.Taxpayer;
import ua.lpnu.tax.model.tax.Tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class Income {
    protected int id;
    protected Taxpayer taxpayer;
    protected IncomeType type;
    protected BigDecimal amount;
    protected String source;
    protected LocalDate accrualDate;
    protected List<Benefit> appliedBenefits = new ArrayList<>();
    protected List<Tax> calculatedTaxes = new ArrayList<>();

    public Income() {}
    public Income(Taxpayer taxpayer, IncomeType type, BigDecimal amount, String source, LocalDate accrualDate) {
        this.taxpayer = taxpayer;
        this.type = type;
        this.amount = amount;
        this.source = source;
        this.accrualDate = accrualDate;
    }

    public abstract BigDecimal getTaxableBase();

    protected BigDecimal applyBenefitDiscounts(BigDecimal base) {
        for (Benefit b : appliedBenefits) {
            base = b.applyTo(base);
        }
        return base.max(BigDecimal.ZERO);
    }

    // Геттери/сеттери
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Taxpayer getTaxpayer() { return taxpayer; }
    public void setTaxpayer(Taxpayer taxpayer) { this.taxpayer = taxpayer; }
    public IncomeType getType() { return type; }
    public void setType(IncomeType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDate getAccrualDate() { return accrualDate; }
    public void setAccrualDate(LocalDate accrualDate) { this.accrualDate = accrualDate; }
    public List<Benefit> getAppliedBenefits() { return appliedBenefits; }
    public void setAppliedBenefits(List<Benefit> appliedBenefits) { this.appliedBenefits = appliedBenefits; }
    public List<Tax> getCalculatedTaxes() { return calculatedTaxes; }
    public void setCalculatedTaxes(List<Tax> calculatedTaxes) { this.calculatedTaxes = calculatedTaxes; }
}