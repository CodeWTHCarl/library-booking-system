package data;

import java.sql.SQLException;
import java.util.List;

/**
 * ABSTRACTION: the contract every data class must follow. The service layer only knows
 * this interface, not how the data is stored (MySQL here).
 */
public interface Repository<T> {
    void add(T item) throws SQLException;

    void update(T item) throws SQLException;

    void delete(int id) throws SQLException;

    List<T> getAll() throws SQLException;
}
