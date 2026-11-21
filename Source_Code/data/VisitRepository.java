package data;

import model.BookedReservation;
import model.Client;
import model.Resource;
import model.ResourceType;
import model.TimeSlot;
import model.Visit;
import model.VisitStatus;
import model.VisitType;
import model.WalkInTicket;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the "visits" table. One table holds both kinds of visit, so this class
 * decides whether a row becomes a BookedReservation or a WalkInTicket.
 */
public class VisitRepository implements Repository<Visit> {

    // One query that also pulls the client and the resource (LEFT JOIN: walk-ins may have no resource yet).
    private static final String SELECT_VISITS =
            "SELECT v.visit_id, v.visit_type, v.requested_type, v.visit_date, v.time_slot, "
          + "v.queue_number, v.purpose, v.status, "
          + "c.client_id, c.full_name, c.student_id, c.contact_no, "
          + "r.resource_id, r.name AS resource_name, r.resource_type, r.capacity, r.is_available "
          + "FROM visits v "
          + "JOIN clients c ON v.client_id = c.client_id "
          + "LEFT JOIN resources r ON v.resource_id = r.resource_id ";

    @Override
    public void add(Visit visit) throws SQLException {
        if (visit.getClient().getClientId() == 0) {
            throw new IllegalStateException("Save the client first so it has an id.");
        }
        String sql = "INSERT INTO visits (client_id, resource_id, visit_type, requested_type, "
                   + "visit_date, time_slot, queue_number, purpose, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, visit.getClient().getClientId());
            setResourceId(ps, 2, visit.getResource());
            ps.setString(3, visit.getVisitType().name());
            ps.setDate(5, Date.valueOf(visit.getVisitDate()));
            ps.setString(8, visit.getPurpose());
            ps.setString(9, visit.getStatus().name());

            if (visit instanceof BookedReservation) {
                BookedReservation booking = (BookedReservation) visit;
                ps.setNull(4, Types.VARCHAR);
                ps.setString(6, booking.getTimeSlot().name());
                ps.setNull(7, Types.INTEGER);
            } else {
                WalkInTicket ticket = (WalkInTicket) visit;
                ps.setString(4, ticket.getRequestedType().name());
                ps.setNull(6, Types.VARCHAR);
                ps.setInt(7, ticket.getQueueNumber());
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    visit.setVisitId(keys.getInt(1));
                }
            }
        }
    }

    /** Saves the things that can change later: resource, date, slot, purpose and status. */
    @Override
    public void update(Visit visit) throws SQLException {
        String sql = "UPDATE visits SET resource_id = ?, visit_date = ?, time_slot = ?, "
                   + "purpose = ?, status = ? WHERE visit_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setResourceId(ps, 1, visit.getResource());
            ps.setDate(2, Date.valueOf(visit.getVisitDate()));
            if (visit instanceof BookedReservation) {
                ps.setString(3, ((BookedReservation) visit).getTimeSlot().name());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, visit.getPurpose());
            ps.setString(5, visit.getStatus().name());
            ps.setInt(6, visit.getVisitId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int visitId) throws SQLException {
        String sql = "DELETE FROM visits WHERE visit_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Visit> getAll() throws SQLException {
        List<Visit> visits = new ArrayList<>();
        String sql = SELECT_VISITS + "ORDER BY v.visit_date DESC, v.visit_id DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                visits.add(mapRow(rs));
            }
        }
        return visits;
    }

    /** The next free queue number for that day and resource type (1 if nobody has queued yet). */
    public int nextQueueNumber(LocalDate date, ResourceType type) throws SQLException {
        String sql = "SELECT COALESCE(MAX(queue_number), 0) + 1 FROM visits "
                   + "WHERE visit_date = ? AND requested_type = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ---- helpers -------------------------------------------------------------------------

    private void setResourceId(PreparedStatement ps, int index, Resource resource) throws SQLException {
        if (resource == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, resource.getResourceId());
        }
    }

    /** Turns the current row into a Client + (maybe) a Resource + the right kind of Visit. */
    private Visit mapRow(ResultSet rs) throws SQLException {
        Client client = new Client(rs.getInt("client_id"), rs.getString("full_name"),
                rs.getString("student_id"), rs.getString("contact_no"));

        Resource resource = null;
        int resourceId = rs.getInt("resource_id");
        if (!rs.wasNull()) { // wasNull() tells us the last column read was SQL NULL
            resource = ResourceRepository.createResource(resourceId, rs.getString("resource_name"),
                    ResourceType.valueOf(rs.getString("resource_type")),
                    rs.getInt("capacity"), rs.getBoolean("is_available"));
        }

        int visitId = rs.getInt("visit_id");
        LocalDate date = rs.getDate("visit_date").toLocalDate();
        String purpose = rs.getString("purpose");
        VisitStatus status = VisitStatus.valueOf(rs.getString("status"));

        if (VisitType.valueOf(rs.getString("visit_type")) == VisitType.BOOKED) {
            TimeSlot slot = TimeSlot.valueOf(rs.getString("time_slot"));
            return new BookedReservation(visitId, client, resource, date, slot, purpose, status);
        }
        ResourceType requested = ResourceType.valueOf(rs.getString("requested_type"));
        return new WalkInTicket(visitId, client, resource, date, requested,
                rs.getInt("queue_number"), purpose, status);
    }
}
