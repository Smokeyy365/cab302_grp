package com.cab302.eduplanner.repository;

import com.cab302.eduplanner.DatabaseConnection;
import com.cab302.eduplanner.model.Task;
import com.cab302.eduplanner.util.DateUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {

    private static final String SQL_SELECT_BY_USER = """
        SELECT task_id, user_id, subject, title, due_date, notes, weight, achieved_mark, max_mark,
               created_at, updated_at
        FROM tasks
        WHERE user_id = ?
        ORDER BY (CASE WHEN due_date IS NULL THEN 1 ELSE 0 END), due_date ASC
        """;

    private static final String SQL_SELECT_ONE = """
        SELECT task_id, user_id, subject, title, due_date, notes, weight, achieved_mark, max_mark,
               created_at, updated_at
        FROM tasks
        WHERE task_id = ? AND user_id = ?
        """;

    private static final String SQL_INSERT = """
        INSERT INTO tasks (user_id, subject, title, due_date, notes, weight, achieved_mark, max_mark)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String SQL_UPDATE = """
        UPDATE tasks
        SET subject = ?, title = ?, due_date = ?, notes = ?, weight = ?, achieved_mark = ?, max_mark = ?,
            updated_at = datetime('now')
        WHERE task_id = ? AND user_id = ?
        """;

    private static final String SQL_DELETE = "DELETE FROM tasks WHERE task_id = ? AND user_id = ?";

    // Read
    public List<Task> findByUserId(long userId) {
        List<Task> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_SELECT_BY_USER)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("findByUserId failed: " + e.getMessage());
        }
        return out;
    }

    public Optional<Task> findById(long taskId, long userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_SELECT_ONE)) {
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("findById failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    // Create
    public Optional<Long> insert(Task t) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, t.getUserId());
            ps.setString(2, nullIfBlank(t.getSubject()));
            ps.setString(3, t.getTitle());
            ps.setString(4, DateUtil.toIso(t.getDueDate()));
            ps.setString(5, nullIfBlank(t.getNotes()));
            setNullableInt(ps, 6, t.getWeight());
            setNullableDouble(ps, 7, t.getAchievedMark());
            setNullableDouble(ps, 8, t.getMaxMark());

            int rows = ps.executeUpdate();
            if (rows == 0) return Optional.empty();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return Optional.of(keys.getLong(1));
            }
        } catch (SQLException e) {
            System.err.println("insert failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    // Update
    public boolean update(Task t) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, nullIfBlank(t.getSubject()));
            ps.setString(2, t.getTitle());
            ps.setString(3, DateUtil.toIso(t.getDueDate()));
            ps.setString(4, nullIfBlank(t.getNotes()));
            setNullableInt(ps, 5, t.getWeight());
            setNullableDouble(ps, 6, t.getAchievedMark());
            setNullableDouble(ps, 7, t.getMaxMark());
            ps.setLong(8, t.getTaskId());
            ps.setLong(9, t.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("update failed: " + e.getMessage());
            return false;
        }
    }

    // Delete
    public boolean delete(long taskId, long userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("delete failed: " + e.getMessage());
            return false;
        }
    }

    // Helpers
    private static Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setTaskId(rs.getLong("task_id"));
        t.setUserId(rs.getLong("user_id"));
        t.setSubject(rs.getString("subject"));
        t.setTitle(rs.getString("title"));
        t.setDueDate(DateUtil.parseIsoDateOrNull(rs.getString("due_date")));
        t.setNotes(rs.getString("notes"));
        t.setWeight((Integer) rs.getObject("weight")); // nullable
        t.setAchievedMark((Double) rs.getObject("achieved_mark"));
        t.setMaxMark((Double) rs.getObject("max_mark"));
        t.setCreatedAt(DateUtil.parseIsoDateTimeOrNull(rs.getString("created_at")));
        t.setUpdatedAt(DateUtil.parseIsoDateTimeOrNull(rs.getString("updated_at")));
        return t;
    }

    private static void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER); else ps.setInt(idx, v);
    }
    private static void setNullableDouble(PreparedStatement ps, int idx, Double v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.REAL); else ps.setDouble(idx, v);
    }
    private static String nullIfBlank(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
