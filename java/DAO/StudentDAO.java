package dao;

import model.Student;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentDAO {

    public java.util.List<Student> getAllStudents() {
        java.util.List<Student> students = new java.util.ArrayList<>();
        String sql = "SELECT * FROM STUDENT";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                students.add(new Student(
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    "PROTECTED" // Password not exposed in list
                ));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return students;
    }
    
    public Student getStudentByEmail(String email) {
        String sql = "SELECT * FROM STUDENT WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Student(
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password")
                );
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null; // Not found or error
    }

    public Student getStudentById(int id) {
        String sql = "SELECT * FROM STUDENT WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Student(
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password")
                );
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int addStudent(Student student) {
        String sql = "INSERT INTO STUDENT (name, email, password) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getUsername()); // Email is stored in username field of User
            pstmt.setString(3, student.getPassword());
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
            return 0; // Failed
            
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public boolean deleteStudent(int studentId) {
        // Delete in order: Progress -> Enrollments -> Student (FK constraints)
        String sqlDelProgress = "DELETE FROM STUDENT_PROGRESS WHERE student_id = ?";
        String sqlDelEnroll = "DELETE FROM ENROLLMENT WHERE student_id = ?";
        String sqlDelStudent = "DELETE FROM STUDENT WHERE student_id = ?";
        
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Delete Progress
                try (PreparedStatement ps = conn.prepareStatement(sqlDelProgress)) {
                    ps.setInt(1, studentId);
                    ps.executeUpdate();
                }
                // 2. Delete Enrollments
                try (PreparedStatement ps = conn.prepareStatement(sqlDelEnroll)) {
                    ps.setInt(1, studentId);
                    ps.executeUpdate();
                }
                // 3. Delete Student
                boolean success;
                try (PreparedStatement ps = conn.prepareStatement(sqlDelStudent)) {
                    ps.setInt(1, studentId);
                    success = ps.executeUpdate() > 0;
                }
                conn.commit();
                return success;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
