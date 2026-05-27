package ua.lpnu.tax.model.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Податок на доходи фізичних осіб (ПДФО).
 * Базова ставка: 18%. Ставка встановлюється через setRateAtTime() перед calculate().
 */
public class PersonalIncomeTax extends Tax {

    @Override
    public BigDecimal calculate(BigDecimal base) {
        this.taxName  = "ПДФО";
        if (this.rateAtTime == null) {
            this.rateAtTime = new BigDecimal("18.00"); // fallback
        }
        this.taxAmount = base.multiply(rateAtTime)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return this.taxAmount;
    }
}
