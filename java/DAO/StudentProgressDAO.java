package dao;

import model.StudentProgress;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentProgressDAO {

    public List<StudentProgress> getProgressByStudentId(int studentId) {
        List<StudentProgress> list = new ArrayList<>();
        // Join STUDENT_PROGRESS -> MATERIAL -> SKILL
        String sql = "SELECT sp.id, sp.student_id, sp.material_id, sp.status, " +
                     "m.title, m.weight, m.type, m.resource_url, s.skill_name " +
                     "FROM STUDENT_PROGRESS sp " +
                     "JOIN MATERIAL m ON sp.material_id = m.id " +
                     "JOIN SKILL s ON m.skill_id = s.skill_id " +
                     "WHERE sp.student_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                StudentProgress sp = new StudentProgress(
                    rs.getInt("id"),
                    rs.getInt("student_id"),
                    rs.getInt("material_id"),
                    rs.getString("status")
                );
                sp.setMaterialTitle(rs.getString("title"));
                sp.setMaterialWeight(rs.getInt("weight"));
                sp.setType(rs.getString("type"));
                sp.setResourceUrl(rs.getString("resource_url"));
                sp.setSkillName(rs.getString("skill_name"));
                
                // Fetch new Content Items
                // DEPRECATED: We are reverting to 1:1 Module = Resource
                // dao.MaterialDAO mDAO = new dao.MaterialDAO();
                // sp.setContents(mDAO.getContentsByMaterialId(sp.getMaterialId()));
                
                list.add(sp);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    // Enroll student in a specific skill by assigning all its materials
    public void enrollStudentInSkill(int studentId, int skillId) {
        // Step 1: Fetch all material IDs first (Read Operation)
        java.util.List<Integer> materialIds = new java.util.ArrayList<>();
        String sqlSelect = "SELECT id FROM MATERIAL WHERE skill_id = ?";
        
        try (java.sql.Connection conn = util.DBConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {
             
            pstmt.setInt(1, skillId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    materialIds.add(rs.getInt("id"));
                }
            }
        } catch (java.sql.SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Step 2: Insert Progress for each material (Write Operation)
        // This is done after the read connection is closed to avoid SQLITE_BUSY locks
        for (Integer matId : materialIds) {
            if (!isProgressExists(studentId, matId)) {
                insertProgress(studentId, matId, "Not Started");
            }
        }
    }

    public boolean isProgressExists(int studentId, int materialId) {
        String sql = "SELECT 1 FROM STUDENT_PROGRESS WHERE student_id = ? AND material_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, materialId);
            return pstmt.executeQuery().next();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void insertProgress(int studentId, int materialId, String status) {
        String sql = "INSERT INTO STUDENT_PROGRESS (student_id, material_id, status) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, materialId);
            pstmt.setString(3, status);
            pstmt.executeUpdate();
         } catch (SQLException | ClassNotFoundException ex) {
             ex.printStackTrace();
         }
    }
    
    public boolean updateStatus(int studentId, int materialId, String status) {
        String sql = "UPDATE STUDENT_PROGRESS SET status = ? WHERE student_id = ? AND material_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, studentId);
            pstmt.setInt(3, materialId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
