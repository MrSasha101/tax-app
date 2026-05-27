package ua.lpnu.tax.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import ua.lpnu.tax.model.IncomeType;
import ua.lpnu.tax.model.Taxpayer;
import ua.lpnu.tax.model.income.Income;
import ua.lpnu.tax.model.income.SalaryIncome;
import ua.lpnu.tax.model.tax.Tax;
import ua.lpnu.tax.service.TaxCalculationService;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("IncomeDao — комплексне тестування збереження доходів")
class IncomeDaoTest {

    private IncomeDao incomeDao;

    @Mock private TaxDao mockTaxDao;
    @Spy  private TaxCalculationService spyTaxService = new TaxCalculationService();

    @Mock private Connection mockConnection;
    @Mock private PreparedStatement mockMainStmt;
    @Mock private PreparedStatement mockBenefitStmt;
    @Mock private ResultSet mockMainRs;
    @Mock private ResultSet mockBenefitRs;
    @Mock private ResultSet mockKeysRs;

    private MockedStatic<DBConnection> mockedDbConnection;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        incomeDao = new IncomeDao();

        Field taxDaoField = IncomeDao.class.getDeclaredField("taxDao");
        taxDaoField.setAccessible(true);
        taxDaoField.set(incomeDao, mockTaxDao);

        Field taxServiceField = IncomeDao.class.getDeclaredField("taxService");
        taxServiceField.setAccessible(true);
        taxServiceField.set(incomeDao, spyTaxService);

        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockedDbConnection.close();
        mocks.close();
    }

    @Test
    @DisplayName("findById() — успішно збирає Income, завантажує пільги та податки")
    void findById_success() throws SQLException {
        when(mockConnection.prepareStatement(contains("SELECT i.*"))).thenReturn(mockMainStmt);
        when(mockMainStmt.executeQuery()).thenReturn(mockMainRs);
        when(mockMainRs.next()).thenReturn(true);

        when(mockMainRs.getString("type_name")).thenReturn("Заробітна плата");
        when(mockMainRs.getInt("type_id")).thenReturn(1);
        when(mockMainRs.getBigDecimal("default_tax_rate")).thenReturn(new BigDecimal("18.0"));
        when(mockMainRs.getInt("taxpayer_id")).thenReturn(10);
        when(mockMainRs.getBigDecimal("amount")).thenReturn(new BigDecimal("15000"));
        when(mockMainRs.getString("source")).thenReturn("Роботодавець");
        when(mockMainRs.getDate("accrual_date")).thenReturn(java.sql.Date.valueOf(LocalDate.now()));
        when(mockMainRs.getInt("id")).thenReturn(5);

        when(mockConnection.prepareStatement(contains("SELECT b.* FROM benefits"))).thenReturn(mockBenefitStmt);
        when(mockBenefitStmt.executeQuery()).thenReturn(mockBenefitRs);
        when(mockBenefitRs.next()).thenReturn(false);

        when(mockTaxDao.findByIncomeId(5)).thenReturn(new ArrayList<>());

        Income result = incomeDao.findById(5);

        assertNotNull(result);
        assertTrue(result instanceof SalaryIncome);
        assertEquals(5, result.getId());
        assertEquals("Заробітна плата", result.getType().getName());
    }

    @Test
    @DisplayName("save() — розраховує податки та зберігає все в базу")
    void save_success() throws SQLException {
        Taxpayer taxpayer = new Taxpayer(); taxpayer.setId(10);
        IncomeType type = new IncomeType("Заробітна плата", new BigDecimal("18.0")); type.setId(1);
        Income income = new SalaryIncome(taxpayer, type, new BigDecimal("10000"), "Фірма", LocalDate.now());
        income.setAppliedBenefits(new ArrayList<>());

        // 1. Мокаємо збереження самого Income
        when(mockConnection.prepareStatement(contains("INSERT INTO incomes"), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockMainStmt);
        when(mockMainStmt.executeUpdate()).thenReturn(1);
        when(mockMainStmt.getGeneratedKeys()).thenReturn(mockKeysRs);
        when(mockKeysRs.next()).thenReturn(true);
        when(mockKeysRs.getInt(1)).thenReturn(42);

        // 2. ВИПРАВЛЕННЯ: Мокаємо збереження пільг (навіть якщо список порожній, метод викликається)
        when(mockConnection.prepareStatement(contains("INSERT INTO income_benefits")))
                .thenReturn(mockBenefitStmt);
        when(mockBenefitStmt.executeBatch()).thenReturn(new int[]{});

        when(mockTaxDao.save(any(Tax.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Income saved = incomeDao.save(income);

        assertEquals(42, saved.getId());
        assertFalse(saved.getCalculatedTaxes().isEmpty());
        verify(mockTaxDao, atLeast(2)).save(any(Tax.class));
    }

    @Test
    @DisplayName("delete() — успішно видаляє запис")
    void delete_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockMainStmt);
        when(mockMainStmt.executeUpdate()).thenReturn(1);

        incomeDao.delete(99);

        verify(mockMainStmt).setInt(1, 99);
        verify(mockMainStmt).executeUpdate();
    }
}