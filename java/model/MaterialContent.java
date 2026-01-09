package model;

public class MaterialContent {
    private int id;
    private int materialId;
    private String type; // TEXT, LINK, FILE
    private String resourceUrl;
    private String displayLabel;
    private int position;

    public MaterialContent() {}

    public MaterialContent(int id, int materialId, String type, String resourceUrl, String displayLabel, int position) {
        this.id = id;
        this.materialId = materialId;
        this.type = type;
        this.resourceUrl = resourceUrl;
        this.displayLabel = displayLabel;
        this.position = position;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMaterialId() { return materialId; }
    public void setMaterialId(int materialId) { this.materialId = materialId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }

    public String getDisplayLabel() { return displayLabel; }
    public void setDisplayLabel(String displayLabel) { this.displayLabel = displayLabel; }
    
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
