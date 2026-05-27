package ua.lpnu.tax.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.lpnu.tax.model.income.Income;
import ua.lpnu.tax.model.tax.MilitaryLevy;
import ua.lpnu.tax.model.tax.PersonalIncomeTax;
import ua.lpnu.tax.model.tax.Tax;
import ua.lpnu.tax.util.EmailSender;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервіс розрахунку ПДФО та Військового збору для доходу.
 */
public class TaxCalculationService {

    private static final Logger logger = LogManager.getLogger(TaxCalculationService.class);

    public List<Tax> calculateTaxes(Income income) {
        List<Tax> taxes = new ArrayList<>();
        try {
            BigDecimal base = income.getTaxableBase();
            logger.info("Розрахунок податків: дохід id={}, база={}", income.getId(), base);

            BigDecimal rate = income.getType().getDefaultTaxRate();

            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                PersonalIncomeTax pdfo = new PersonalIncomeTax();
                pdfo.setRateAtTime(rate);
                pdfo.setIncome(income);
                pdfo.calculate(base);
                taxes.add(pdfo);
                logger.debug("ПДФО: {}% від {} = {}", rate, base, pdfo.getTaxAmount());

                MilitaryLevy vz = new MilitaryLevy();
                vz.setIncome(income);
                vz.calculate(base);
                taxes.add(vz);
                logger.debug("Військ. збір: {}% від {} = {}", MilitaryLevy.MILITARY_RATE, base, vz.getTaxAmount());
            } else {
                logger.info("Дохід id={} не оподатковується (ставка 0%)", income.getId());
            }
        } catch (Exception e) {
            logger.error("Критична помилка розрахунку податків для доходу id={}", income.getId(), e);
            EmailSender.sendCriticalAlert(
                    "Помилка розрахунку податків",
                    "Дохід id=" + income.getId() + "\n" + e.getMessage()
            );
        }
        return taxes;
    }
}
