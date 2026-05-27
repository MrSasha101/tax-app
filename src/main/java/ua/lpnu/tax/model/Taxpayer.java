package ua.lpnu.tax.model;

import ua.lpnu.tax.model.income.Income;
import ua.lpnu.tax.model.tax.Tax;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Taxpayer {
    private int id;
    private String fullName;
    private String taxId;
    private int childrenCount;
    private String specialStatus;
    private List<Income> incomes = new ArrayList<>();

    public Taxpayer() {}

    public Taxpayer(String fullName, String taxId, int childrenCount, String specialStatus) {
        this.fullName = fullName;
        this.taxId = taxId;
        this.childrenCount = childrenCount;
        this.specialStatus = specialStatus;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public int getChildrenCount() { return childrenCount; }
    public void setChildrenCount(int childrenCount) { this.childrenCount = childrenCount; }
    public String getSpecialStatus() { return specialStatus; }
    public void setSpecialStatus(String specialStatus) { this.specialStatus = specialStatus; }
    public List<Income> getIncomes() { return incomes; }
    public void setIncomes(List<Income> incomes) { this.incomes = incomes; }

    public BigDecimal getTotalAnnualIncome() {
        return incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalTaxesPaid() {
        return incomes.stream()
                .flatMap(i -> i.getCalculatedTaxes().stream())
                .map(Tax::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Income> getSortedIncomes(Comparator<Income> cmp) {
        return incomes.stream().sorted(cmp).collect(Collectors.toList());
    }

    public List<Income> searchByAmountRange(BigDecimal min, BigDecimal max) {
        return incomes.stream()
                .filter(i -> i.getAmount().compareTo(min) >= 0
                        && i.getAmount().compareTo(max) <= 0)
                .collect(Collectors.toList());
    }

    /**
     * Відображається у ListView — показує ПІБ та ІПН.
     */
    @Override
    public String toString() {
        return fullName + " (ІПН: " + taxId + ")";
    }
}
