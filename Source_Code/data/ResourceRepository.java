package data;

import model.ComputerStation;
import model.Resource;
import model.ResourceType;
import model.StudyRoom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes the "resources" table. Turns each row into a StudyRoom or a ComputerStation. */
public class ResourceRepository implements Repository<Resource> {

    /**
     * Builds the correct child object for a database row. VisitRepository reuses this too.
     * The caller just receives a Resource; which child it really is gets decided here.
     */
    public static Resource createResource(int id, String name, ResourceType type,
                                          int capacity, boolean available) {
        if (type == ResourceType.ROOM) {
            return new StudyRoom(id, name, capacity, available);
        }
        return new ComputerStation(id, name, available);
    }

    @Override
    public void add(Resource resource) throws SQLException {
        String sql = "INSERT INTO resources (name, resource_type, capacity, is_available) VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, resource.getName());
            ps.setString(2, resource.getResourceType().name());
            ps.setInt(3, resource.getMaxUsers()); // polymorphism: a room gives its capacity, a PC gives 1
            ps.setBoolean(4, resource.isAvailable());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    resource.setResourceId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Resource resource) throws SQLException {
        String sql = "UPDATE resources SET name = ?, capacity = ?, is_available = ? WHERE resource_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, resource.getName());
            ps.setInt(2, resource.getMaxUsers());
            ps.setBoolean(3, resource.isAvailable());
            ps.setInt(4, resource.getResourceId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int resourceId) throws SQLException {
        String sql = "DELETE FROM resources WHERE resource_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Resource> getAll() throws SQLException {
        List<Resource> resources = new ArrayList<>();
        String sql = "SELECT resource_id, name, resource_type, capacity, is_available "
                + "FROM resources ORDER BY resource_type, name";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resources.add(createResource(rs.getInt("resource_id"), rs.getString("name"),
                        ResourceType.valueOf(rs.getString("resource_type")),
                        rs.getInt("capacity"), rs.getBoolean("is_available")));
            }
        }
        return resources;
    }
}
