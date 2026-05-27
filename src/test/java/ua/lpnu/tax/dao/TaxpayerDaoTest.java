package ua.lpnu.tax.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ua.lpnu.tax.model.Taxpayer;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("TaxpayerDao — тестування бази даних через моби (Mockito)")
class TaxpayerDaoTest {

    private TaxpayerDao taxpayerDao;

    // Мокаємо всі сутності JDBC
    @Mock private Connection mockConnection;
    @Mock private PreparedStatement mockPreparedStatement;
    @Mock private Statement mockStatement;
    @Mock private ResultSet mockResultSet;

    // Для перехоплення статичного методу DBConnection
    private MockedStatic<DBConnection> mockedDbConnection;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws SQLException {
        // Ініціалізуємо моки, позначені анотаціями @Mock
        mocks = MockitoAnnotations.openMocks(this);
        taxpayerDao = new TaxpayerDao();

        // Змушуємо DBConnection повертати наше фейкове з'єднання
        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Закриваємо статичний мок після кожного тесту
        mockedDbConnection.close();
        mocks.close();
    }

    @Test
    @DisplayName("findById() — успішно знаходить платника")
    void findById_success() throws SQLException {
        // Налаштовуємо поведінку моків
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Кажемо, що ResultSet має 1 рядок (next() повертає true один раз, потім false)
        when(mockResultSet.next()).thenReturn(true);
        
        // Імітуємо дані, які б повернула база
        when(mockResultSet.getInt("id")).thenReturn(10);
        when(mockResultSet.getString("full_name")).thenReturn("Шевченко Тарас");
        when(mockResultSet.getString("tax_id")).thenReturn("1234567890");
        when(mockResultSet.getInt("children_count")).thenReturn(2);
        when(mockResultSet.getString("special_status")).thenReturn("ВПО");

        // Викликаємо реальний метод
        Taxpayer result = taxpayerDao.findById(10);

        // Перевіряємо результати
        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Шевченко Тарас", result.getFullName());
        
        // Перевіряємо, чи підставилась правильна ID в SQL запит
        verify(mockPreparedStatement).setInt(1, 10);
    }

    @Test
    @DisplayName("findById() — повертає null, якщо запис не знайдено")
    void findById_notFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        // База порожня
        when(mockResultSet.next()).thenReturn(false);

        Taxpayer result = taxpayerDao.findById(99);

        assertNull(result);
    }

    @Test
    @DisplayName("findAll() — повертає список платників")
    void findAll_returnsList() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        
        // Імітуємо 2 рядки в базі
        when(mockResultSet.next()).thenReturn(true, true, false);
        
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("full_name")).thenReturn("Один", "Два");

        List<Taxpayer> list = taxpayerDao.findAll();

        assertEquals(2, list.size());
        assertEquals("Один", list.get(0).getFullName());
        assertEquals("Два", list.get(1).getFullName());
    }

    @Test
    @DisplayName("save() — успішно зберігає та встановлює згенерований ID")
    void save_success() throws SQLException {
        Taxpayer newTaxpayer = new Taxpayer("Іван", "111", 0, null);

        // Для save використовується prepareStatement з Statement.RETURN_GENERATED_KEYS
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        
        // Імітуємо повернення згенерованого ID = 42
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(42);

        Taxpayer saved = taxpayerDao.save(newTaxpayer);

        // Перевіряємо, що об'єкту присвоївся новий ID з "бази"
        assertEquals(42, saved.getId());
        verify(mockPreparedStatement).setString(1, "Іван");
    }

    @Test
    @DisplayName("update() — успішно оновлює запис")
    void update_success() throws SQLException {
        Taxpayer existing = new Taxpayer("Марія", "222", 1, null);
        existing.setId(5);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        Taxpayer updated = taxpayerDao.update(existing);

        assertEquals(5, updated.getId());
        verify(mockPreparedStatement).setString(1, "Марія");
        verify(mockPreparedStatement).setInt(5, 5); // ID в умові WHERE
    }

    @Test
    @DisplayName("delete() — успішно видаляє запис")
    void delete_success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        taxpayerDao.delete(7);

        verify(mockPreparedStatement).setInt(1, 7);
        verify(mockPreparedStatement).executeUpdate();
    }
}