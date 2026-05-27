package ua.lpnu.tax.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ua.lpnu.tax.model.IncomeType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("IncomeTypeDao — тестування бази даних типів доходу")
class IncomeTypeDaoTest {

    private IncomeTypeDao incomeTypeDao;

    @Mock private Connection mockConnection;
    @Mock private PreparedStatement mockPreparedStatement;
    @Mock private Statement mockStatement;
    @Mock private ResultSet mockResultSet;

    private MockedStatic<DBConnection> mockedDbConnection;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws SQLException {
        mocks = MockitoAnnotations.openMocks(this);
        incomeTypeDao = new IncomeTypeDao();

        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockedDbConnection.close();
        mocks.close();
    }

    @Test
    @DisplayName("findById() — успішно знаходить тип доходу")
    void findById_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        
        when(mockResultSet.getInt("id")).thenReturn(5);
        when(mockResultSet.getString("name")).thenReturn("Заробітна плата");
        when(mockResultSet.getBigDecimal("default_tax_rate")).thenReturn(new BigDecimal("18.00"));

        IncomeType result = incomeTypeDao.findById(5);

        assertNotNull(result);
        assertEquals(5, result.getId());
        assertEquals("Заробітна плата", result.getName());
        assertEquals(new BigDecimal("18.00"), result.getDefaultTaxRate());
    }

    @Test
    @DisplayName("findById() — повертає null, якщо не знайдено")
    void findById_notFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        assertNull(incomeTypeDao.findById(99));
    }

    @Test
    @DisplayName("findAll() — повертає список типів доходу")
    void findAll_returnsList() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("name")).thenReturn("Тип 1", "Тип 2");

        List<IncomeType> list = incomeTypeDao.findAll();

        assertEquals(2, list.size());
        assertEquals("Тип 1", list.get(0).getName());
    }

    @Test
    @DisplayName("save() — успішно зберігає та встановлює ID")
    void save_success() throws SQLException {
        IncomeType newType = new IncomeType("Новий тип", new BigDecimal("5.0"));

        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(20);

        IncomeType saved = incomeTypeDao.save(newType);

        assertEquals(20, saved.getId());
        verify(mockPreparedStatement).setString(1, "Новий тип");
        verify(mockPreparedStatement).setBigDecimal(2, new BigDecimal("5.0"));
    }

    @Test
    @DisplayName("update() — успішно оновлює запис")
    void update_success() throws SQLException {
        IncomeType existing = new IncomeType("Оновлений", new BigDecimal("9.0"));
        existing.setId(7);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        IncomeType updated = incomeTypeDao.update(existing);

        assertEquals(7, updated.getId());
        verify(mockPreparedStatement).setString(1, "Оновлений");
        verify(mockPreparedStatement).setInt(3, 7); // ID в умові WHERE
    }

    @Test
    @DisplayName("delete() — успішно видаляє запис")
    void delete_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        incomeTypeDao.delete(2);

        verify(mockPreparedStatement).setInt(1, 2);
        verify(mockPreparedStatement).executeUpdate();
    }
}