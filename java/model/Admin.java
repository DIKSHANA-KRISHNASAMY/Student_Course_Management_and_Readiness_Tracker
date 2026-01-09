package model;

/**
 * Admin class demonstrating INHERITANCE (extends User).
 */
public class Admin extends User {

    public Admin(String username, String password) {
        super(0, username, password, "ADMIN");
    }

    // POLYMORPHISM: Implementation of abstract method
    @Override
    public String getDashboardPath() {
        return "/admin_dashboard"; // Admins go to admin dashboard
    }
}
