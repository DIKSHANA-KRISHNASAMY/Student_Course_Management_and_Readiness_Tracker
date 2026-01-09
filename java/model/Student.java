package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Student class demonstrating INHERITANCE (extends User).
 */
public class Student extends User {
    private String name;
    // List to hold enrolled courses - representing aggregation
    private List<StudentProgress> progressList;

    public Student(int id, String name, String email, String password) {
        super(id, email, password, "STUDENT");
        this.name = name;
        this.progressList = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<StudentProgress> getProgressList() { return progressList; }
    public void setProgressList(List<StudentProgress> progressList) { this.progressList = progressList; }

    // POLYMORPHISM: Implementation of abstract method
    @Override
    public String getDashboardPath() {
        return "/dashboard"; // Students go to standard dashboard
    }
}
