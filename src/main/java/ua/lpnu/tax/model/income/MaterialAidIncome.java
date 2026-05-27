package ua.lpnu.tax.model.income;

import ua.lpnu.tax.model.IncomeType;
import ua.lpnu.tax.model.Taxpayer;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MaterialAidIncome extends Income {
    private static final BigDecimal TAX_FREE_LIMIT = new BigDecimal("3760.00");
    private BigDecimal alreadyReceivedThisYear;

    public MaterialAidIncome() {
        this.alreadyReceivedThisYear = BigDecimal.ZERO;
    }

    public MaterialAidIncome(Taxpayer taxpayer, IncomeType type, BigDecimal amount,
                             String source, LocalDate accrualDate, BigDecimal alreadyReceivedThisYear) {
        super(taxpayer, type, amount, source, accrualDate);
        this.alreadyReceivedThisYear = alreadyReceivedThisYear;
    }

    public BigDecimal getAlreadyReceivedThisYear() {
        return alreadyReceivedThisYear;
    }

    public void setAlreadyReceivedThisYear(BigDecimal alreadyReceivedThisYear) {
        this.alreadyReceivedThisYear = alreadyReceivedThisYear;
    }

    @Override
    public BigDecimal getTaxableBase() {
        BigDecimal remaining = TAX_FREE_LIMIT.subtract(alreadyReceivedThisYear);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return amount;
        }
        BigDecimal taxFree = remaining.min(amount);
        return amount.subtract(taxFree);
    }
}