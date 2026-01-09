package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/stack_change_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "diks@123";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    // Test the connection
    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                System.out.println("Connection to MySQL has been established.");
                conn.close();
            }
        } catch (Exception e) {
            System.out.println("Connection Failed.");
            e.printStackTrace();
        }
    }
}
