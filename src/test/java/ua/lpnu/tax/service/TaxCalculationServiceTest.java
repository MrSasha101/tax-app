package ua.lpnu.tax.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.lpnu.tax.model.IncomeType;
import ua.lpnu.tax.model.Taxpayer;
import ua.lpnu.tax.model.income.*;
import ua.lpnu.tax.model.tax.MilitaryLevy;
import ua.lpnu.tax.model.tax.PersonalIncomeTax;
import ua.lpnu.tax.model.tax.Tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaxCalculationService — розрахунок ПДФО і ВЗ")
class TaxCalculationServiceTest {

    private TaxCalculationService service;
    private Taxpayer taxpayer;

    @BeforeEach
    void setUp() {
        service  = new TaxCalculationService();
        taxpayer = new Taxpayer("Тест Тест", "0000000000", 0, null);
    }

    // ── Зарплата ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Зарплата 10 000 грн → ПДФО 18% = 1800, ВЗ 5% = 500")
    void salary_normalAmount_correctPdfoAndLevy() {
        IncomeType type = new IncomeType("Заробітна плата", new BigDecimal("18.00"));
        Income income = new SalaryIncome(taxpayer, type,
                new BigDecimal("10000"), "Фірма", LocalDate.now());

        List<Tax> taxes = service.calculateTaxes(income);

        assertEquals(2, taxes.size());

        Tax pdfo  = findByName(taxes, "ПДФО");
        Tax levy  = findByName(taxes, "Військовий збір");

        assertNotNull(pdfo,  "ПДФО має бути у списку");
        assertNotNull(levy,  "Військовий збір має бути у списку");

        assertEquals(0, new BigDecimal("1800.00").compareTo(pdfo.getTaxAmount()), "ПДФО = 1800.00");
        assertEquals(0, new BigDecimal("500.00").compareTo(levy.getTaxAmount()), "ВЗ = 500.00");
    }

    @Test
    @DisplayName("Зарплата нижче порогу ПСП → ПДФО розраховується від зменшеної бази")
    void salary_belowPspThreshold_taxOnReducedBase() {
        IncomeType type = new IncomeType("Заробітна плата", new BigDecimal("18.00"));
        Income income = new SalaryIncome(taxpayer, type,
                new BigDecimal("3000"), "Фірма", LocalDate.now());

        List<Tax> taxes = service.calculateTaxes(income);

        Tax pdfo = findByName(taxes, "ПДФО");
        assertNotNull(pdfo);
        assertEquals(0, new BigDecimal("267.48").compareTo(pdfo.getTaxAmount()), "ПДФО від зменшеної ПСП-бази");
    }

    // ── Дохід зі ставкою 0% ─────────────────────────────────────────────────

    @Test
    @DisplayName("Дохід зі ставкою 0% → повертає порожній список")
    void income_zeroRate_returnsEmptyList() {
        IncomeType taxFreeType = new IncomeType("Неоподатковуваний", BigDecimal.ZERO);
        Income income = new PropertySaleIncome(taxpayer, taxFreeType,
                new BigDecimal("100000"), "Квартира", LocalDate.now());

        List<Tax> taxes = service.calculateTaxes(income);

        assertTrue(taxes.isEmpty(), "Для ставки 0% не має бути жодного податку");
    }

    // ── Подарунок від родича ─────────────────────────────────────────────────

    @Test
    @DisplayName("Подарунок від родича I ступеня → база 0, податки 0")
    void gift_fromFirstDegreeRelative_noTaxes() {
        IncomeType type = new IncomeType("Подарунок від родича", new BigDecimal("18.00"));
        Income income = new GiftIncome(taxpayer, type,
                new BigDecimal("50000"), "Батько", LocalDate.now(), true);

        List<Tax> taxes = service.calculateTaxes(income);

        taxes.forEach(t ->
                assertEquals(0, BigDecimal.ZERO.compareTo(t.getTaxAmount()), "Усі суми мають бути 0")
        );
    }

    // ── Матеріальна допомога ─────────────────────────────────────────────────

    @Test
    @DisplayName("Мат. допомога 5000 (ліміт не вичерпаний) → ПДФО від 1240 грн")
    void materialAid_exceedsLimit_pdfoOnExcess() {
        IncomeType type = new IncomeType("Матеріальна допомога", new BigDecimal("18.00"));
        Income income = new MaterialAidIncome(taxpayer, type,
                new BigDecimal("5000"), "Профспілка", LocalDate.now(),
                BigDecimal.ZERO);

        List<Tax> taxes = service.calculateTaxes(income);
        Tax pdfo = findByName(taxes, "ПДФО");

        assertNotNull(pdfo);
        assertEquals(0, new BigDecimal("223.20").compareTo(pdfo.getTaxAmount()), "ПДФО від суми перевищення ліміту");
    }

    // ── Стандартні ставки ────────────────────────────────────────────────────

    @Test
    @DisplayName("PersonalIncomeTax: ставка за замовчуванням 18%")
    void personalIncomeTax_defaultRate_is18Percent() {
        PersonalIncomeTax pdfo = new PersonalIncomeTax();
        pdfo.calculate(new BigDecimal("10000"));

        assertEquals(0, new BigDecimal("18.00").compareTo(pdfo.getRateAtTime()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(pdfo.getTaxAmount()));
    }

    @Test
    @DisplayName("MilitaryLevy: константа MILITARY_RATE = 5.0%")
    void militaryLevy_rate_is5Percent() {
        assertEquals(0, new BigDecimal("5.0").compareTo(MilitaryLevy.MILITARY_RATE));
    }

    @Test
    @DisplayName("MilitaryLevy: розрахунок від бази 10 000 → 500.00")
    void militaryLevy_calculate_correctAmount() {
        MilitaryLevy levy = new MilitaryLevy();
        levy.calculate(new BigDecimal("10000"));

        assertEquals(0, new BigDecimal("500.00").compareTo(levy.getTaxAmount()));
        assertEquals("Військовий збір", levy.getTaxName());
    }

    @Test
    @DisplayName("PersonalIncomeTax: кастомна ставка застосовується коректно")
    void personalIncomeTax_customRate_appliedCorrectly() {
        PersonalIncomeTax pdfo = new PersonalIncomeTax();
        pdfo.setRateAtTime(new BigDecimal("9.00")); // пільгова ставка
        pdfo.calculate(new BigDecimal("10000"));

        assertEquals(0, new BigDecimal("900.00").compareTo(pdfo.getTaxAmount()));
    }

    // ── Допоміжний метод ─────────────────────────────────────────────────────

    private Tax findByName(List<Tax> taxes, String name) {
        return taxes.stream()
                .filter(t -> name.equals(t.getTaxName()))
                .findFirst()
                .orElse(null);
    }
}