package ua.lpnu.tax.model.income;

import ua.lpnu.tax.model.IncomeType;
import ua.lpnu.tax.model.Taxpayer;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GiftIncome extends Income {
    private boolean fromFirstDegreeRelative;

    public GiftIncome() {}

    public GiftIncome(Taxpayer taxpayer, IncomeType type, BigDecimal amount,
                      String source, LocalDate accrualDate, boolean fromFirstDegreeRelative) {
        super(taxpayer, type, amount, source, accrualDate);
        this.fromFirstDegreeRelative = fromFirstDegreeRelative;
    }

    public boolean isFromFirstDegreeRelative() {
        return fromFirstDegreeRelative;
    }

    public void setFromFirstDegreeRelative(boolean fromFirstDegreeRelative) {
        this.fromFirstDegreeRelative = fromFirstDegreeRelative;
    }

    @Override
    public BigDecimal getTaxableBase() {
        if (fromFirstDegreeRelative) {
            return BigDecimal.ZERO;
        }
        return applyBenefitDiscounts(amount);
    }
}