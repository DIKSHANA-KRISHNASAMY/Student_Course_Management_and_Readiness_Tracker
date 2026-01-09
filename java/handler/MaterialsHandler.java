package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;

import dao.MaterialDAO;
import dao.EnrollmentDAO;
import dao.StudentProgressDAO;
import dao.SkillDAO;
import model.Material;
import model.Student;
import model.User;
import model.Admin;
import util.SimpleSessionManager;
import util.MultipartParser;

/**
 * Handles Material/Content management for courses.
 */
public class MaterialsHandler implements HttpHandler {

    private MaterialDAO materialDAO = new MaterialDAO();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private StudentProgressDAO progressDAO = new StudentProgressDAO();
    private SkillDAO skillDAO = new SkillDAO();
    private static final String UPLOAD_DIR = "src/main/webapp/uploads";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        User user = SimpleSessionManager.getSessionUser(exchange);
        if (user == null || !(user instanceof Admin)) {
            exchange.getResponseHeaders().set("Location", "/admin_login.html");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        String method = exchange.getRequestMethod();
        
        if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange);
        } else {
            handleGet(exchange);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        Map<String, String> params = new HashMap<>();
        String fileUrl = null;
        
        if (contentType != null && contentType.contains("multipart/form-data")) {
            MultipartParser parser = new MultipartParser();
            parser.parse(exchange.getRequestBody(), contentType);
            params = parser.getFields();
            
            if (parser.hasFile()) {
                String fileName = System.currentTimeMillis() + "_" + parser.getFileName();
                File uploadDir = new File(UPLOAD_DIR);
                if (!uploadDir.exists()) uploadDir.mkdirs();
                
                try (FileOutputStream fos = new FileOutputStream(new File(uploadDir, fileName))) {
                    fos.write(parser.getFileData());
                }
                fileUrl = fileName;
            }
        } else {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            params = parseQuery(body);
        }
        
        String skillIdStr = params.get("skillId");
        int skillId = (skillIdStr != null) ? Integer.parseInt(skillIdStr) : 0;
        String action = params.get("action");
        String errorMsg = null;
        
        if ("delete".equals(action)) {
            String idStr = params.get("id");
            if (idStr != null) {
                materialDAO.deleteMaterial(Integer.parseInt(idStr));
            }
        } else if ("add".equals(action) || "update".equals(action)) {
            String title = params.get("title");
            String weightStr = params.get("weight");
            int weight = (weightStr != null) ? Integer.parseInt(weightStr) : 0;
            String type = params.get("type");
            String idStr = params.get("id");
            int editingId = (idStr != null) ? Integer.parseInt(idStr) : -1;
            
            // Weight Validation Logic
            int currentTotal = 0;
            List<Material> existingMaterials = materialDAO.getMaterialsBySkillId(skillId);
            for(Material em : existingMaterials) {
                if(em.getId() == editingId) {
                    continue; // Skip the one we are editing
                }
                currentTotal += em.getWeight();
            }
            
            if (currentTotal + weight > 100) {
                 errorMsg = "Total weight cannot exceed 100 percent. Current total: " + currentTotal + ". You tried to add: " + weight + ".";
            } else {
                String resUrl = getResourceUrl(type, params, fileUrl);
                
                if ("add".equals(action)) {
                     Material m = new Material();
                     m.setSkillId(skillId);
                     m.setTitle(title);
                     m.setWeight(weight);
                     m.setType(type);
                     m.setResourceUrl(resUrl != null ? resUrl : "");
                     
                     int newMatId = materialDAO.addMaterial(m);
                     if (newMatId != -1) {
                         List<Student> students = enrollmentDAO.getEnrolledStudentsBySkill(skillId);
                         for (Student s : students) {
                             if (!progressDAO.isProgressExists(s.getId(), newMatId)) {
                                 progressDAO.insertProgress(s.getId(), newMatId, "Not Started");
                             }
                         }
                     }
                } else if ("update".equals(action)) {
                     if (resUrl == null || resUrl.isEmpty()) {
                         resUrl = params.get("existing_file");
                     }
                     Material m = new Material();
                     m.setId(editingId);
                     m.setSkillId(skillId);
                     m.setTitle(title);
                     m.setWeight(weight);
                     m.setType(type);
                     m.setResourceUrl(resUrl != null ? resUrl : "");
                     
                     materialDAO.updateMaterial(m);
                }
            }
        }
        
        String redirectUrl = "/materials?skillId=" + skillId;
        if (errorMsg != null) {
            redirectUrl += "&error=" + java.net.URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
            if ("update".equals(action)) {
                redirectUrl += "&editId=" + params.get("id");
            }
        } else {
            // Add success message
            if ("add".equals(action)) redirectUrl += "&success=Module+added+successfully";
            if ("update".equals(action)) redirectUrl += "&success=Module+updated+successfully";
            if ("delete".equals(action)) redirectUrl += "&success=Module+deleted+successfully";
        }
        
        exchange.getResponseHeaders().set("Location", redirectUrl);
        exchange.sendResponseHeaders(302, -1);
    }
    
    private String getResourceUrl(String type, Map<String, String> params, String fileUrl) {
        if ("TEXT".equals(type)) return params.get("text_val");
        if ("LINK".equals(type)) return params.get("link_val");
        if ("FILE".equals(type) || "IMAGE".equals(type)) return fileUrl;
        return null;
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);
        
        String skillIdStr = params.get("skillId");
        if (skillIdStr == null) {
            exchange.getResponseHeaders().set("Location", "/subjects?mode=manage");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        int skillId = Integer.parseInt(skillIdStr);
        
        List<Material> materials = materialDAO.getMaterialsBySkillId(skillId);
        int totalWeight = materialDAO.getTotalWeightBySkillId(skillId);
        
        Material editMaterial = null;
        String editIdStr = params.get("editId");
        if (editIdStr != null) {
            editMaterial = materialDAO.getMaterialById(Integer.parseInt(editIdStr));
        }
        
        String error = params.get("error");
        
        // Read template
        String html = readTemplate("admin_materials.html");
        html = html.replace("{{skillId}}", String.valueOf(skillId));
        html = html.replace("{{totalWeight}}", String.valueOf(totalWeight));
        html = html.replace("{{weightClass}}", totalWeight == 100 ? "weight-ok" : "weight-warn");
        
        // Get skill name for breadcrumbs
        String skillName = skillDAO.getSkillById(skillId).getSkillName();
        html = html.replace("{{skillName}}", skillName != null ? skillName : "Course");
        
        // Form configuration
        if (editMaterial != null) {
            html = html.replace("{{formTitle}}", "Edit Module");
            html = html.replace("{{cancelButton}}", "<a href='materials?skillId=" + skillId + "' class='btn-secondary'>Cancel</a>");
            html = html.replace("{{action}}", "update");
            html = html.replace("{{editIdField}}", "<input type='hidden' name='id' value='" + editMaterial.getId() + "'>");
            html = html.replace("{{editTitle}}", editMaterial.getTitle().replace("'", "&#39;"));
            html = html.replace("{{editWeight}}", String.valueOf(editMaterial.getWeight()));
            html = html.replace("{{editType}}", editMaterial.getType() != null ? editMaterial.getType() : "TEXT");
            String resUrl = editMaterial.getResourceUrl() != null ? editMaterial.getResourceUrl() : "";
            html = html.replace("{{editTextVal}}", "TEXT".equals(editMaterial.getType()) ? resUrl : "");
            html = html.replace("{{editLinkVal}}", "LINK".equals(editMaterial.getType()) ? resUrl : "");
            if (("FILE".equals(editMaterial.getType()) || "IMAGE".equals(editMaterial.getType())) && !resUrl.isEmpty()) {
                html = html.replace("{{existingFileInfo}}", "<div style='font-size:0.8rem;color:#64748b;margin-bottom:5px'>Current: " + resUrl + "</div><input type='hidden' name='existing_file' value='" + resUrl + "'>");
            } else {
                html = html.replace("{{existingFileInfo}}", "");
            }
            html = html.replace("{{submitText}}", "Save Changes");
        } else {
            html = html.replace("{{formTitle}}", "Create New Module");
            html = html.replace("{{cancelButton}}", "");
            html = html.replace("{{action}}", "add");
            html = html.replace("{{editIdField}}", "");
            html = html.replace("{{editTitle}}", "");
            html = html.replace("{{editWeight}}", "");
            html = html.replace("{{editType}}", "TEXT");
            html = html.replace("{{editTextVal}}", "");
            html = html.replace("{{editLinkVal}}", "");
            html = html.replace("{{existingFileInfo}}", "");
            html = html.replace("{{submitText}}", "Create Module");
        }
        
        // Error box
        if (error != null && !error.isEmpty()) {
            html = html.replace("{{errorBox}}", "<div class='error-box'><i class='fas fa-exclamation-circle'></i> " + error + "</div>");
        } else {
            html = html.replace("{{errorBox}}", "");
        }
        
        // Build materials list
        StringBuilder content = new StringBuilder();
        if (materials == null || materials.isEmpty()) {
            content.append("<div class='card-panel' style='text-align:center; padding:40px; color:#64748b;'>No modules yet.</div>");
        } else {
            for (Material m : materials) {
                String icon = "fa-file";
                String typeClass = "text-type";
                String typeName = "Text";
                
                if ("TEXT".equals(m.getType())) { icon = "fa-align-left"; typeClass = "text-type"; typeName = "Text"; }
                if ("LINK".equals(m.getType())) { icon = "fa-link"; typeClass = "link-type"; typeName = "Link"; }
                if ("FILE".equals(m.getType())) { icon = "fa-file-pdf"; typeClass = "file-type"; typeName = "File"; }
                if ("IMAGE".equals(m.getType())) { icon = "fa-image"; typeClass = "image-type"; typeName = "Image"; }
                
                String highlight = (editMaterial != null && editMaterial.getId() == m.getId()) ? "border:2px solid #6366f1;" : "";
                
                content.append("<div class='card-panel material-card' style='").append(highlight).append("'>");
                content.append("<div class='material-header'>");
                content.append("<div style='display:flex;align-items:center;gap:16px'>");
                content.append("<div class='material-icon'><i class='fas ").append(icon).append("'></i></div>");
                content.append("<h3 style='margin:0;font-size:1.1rem;font-weight:600;color:#0f172a'>").append(m.getTitle()).append("</h3>");
                content.append("</div>");
                content.append("<div style='display:flex;gap:12px'>");
                content.append("<a href='materials?skillId=").append(skillId).append("&editId=").append(m.getId()).append("' class='btn-secondary action-btn'><i class='fas fa-pen'></i></a>");
                content.append("<form action='materials' method='POST' style='margin:0'><input type='hidden' name='action' value='delete'><input type='hidden' name='skillId' value='").append(skillId).append("'><input type='hidden' name='id' value='").append(m.getId()).append("'><button class='btn-secondary action-btn' style='color:#ef4444;border-color:#e2e8f0'><i class='fas fa-trash'></i></button></form>");
                content.append("</div>");
                content.append("</div>");
                content.append("<div style='display:flex;gap:8px;margin-top:16px'>");
                content.append("<div class='meta-container'>");
                content.append("<div class='meta-pill type ").append(typeClass).append("'>").append(typeName).append("</div>");
                content.append("<div class='meta-pill weight'>").append(m.getWeight()).append("%</div>");
                content.append("</div>");
                content.append("</div></div>");
            }
        }
        
        html = html.replace("{{materialsContent}}", content.toString());
        
        // Generate toast HTML if success param present
        String success = params.get("success");
        StringBuilder toastHtml = new StringBuilder();
        if (success != null && !success.isEmpty()) {
            toastHtml.append("<div class='toast-container'>");
            toastHtml.append("<div class='toast toast-success'>");
            toastHtml.append("<div class='toast-icon'><i class='fas fa-check'></i></div>");
            toastHtml.append("<div class='toast-content'>");
            toastHtml.append("<p class='toast-title'>Success</p>");
            toastHtml.append("<p class='toast-message'>").append(success.replace("+", " ")).append("</p>");
            toastHtml.append("</div></div></div>");
        }
        html = html.replace("{{toastHtml}}", toastHtml.toString());

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readTemplate(String filename) throws IOException {
        File file = new File("src/main/webapp/" + filename);
        try (FileInputStream fis = new FileInputStream(file)) {
            return new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;
        
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                try {
                    result.put(
                        URLDecoder.decode(entry[0], StandardCharsets.UTF_8.name()),
                        URLDecoder.decode(entry[1], StandardCharsets.UTF_8.name())
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error decoding param: " + param);
                }
            } else if (entry.length == 1) {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
