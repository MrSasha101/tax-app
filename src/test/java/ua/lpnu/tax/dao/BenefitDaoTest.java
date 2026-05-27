package ua.lpnu.tax.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ua.lpnu.tax.model.Benefit;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("BenefitDao — тестування бази даних пільг")
class BenefitDaoTest {

    private BenefitDao benefitDao;

    @Mock private Connection mockConnection;
    @Mock private PreparedStatement mockPreparedStatement;
    @Mock private Statement mockStatement;
    @Mock private ResultSet mockResultSet;

    private MockedStatic<DBConnection> mockedDbConnection;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws SQLException {
        mocks = MockitoAnnotations.openMocks(this);
        benefitDao = new BenefitDao();

        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockedDbConnection.close();
        mocks.close();
    }

    @Test
    @DisplayName("findById() — успішно знаходить пільгу")
    void findById_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("name")).thenReturn("Студентська");
        when(mockResultSet.getString("description")).thenReturn("Знижка для студентів");
        when(mockResultSet.getBigDecimal("discount_amount")).thenReturn(new BigDecimal("500.00"));
        when(mockResultSet.getBigDecimal("discount_percentage")).thenReturn(null);

        Benefit result = benefitDao.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Студентська", result.getName());
        assertEquals(new BigDecimal("500.00"), result.getDiscountAmount());
        
        verify(mockPreparedStatement).setInt(1, 1);
    }

    @Test
    @DisplayName("findById() — повертає null, якщо не знайдено")
    void findById_notFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        assertNull(benefitDao.findById(99));
    }

    @Test
    @DisplayName("findAll() — повертає список пільг")
    void findAll_returnsList() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("name")).thenReturn("Пільга 1", "Пільга 2");

        List<Benefit> list = benefitDao.findAll();

        assertEquals(2, list.size());
        assertEquals("Пільга 1", list.get(0).getName());
        assertEquals("Пільга 2", list.get(1).getName());
    }

    @Test
    @DisplayName("save() — успішно зберігає та встановлює ID")
    void save_success() throws SQLException {
        Benefit newBenefit = new Benefit("ВПО", "Для переселенців", null, new BigDecimal("10.0"));

        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(15); // згенерований ID

        Benefit saved = benefitDao.save(newBenefit);

        assertEquals(15, saved.getId());
        verify(mockPreparedStatement).setString(1, "ВПО");
        verify(mockPreparedStatement).setBigDecimal(4, new BigDecimal("10.0"));
    }

    @Test
    @DisplayName("update() — успішно оновлює запис")
    void update_success() throws SQLException {
        Benefit existing = new Benefit("Стара", "Опис", BigDecimal.ZERO, BigDecimal.ZERO);
        existing.setId(3);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        Benefit updated = benefitDao.update(existing);

        assertEquals(3, updated.getId());
        verify(mockPreparedStatement).setString(1, "Стара");
        verify(mockPreparedStatement).setInt(5, 3); // ID в умові WHERE
    }

    @Test
    @DisplayName("delete() — успішно видаляє запис")
    void delete_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        benefitDao.delete(10);

        verify(mockPreparedStatement).setInt(1, 10);
        verify(mockPreparedStatement).executeUpdate();
    }
}