package data;

import model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes the "clients" table. Follows the Repository contract (interface). */
public class ClientRepository implements Repository<Client> {

    @Override
    public void add(Client client) throws SQLException {
        String sql = "INSERT INTO clients (full_name, student_id, contact_no) VALUES (?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, client.getFullName());
            ps.setString(2, client.getStudentId());
            ps.setString(3, client.getContactNo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    client.setClientId(keys.getInt(1)); // give the object the id MySQL created
                }
            }
        }
    }

    @Override
    public void update(Client client) throws SQLException {
        String sql = "UPDATE clients SET full_name = ?, student_id = ?, contact_no = ? WHERE client_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, client.getFullName());
            ps.setString(2, client.getStudentId());
            ps.setString(3, client.getContactNo());
            ps.setInt(4, client.getClientId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int clientId) throws SQLException {
        String sql = "DELETE FROM clients WHERE client_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Client> getAll() throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT client_id, full_name, student_id, contact_no FROM clients ORDER BY full_name";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clients.add(new Client(rs.getInt("client_id"), rs.getString("full_name"),
                        rs.getString("student_id"), rs.getString("contact_no")));
            }
        }
        return clients;
    }

    /** Extra lookup: returns the client with this student ID, or null if there is none. */
    public Client findByStudentId(String studentId) throws SQLException {
        String sql = "SELECT client_id, full_name, student_id, contact_no FROM clients WHERE student_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Client(rs.getInt("client_id"), rs.getString("full_name"),
                            rs.getString("student_id"), rs.getString("contact_no"));
                }
            }
        }
        return null;
    }
}
