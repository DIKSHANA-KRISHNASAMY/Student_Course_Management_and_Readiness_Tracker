package model;

public class Skill {
    private int skillId;
    private String skillName;

    private String imageUrl;
    private String status = "active"; // Default to active

    public Skill() {}

    public Skill(int skillId, String skillName) {
        this.skillId = skillId;
        this.skillName = skillName;
    }

    public Skill(int skillId, String skillName, String imageUrl) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.imageUrl = imageUrl;
    }

    public Skill(int skillId, String skillName, String imageUrl, String status) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    public int getSkillId() { return skillId; }
    public void setSkillId(int skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getStatus() { return status != null ? status : "active"; }
    public void setStatus(String status) { this.status = status; }
}
