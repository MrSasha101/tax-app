package ua.lpnu.tax.model.income;

import ua.lpnu.tax.model.IncomeType;
import ua.lpnu.tax.model.Taxpayer;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AuthorRoyaltyIncome extends Income {

    public AuthorRoyaltyIncome() {}

    public AuthorRoyaltyIncome(Taxpayer taxpayer, IncomeType type, BigDecimal amount,
                               String source, LocalDate accrualDate) {
        super(taxpayer, type, amount, source, accrualDate);
    }

    @Override
    public BigDecimal getTaxableBase() {
        return applyBenefitDiscounts(amount);
    }
}