package model;

/**
 * Abstract Base Class demonstrating INHERITANCE and ABSTRACTION.
 * Common properties for all users are defined here.
 */
public abstract class User {
    // ENCAPSULATION: private fields accessed via getters/setters
    private int id;
    private String username; // or email for student
    private String password;
    private String role; // "ADMIN" or "STUDENT"

    public User(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // POLYMORPHISM: Abstract method forces subclasses to implement their own dashboard logic
    public abstract String getDashboardPath();

    // Getters and Setters demonstrating ENCAPSULATION
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
