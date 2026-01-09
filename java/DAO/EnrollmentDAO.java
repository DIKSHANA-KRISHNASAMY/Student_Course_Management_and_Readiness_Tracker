package dao;

import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO {

    public void enroll(int studentId, int skillId) {
        String sql = "INSERT IGNORE INTO ENROLLMENT (student_id, skill_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, skillId);
            pstmt.executeUpdate();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public List<Integer> getEnrolledSkillIds(int studentId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT skill_id FROM ENROLLMENT WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getInt("skill_id"));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<model.Student> getEnrolledStudentsBySkill(int skillId) {
        List<model.Student> list = new ArrayList<>();
        String sql = "SELECT s.student_id, s.name, s.email FROM STUDENT s " +
                     "JOIN ENROLLMENT e ON s.student_id = e.student_id " +
                     "WHERE e.skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, skillId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new model.Student(
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    "PROTECTED"
                ));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getTotalUniqueEnrolledStudents() {
        String sql = "SELECT COUNT(DISTINCT student_id) FROM ENROLLMENT";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<model.Student> getAllActiveStudents() {
        List<model.Student> list = new ArrayList<>();
        String sql = "SELECT DISTINCT s.student_id, s.name, s.email FROM STUDENT s " +
                     "JOIN ENROLLMENT e ON s.student_id = e.student_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new model.Student(
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    "PROTECTED"
                ));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }
    public List<model.Skill> getEnrolledSkills(int studentId) {
        List<model.Skill> list = new ArrayList<>();
        String sql = "SELECT s.skill_id, s.skill_name, s.image_url, s.status FROM SKILL s " +
                     "JOIN ENROLLMENT e ON s.skill_id = e.skill_id " +
                     "WHERE e.student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new model.Skill(
                    rs.getInt("skill_id"),
                    rs.getString("skill_name"),
                    rs.getString("image_url"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }
}
