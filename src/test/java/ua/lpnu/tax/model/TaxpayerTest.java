package ua.lpnu.tax.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.lpnu.tax.model.income.Income;
import ua.lpnu.tax.model.income.SalaryIncome;
import ua.lpnu.tax.model.tax.MilitaryLevy;
import ua.lpnu.tax.model.tax.PersonalIncomeTax;
import ua.lpnu.tax.model.tax.Tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Taxpayer — модель платника")
class TaxpayerTest {

    private Taxpayer taxpayer;
    private IncomeType salaryType;

    @BeforeEach
    void setUp() {
        taxpayer   = new Taxpayer("Іваненко Іван Іванович", "1234567890", 2, null);
        salaryType = new IncomeType("Заробітна плата", new BigDecimal("18.00"));
    }

    // ── Базові поля ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Геттери повертають коректні значення після конструктора")
    void constructor_setsFieldsCorrectly() {
        assertEquals("Іваненко Іван Іванович", taxpayer.getFullName());
        assertEquals("1234567890", taxpayer.getTaxId());
        assertEquals(2, taxpayer.getChildrenCount());
        assertNull(taxpayer.getSpecialStatus());
    }

    @Test
    @DisplayName("toString() містить ПІБ та ІПН")
    void toString_containsNameAndTaxId() {
        String s = taxpayer.toString();
        assertTrue(s.contains("Іваненко Іван Іванович"));
        assertTrue(s.contains("1234567890"));
    }

    // ── Агрегація доходів ────────────────────────────────────────────────────

    @Test
    @DisplayName("getTotalAnnualIncome() — сума всіх доходів")
    void getTotalAnnualIncome_sumsAllAmounts() {
        Income i1 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("10000"), "Роботодавець", LocalDate.now());
        Income i2 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("5000"),  "Роботодавець", LocalDate.now());
        taxpayer.setIncomes(List.of(i1, i2));

        assertEquals(0, new BigDecimal("15000").compareTo(taxpayer.getTotalAnnualIncome()));
    }

    @Test
    @DisplayName("getTotalAnnualIncome() — порожній список повертає 0")
    void getTotalAnnualIncome_emptyList_returnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(taxpayer.getTotalAnnualIncome()));
    }

    @Test
    @DisplayName("getTotalTaxesPaid() — сума всіх нарахованих податків")
    void getTotalTaxesPaid_sumsAllTaxes() {
        Income income = new SalaryIncome(taxpayer, salaryType, new BigDecimal("10000"), "Фірма", LocalDate.now());

        PersonalIncomeTax pdfo = new PersonalIncomeTax();
        pdfo.setRateAtTime(new BigDecimal("18.00"));
        pdfo.calculate(new BigDecimal("10000")); // 1800.00

        MilitaryLevy levy = new MilitaryLevy();
        levy.calculate(new BigDecimal("10000")); // 500.00

        income.setCalculatedTaxes(List.of(pdfo, levy));
        taxpayer.setIncomes(List.of(income));

        assertEquals(0, new BigDecimal("2300.00").compareTo(taxpayer.getTotalTaxesPaid()));
    }

    @Test
    @DisplayName("getTotalTaxesPaid() — без доходів повертає 0")
    void getTotalTaxesPaid_noIncomes_returnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(taxpayer.getTotalTaxesPaid()));
    }

    // ── Фільтрація доходів ───────────────────────────────────────────────────

    @Test
    @DisplayName("searchByAmountRange() — повертає лише доходи в діапазоні")
    void searchByAmountRange_returnsInRangeOnly() {
        Income i1 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("5000"),  "A", LocalDate.now());
        Income i2 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("10000"), "B", LocalDate.now());
        Income i3 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("20000"), "C", LocalDate.now());
        taxpayer.setIncomes(List.of(i1, i2, i3));

        List<Income> result = taxpayer.searchByAmountRange(
                new BigDecimal("8000"), new BigDecimal("15000"));

        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("10000").compareTo(result.get(0).getAmount()));
    }

    @Test
    @DisplayName("searchByAmountRange() — кордонні значення включаються")
    void searchByAmountRange_boundaryValuesIncluded() {
        Income i1 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("5000"),  "A", LocalDate.now());
        Income i2 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("10000"), "B", LocalDate.now());
        taxpayer.setIncomes(List.of(i1, i2));

        List<Income> result = taxpayer.searchByAmountRange(
                new BigDecimal("5000"), new BigDecimal("10000"));

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("searchByAmountRange() — порожній результат якщо нічого не в діапазоні")
    void searchByAmountRange_noMatches_returnsEmpty() {
        Income i1 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("5000"), "A", LocalDate.now());
        taxpayer.setIncomes(List.of(i1));

        List<Income> result = taxpayer.searchByAmountRange(
                new BigDecimal("10000"), new BigDecimal("20000"));

        assertTrue(result.isEmpty());
    }

    // ── Сортування доходів ───────────────────────────────────────────────────

    @Test
    @DisplayName("getSortedIncomes() — сортує за сумою зростання")
    void getSortedIncomes_byAmountAscending() {
        Income i1 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("30000"), "A", LocalDate.now());
        Income i2 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("5000"),  "B", LocalDate.now());
        Income i3 = new SalaryIncome(taxpayer, salaryType, new BigDecimal("15000"), "C", LocalDate.now());
        taxpayer.setIncomes(List.of(i1, i2, i3));

        List<Income> sorted = taxpayer.getSortedIncomes(
                (a, b) -> a.getAmount().compareTo(b.getAmount()));

        assertEquals(0, new BigDecimal("5000").compareTo(sorted.get(0).getAmount()));
        assertEquals(0, new BigDecimal("15000").compareTo(sorted.get(1).getAmount()));
        assertEquals(0, new BigDecimal("30000").compareTo(sorted.get(2).getAmount()));
    }
}