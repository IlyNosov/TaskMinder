package ru.ilynosov.taskminder.infrastructure.persistence;

import ru.ilynosov.taskminder.application.port.out.ReminderRepository;
import ru.ilynosov.taskminder.domain.model.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class PostgresReminderRepository implements ReminderRepository {

    private final DataSource dataSource;

    public PostgresReminderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Reminder reminder) {

        String sql = """
            INSERT INTO reminders (id, user_id, text, scheduled_at, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, reminder.getId());
            ps.setObject(2, reminder.getUserId());
            ps.setString(3, reminder.getText());
            ps.setTimestamp(4, Timestamp.valueOf(reminder.getScheduledAt()));
            ps.setString(5, reminder.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(reminder.getCreatedAt()));

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Reminder> findById(UUID id) {

        String sql = "SELECT * FROM reminders WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(map(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Reminder> findAll() {

        String sql = "SELECT * FROM reminders ORDER BY scheduled_at ASC";

        List<Reminder> reminders = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reminders.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return reminders;
    }

    @Override
    public List<Reminder> findActiveByUser(UUID userId) {

        String sql = """
            SELECT * FROM reminders
            WHERE user_id = ? AND status = 'ACTIVE'
        """;

        List<Reminder> reminders = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                reminders.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return reminders;
    }

    @Override
    public List<Reminder> findAllActive() {

        String sql = "SELECT * FROM reminders WHERE status = 'ACTIVE'";

        List<Reminder> reminders = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reminders.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return reminders;
    }

    @Override
    public void update(Reminder reminder) {

        String sql = """
            UPDATE reminders
            SET text = ?, scheduled_at = ?, status = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reminder.getText());
            ps.setTimestamp(2, Timestamp.valueOf(reminder.getScheduledAt()));
            ps.setString(3, reminder.getStatus().name());
            ps.setObject(4, reminder.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateStatus(UUID reminderId, String status) {

        String sql = "UPDATE reminders SET status = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setObject(2, reminderId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(UUID reminderId) {

        String sql = "DELETE FROM reminders WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, reminderId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Reminder map(ResultSet rs) throws SQLException {

        return Reminder.restore(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("text"),
                rs.getTimestamp("scheduled_at").toLocalDateTime(),
                ReminderStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
