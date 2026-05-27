package ua.lpnu.tax.model.income;

import ua.lpnu.tax.model.IncomeType;
import ua.lpnu.tax.model.Taxpayer;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SalaryIncome extends Income {
    private static final BigDecimal PSP_THRESHOLD = new BigDecimal("3028.00");
    private static final BigDecimal BASE_PSP = new BigDecimal("1514.00");

    public SalaryIncome() {}

    public SalaryIncome(Taxpayer taxpayer, IncomeType type, BigDecimal amount,
                        String source, LocalDate accrualDate) {
        super(taxpayer, type, amount, source, accrualDate);
    }

    @Override
    public BigDecimal getTaxableBase() {
        BigDecimal base = this.amount;
        if (base.compareTo(PSP_THRESHOLD) <= 0) {
            int children = taxpayer.getChildrenCount();
            BigDecimal psp = children > 0
                    ? BASE_PSP.multiply(BigDecimal.valueOf(children))
                    : BASE_PSP;
            base = base.subtract(psp).max(BigDecimal.ZERO);
        }
        return applyBenefitDiscounts(base);
    }
}