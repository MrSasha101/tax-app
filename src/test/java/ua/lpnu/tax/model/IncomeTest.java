package ua.lpnu.tax.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ua.lpnu.tax.model.income.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Income — ієрархія доходів")
class IncomeTest {

    private Taxpayer taxpayerNoChildren;
    private Taxpayer taxpayerWithChildren;
    private IncomeType salaryType;
    private IncomeType genericType;

    @BeforeEach
    void setUp() {
        taxpayerNoChildren   = new Taxpayer("Петренко Петро", "1111111111", 0, null);
        taxpayerWithChildren = new Taxpayer("Коваль Марія",   "2222222222", 2, null);

        salaryType  = new IncomeType("Заробітна плата",  new BigDecimal("18.00"));
        genericType = new IncomeType("Інший дохід",      new BigDecimal("18.00"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("SalaryIncome — заробітна плата")
    class SalaryIncomeTest {

        @Test
        @DisplayName("База оподаткування = сума - ПСП (коли сума <= 3028 і немає дітей)")
        void taxableBase_belowThreshold_noChildren_subtractsPsp() {
            SalaryIncome income = new SalaryIncome(
                    taxpayerNoChildren, salaryType,
                    new BigDecimal("3000"), "ТОВ Ромашка", LocalDate.now());

            assertEquals(0, new BigDecimal("1486.00").compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("База оподаткування = сума - ПСП × діти (коли сума <= 3028 і є діти)")
        void taxableBase_belowThreshold_withChildren_subtractsChildPsp() {
            SalaryIncome income = new SalaryIncome(
                    taxpayerWithChildren, salaryType,
                    new BigDecimal("3000"), "Підприємство", LocalDate.now());

            assertEquals(0, BigDecimal.ZERO.compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("База оподаткування = повна сума (коли сума > 3028)")
        void taxableBase_aboveThreshold_fullAmount() {
            SalaryIncome income = new SalaryIncome(
                    taxpayerNoChildren, salaryType,
                    new BigDecimal("10000"), "Фірма", LocalDate.now());

            assertEquals(0, new BigDecimal("10000").compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("Рівно на порозі 3028 — ПСП застосовується")
        void taxableBase_exactlyAtThreshold_pspApplied() {
            SalaryIncome income = new SalaryIncome(
                    taxpayerNoChildren, salaryType,
                    new BigDecimal("3028"), "Фірма", LocalDate.now());

            assertEquals(0, new BigDecimal("1514").compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("База не може бути від'ємною")
        void taxableBase_neverNegative() {
            SalaryIncome income = new SalaryIncome(
                    taxpayerWithChildren, salaryType,
                    new BigDecimal("100"), "Благодійна організація", LocalDate.now());

            assertTrue(income.getTaxableBase().compareTo(BigDecimal.ZERO) >= 0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GiftIncome — подарунки")
    class GiftIncomeTest {

        @Test
        @DisplayName("Подарунок від родича I ступеня → база = 0 (не оподатковується)")
        void taxableBase_fromFirstDegreeRelative_isZero() {
            GiftIncome income = new GiftIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("50000"), "Батько", LocalDate.now(), true);

            assertEquals(0, BigDecimal.ZERO.compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("Подарунок від третьої особи → база = повна сума")
        void taxableBase_fromOther_isFullAmount() {
            GiftIncome income = new GiftIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("20000"), "Роботодавець", LocalDate.now(), false);

            assertEquals(0, new BigDecimal("20000").compareTo(income.getTaxableBase()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("MaterialAidIncome — матеріальна допомога")
    class MaterialAidIncomeTest {

        @Test
        @DisplayName("Перша допомога до ліміту (3760) — база = 0")
        void taxableBase_withinLimit_isZero() {
            MaterialAidIncome income = new MaterialAidIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("3000"), "Профспілка", LocalDate.now(),
                    BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("Сума перевищує ліміт — база = перевищення")
        void taxableBase_exceedsLimit_baseIsExcess() {
            MaterialAidIncome income = new MaterialAidIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("5000"), "Підприємство", LocalDate.now(),
                    BigDecimal.ZERO);

            assertEquals(0, new BigDecimal("1240.00").compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("Ліміт вже вичерпаний — уся сума оподатковується")
        void taxableBase_limitExhausted_fullAmountTaxed() {
            MaterialAidIncome income = new MaterialAidIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("2000"), "Підприємство", LocalDate.now(),
                    new BigDecimal("3760"));

            assertEquals(0, new BigDecimal("2000").compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("Частково вичерпаний ліміт — оподатковується тільки решта")
        void taxableBase_partialLimitUsed() {
            MaterialAidIncome income = new MaterialAidIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("1000"), "Підприємство", LocalDate.now(),
                    new BigDecimal("3000"));

            assertEquals(0, new BigDecimal("240.00").compareTo(income.getTaxableBase()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PropertySaleIncome — продаж майна")
    class PropertySaleIncomeTest {

        @Test
        @DisplayName("База = повна сума продажу без пільг")
        void taxableBase_noBenefits_fullAmount() {
            PropertySaleIncome income = new PropertySaleIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("500000"), "Квартира", LocalDate.now());

            assertEquals(0, new BigDecimal("500000").compareTo(income.getTaxableBase()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("ForeignTransferIncome — переказ з-за кордону")
    class ForeignTransferIncomeTest {

        @Test
        @DisplayName("База = повна сума без пільг")
        void taxableBase_noBenefits_fullAmount() {
            ForeignTransferIncome income = new ForeignTransferIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("25000"), "PayPal", LocalDate.now());

            assertEquals(0, new BigDecimal("25000").compareTo(income.getTaxableBase()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("AuthorRoyaltyIncome — авторська винагорода")
    class AuthorRoyaltyIncomeTest {

        @Test
        @DisplayName("База = повна сума без пільг")
        void taxableBase_noBenefits_fullAmount() {
            AuthorRoyaltyIncome income = new AuthorRoyaltyIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("8000"), "Видавництво", LocalDate.now());

            assertEquals(0, new BigDecimal("8000").compareTo(income.getTaxableBase()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Benefit — застосування пільг")
    class BenefitTest {

        @Test
        @DisplayName("Пільга з фіксованою знижкою (discountAmount) зменшує базу")
        void benefit_fixedDiscount_reducesBase() {
            Benefit benefit = new Benefit("Пільга", "Тест",
                    new BigDecimal("1000"), null);

            AuthorRoyaltyIncome income = new AuthorRoyaltyIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("5000"), "Видавництво", LocalDate.now());
            income.setAppliedBenefits(List.of(benefit));

            assertEquals(0, new BigDecimal("4000").compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("Пільга з відсотковою знижкою (discountPercentage) зменшує базу")
        void benefit_percentageDiscount_reducesBase() {
            Benefit benefit = new Benefit("10%-пільга", "Тест",
                    null, new BigDecimal("10"));

            ForeignTransferIncome income = new ForeignTransferIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("5000"), "PayPal", LocalDate.now());
            income.setAppliedBenefits(List.of(benefit));

            assertEquals(0, new BigDecimal("4500.00").compareTo(income.getTaxableBase()));
        }

        @Test
        @DisplayName("База після пільг не може бути від'ємною")
        void benefit_largerThanBase_resultIsZero() {
            Benefit hugeBenefit = new Benefit("Надвелика", "Тест",
                    new BigDecimal("99999"), null);

            AuthorRoyaltyIncome income = new AuthorRoyaltyIncome(
                    taxpayerNoChildren, genericType,
                    new BigDecimal("1000"), "Видавництво", LocalDate.now());
            income.setAppliedBenefits(List.of(hugeBenefit));

            assertEquals(0, BigDecimal.ZERO.compareTo(income.getTaxableBase()));
        }
    }
}