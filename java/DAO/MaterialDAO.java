package dao;

import model.Material;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MaterialDAO {

    public int addMaterial(Material material) {
        String sql = "INSERT INTO MATERIAL (skill_id, title, type, resource_url, weight) VALUES (?, ?, ?, ?, ?)";
        int savedId = -1;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, material.getSkillId());
            pstmt.setString(2, material.getTitle());
            pstmt.setString(3, material.getType());
            pstmt.setString(4, material.getResourceUrl());
            pstmt.setInt(5, material.getWeight());
            
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                savedId = rs.getInt(1);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return savedId;
    }
    
    public List<Material> getMaterialsBySkillId(int skillId) {
        List<Material> list = new ArrayList<>();
        String sql = "SELECT * FROM MATERIAL WHERE skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, skillId);
            ResultSet rs = pstmt.executeQuery();
            
            while(rs.next()) {
                list.add(new Material(
                    rs.getInt("id"),
                    rs.getInt("skill_id"),
                    rs.getString("title"),
                    rs.getString("type"),
                    rs.getString("resource_url"),
                    rs.getInt("weight")
                ));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public int getTotalWeightBySkillId(int skillId) {
        String sql = "SELECT SUM(weight) FROM MATERIAL WHERE skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, skillId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void updateMaterial(Material material) {
        String sql = "UPDATE MATERIAL SET title = ?, type = ?, resource_url = ?, weight = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, material.getTitle());
            pstmt.setString(2, material.getType());
            pstmt.setString(3, material.getResourceUrl());
            pstmt.setInt(4, material.getWeight());
            pstmt.setInt(5, material.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Material getMaterialById(int id) {
        String sql = "SELECT * FROM MATERIAL WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Material(
                    rs.getInt("id"),
                    rs.getInt("skill_id"),
                    rs.getString("title"),
                    rs.getString("type"),
                    rs.getString("resource_url"),
                    rs.getInt("weight")
                );
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String deleteMaterial(int id) {
        String sqlDelProgress = "DELETE FROM STUDENT_PROGRESS WHERE material_id = ?";
        String sqlDelMaterial = "DELETE FROM MATERIAL WHERE id = ?";
        
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // Delete Progress
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDelProgress)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
            // Delete Material
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDelMaterial)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
            
            conn.commit(); // Commit Transaction
            return null; // Success (no error)
            
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return e.getMessage(); // Return specific error
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
