package ua.lpnu.tax.model.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Військовий збір.
 * Ставка: 5.0% (підвищена з 1.5% у 2024 р. відповідно до Закону України).
 */
public class MilitaryLevy extends Tax {

    public static final BigDecimal MILITARY_RATE = new BigDecimal("5.0");

    @Override
    public BigDecimal calculate(BigDecimal base) {
        this.taxName   = "Військовий збір";
        this.rateAtTime = MILITARY_RATE;
        this.taxAmount  = base.multiply(MILITARY_RATE)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return this.taxAmount;
    }
}
