package model;

public class StudentProgress {
    private int id;
    private int studentId;
    private int materialId;
    private String status;
    
    // Joined fields for display
    private String materialTitle;
    private int materialWeight;
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

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getMaterialId() { return materialId; }
    public String getStatus() { return status; }
    
    public String getMaterialTitle() { return materialTitle; }
    public void setMaterialTitle(String title) { this.materialTitle = title; }
    
    public int getMaterialWeight() { return materialWeight; }
    public void setMaterialWeight(int weight) { this.materialWeight = weight; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String url) { this.resourceUrl = url; }
    
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public java.util.List<MaterialContent> getContents() { return contents; }
    public void setContents(java.util.List<MaterialContent> contents) { this.contents = contents; }
}
