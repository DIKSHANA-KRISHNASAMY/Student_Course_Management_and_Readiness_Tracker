package model;

public class StudentProgress {
    private int id;
    private int studentId;
    private int materialId;
    private String status;
    
    private String materialTitle;
    private int materialWeight; 
    private int totalCourseWeight; 
    private String type;
    private String resourceUrl;
    private String skillName;
    private java.util.List<MaterialContent> contents = new java.util.ArrayList<>();

    public StudentProgress(int id, int studentId, int materialId, String status) {
        this.id = id;
        this.studentId = studentId;
        this.materialId = materialId;
        this.status = status;
    }

    public int getMaterialWeight() { return materialWeight; }
    public void setMaterialWeight(int weight) { this.materialWeight = weight; }

    public int getTotalCourseWeight() { return totalCourseWeight; }
    public void setTotalCourseWeight(int totalCourseWeight) { this.totalCourseWeight = totalCourseWeight; }

    public String getStatus() { return status; }

    /**
     * Calculates readiness contribution based on formula:
     * (materialWeight / 100) * totalCourseWeight
     */
    public double getReadinessContribution() {
        if (!"COMPLETED".equalsIgnoreCase(status)) {
            return 0;
        }
        return (materialWeight / 100.0) * totalCourseWeight;
    }
}
