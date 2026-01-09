package dao;

import model.Skill;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SkillDAO {

    public List<Skill> getAllSkills() {
        List<Skill> skills = new ArrayList<>();
        String sql = "SELECT * FROM SKILL";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                skills.add(new Skill(
                    rs.getInt("skill_id"),
                    rs.getString("skill_name"),
                    rs.getString("image_url"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return skills;
    }
    
    public Skill getSkillById(int skillId) {
        String sql = "SELECT * FROM SKILL WHERE skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, skillId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Skill(
                        rs.getInt("skill_id"),
                        rs.getString("skill_name"),
                        rs.getString("image_url"),
                        rs.getString("status")
                    );
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addSkill(Skill skill) {
        String sql = "INSERT INTO SKILL (skill_name, image_url, status) VALUES (?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, skill.getSkillName());
            pstmt.setString(2, skill.getImageUrl());
            pstmt.setString(3, "active"); // Default
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateStatus(int skillId, String status) {
        String sql = "UPDATE SKILL SET status = ? WHERE skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, skillId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteSkill(int skillId) {
        // Must handle constraints: Enrollments, Materials, StudentProgress
        // Simplified Logic: Delete children first (if no CASCADE on DB)
        
        String sqlDelEnroll = "DELETE FROM ENROLLMENT WHERE skill_id = ?";
        // Progress linked to materials, Materials linked to Skill
        // We'll trust MaterialDAO to handle cleaning materials if we delete them manually, 
        // OR we just delete materials by skill_id first.
        
        String sqlDelProgress = "DELETE FROM STUDENT_PROGRESS WHERE material_id IN (SELECT id FROM MATERIAL WHERE skill_id = ?)";
        String sqlDelMaterials = "DELETE FROM MATERIAL WHERE skill_id = ?";
        String sqlDelSkill = "DELETE FROM SKILL WHERE skill_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // Transaction
            try {
                // 1. Delete Student Progress for this skill's materials
                try (PreparedStatement ps = conn.prepareStatement(sqlDelProgress)) {
                    ps.setInt(1, skillId);
                    ps.executeUpdate();
                }
                // 2. Delete Materials
                try (PreparedStatement ps = conn.prepareStatement(sqlDelMaterials)) {
                    ps.setInt(1, skillId);
                    ps.executeUpdate();
                }
                // 3. Delete Enrollments
                try (PreparedStatement ps = conn.prepareStatement(sqlDelEnroll)) {
                    ps.setInt(1, skillId);
                    ps.executeUpdate();
                }
                // 4. Delete Skill
                boolean success;
                try (PreparedStatement ps = conn.prepareStatement(sqlDelSkill)) {
                    ps.setInt(1, skillId);
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
