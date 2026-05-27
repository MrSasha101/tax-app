package ua.lpnu.tax.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ua.lpnu.tax.model.income.Income;
import ua.lpnu.tax.model.income.SalaryIncome;
import ua.lpnu.tax.model.tax.MilitaryLevy;
import ua.lpnu.tax.model.tax.PersonalIncomeTax;
import ua.lpnu.tax.model.tax.Tax;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("TaxDao — тестування бази даних податків")
class TaxDaoTest {

    private TaxDao taxDao;

    @Mock private Connection mockConnection;
    @Mock private PreparedStatement mockPreparedStatement;
    @Mock private ResultSet mockResultSet;

    private MockedStatic<DBConnection> mockedDbConnection;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws SQLException {
        mocks = MockitoAnnotations.openMocks(this);
        taxDao = new TaxDao();

        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockedDbConnection.close();
        mocks.close();
    }

    @Test
    @DisplayName("findByIncomeId() — успішно повертає список податків")
    void findByIncomeId_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, true, false);

        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("tax_name")).thenReturn("ПДФО", "Військовий збір");
        when(mockResultSet.getBigDecimal("tax_amount"))
                .thenReturn(new BigDecimal("1800.00"), new BigDecimal("500.00"));
        when(mockResultSet.getBigDecimal("rate_at_time"))
                .thenReturn(new BigDecimal("18.0"), new BigDecimal("5.0"));

        List<Tax> list = taxDao.findByIncomeId(100);

        assertEquals(2, list.size());
        assertTrue(list.get(0) instanceof PersonalIncomeTax);
        assertTrue(list.get(1) instanceof MilitaryLevy);
    }

    @Test
    @DisplayName("save() — успішно зберігає податок")
    void save_success() throws SQLException {
        Income dummyIncome = new SalaryIncome();
        dummyIncome.setId(55);

        Tax newTax = new PersonalIncomeTax();
        newTax.setIncome(dummyIncome);
        newTax.setTaxName("ПДФО"); // ВИПРАВЛЕННЯ: Додано встановлення імені податку
        newTax.setTaxAmount(new BigDecimal("900.00"));
        newTax.setRateAtTime(new BigDecimal("18.0"));

        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(77);

        Tax saved = taxDao.save(newTax);

        assertEquals(77, saved.getId());
        verify(mockPreparedStatement).setInt(1, 55);
        verify(mockPreparedStatement).setString(2, "ПДФО");
    }

    @Test
    @DisplayName("update() — заглушка, повертає той самий об'єкт")
    void update_returnsSameEntity() throws SQLException {
        Tax tax = new PersonalIncomeTax();
        Tax result = taxDao.update(tax);
        assertSame(tax, result);
    }

    @Test
    @DisplayName("delete() — успішно видаляє запис за ID податку")
    void delete_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        taxDao.delete(12);
        verify(mockPreparedStatement).setInt(1, 12);
    }

    @Test
    @DisplayName("deleteByIncomeId() — успішно видаляє всі податки прив'язані до доходу")
    void deleteByIncomeId_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(2);
        taxDao.deleteByIncomeId(55);
        verify(mockPreparedStatement).setInt(1, 55);
    }
}