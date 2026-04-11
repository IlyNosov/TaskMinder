package ru.ilynosov.taskminder.infrastructure.persistence;

import ru.ilynosov.taskminder.application.port.out.UserRepository;
import ru.ilynosov.taskminder.domain.model.User;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class PostgresUserRepository implements UserRepository {

    private final DataSource dataSource;

    public PostgresUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findByTelegramId(long telegramId) {

        String sql = "SELECT * FROM users WHERE telegram_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, telegramId);

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
    public User save(User user) {

        String sql = """
            INSERT INTO users (id, telegram_id, timezone, created_at)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, user.getId());
            ps.setLong(2, user.getTelegramId());
            ps.setString(3, user.getTimezone());
            ps.setTimestamp(4, Timestamp.valueOf(user.getCreatedAt()));

            ps.executeUpdate();

            return user;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User map(ResultSet rs) throws SQLException {

        return new User(
                rs.getObject("id", UUID.class),
                rs.getLong("telegram_id"),
                rs.getString("timezone"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}