package model;

public class Material {
    private int id;
    private int skillId;
    private String title;
    private String type; // Kept for legacy/simple modules, but defaulting to MODULE
    private String resourceUrl;
    private int weight;
    private java.util.List<MaterialContent> contents = new java.util.ArrayList<>();

    public Material() {}

    public Material(int id, int skillId, String title, String type, String resourceUrl, int weight) {
        this.id = id;
        this.skillId = skillId;
        this.title = title;
        this.type = type;
        this.resourceUrl = resourceUrl;
        this.weight = weight;
    }
    
    public java.util.List<MaterialContent> getContents() { return contents; }
    public void setContents(java.util.List<MaterialContent> contents) { this.contents = contents; }
    public void addContent(MaterialContent content) { this.contents.add(content); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getSkillId() { return skillId; }
    public void setSkillId(int skillId) { this.skillId = skillId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }
    
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
}
