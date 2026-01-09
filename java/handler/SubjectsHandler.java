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
import java.util.ArrayList;
import java.net.URLDecoder;

import dao.SkillDAO;
import dao.MaterialDAO;
import model.Skill;
import model.User;
import model.Admin;
import util.SimpleSessionManager;
import util.MultipartParser;

/**
 * Handles Admin Subjects/Courses management.
 */
public class SubjectsHandler implements HttpHandler {

    private SkillDAO skillDAO = new SkillDAO();
    private MaterialDAO materialDAO = new MaterialDAO();
    private static final String UPLOAD_DIR = "src/main/webapp/uploads";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Session Check
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
        String imageUrl = null;

        // Check if it's multipart form data
        if (contentType != null && contentType.contains("multipart/form-data")) {
            MultipartParser parser = new MultipartParser();
            parser.parse(exchange.getRequestBody(), contentType);
            params = parser.getFields();

            // Handle file upload
            if (parser.hasFile()) {
                String originalName = parser.getFileName();
                String fileName = System.currentTimeMillis() + "_" + originalName;

                // Ensure upload directory exists
                File uploadDir = new File(UPLOAD_DIR);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                // Save file
                File outFile = new File(uploadDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(parser.getFileData());
                }
                imageUrl = fileName;
            }
        } else {
            // Regular form data
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            params = parseQuery(body);
        }

        String action = params.get("action");

        if ("delete".equals(action)) {
            int skillId = Integer.parseInt(params.get("skillId"));
            skillDAO.deleteSkill(skillId);
        } else if ("toggleStatus".equals(action)) {
            int skillId = Integer.parseInt(params.get("skillId"));
            String newStatus = params.get("newStatus");
            skillDAO.updateStatus(skillId, newStatus);
        } else if ("add".equals(action)) {
            String skillName = params.get("skillName");
            if (skillName != null && !skillName.trim().isEmpty()) {
                skillDAO.addSkill(new Skill(0, skillName.trim(), imageUrl, "active"));
            }
        }

        String mode = params.get("mode");
        String successMsg = "";
        if ("add".equals(action))
            successMsg = "&success=Course+created+successfully";
        if ("delete".equals(action))
            successMsg = "&success=Course+deleted+successfully";
        if ("toggleStatus".equals(action))
            successMsg = "&success=Course+status+updated";

        exchange.getResponseHeaders().set("Location",
                "/subjects?mode=" + (mode != null ? mode : "manage") + successMsg);
        exchange.sendResponseHeaders(302, -1);
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);

        String mode = params.get("mode");
        String filter = params.get("filter");
        boolean isViewMode = "view".equals(mode);
        boolean isActiveFilter = "active".equals(filter);
        boolean isInactiveFilter = "inactive".equals(filter);

        String pageTitle;
        String pageDescription;

        if (isViewMode) {
            if (isActiveFilter) {
                pageTitle = "Active Courses";
                pageDescription = "Manage currently active courses and enrollments.";
            } else if (isInactiveFilter) {
                pageTitle = "Inactive Courses";
                pageDescription = "View archived or disabled courses.";
            } else if (params.containsKey("analytics")) {
                pageTitle = "Course Analytics";
                pageDescription = "Monitor student performance and readiness across all subjects.";
            } else {
                pageTitle = "Total Courses";
                pageDescription = "View and manage all courses in the system.";
            }
        } else {
            pageTitle = "Curriculum Manager";
            pageDescription = "Create new subjects and manage learning materials.";
        }

        // Read template
        String html = readTemplate("admin_subjects.html");
        html = html.replace("{{pageTitle}}", pageTitle);
        html = html.replace("{{pageDescription}}", pageDescription);

        // Inject Filter Selections
        html = html.replace("{{filterAllSelected}}", (filter == null || filter.isEmpty()) ? "selected" : "");
        html = html.replace("{{filterActiveSelected}}", isActiveFilter ? "selected" : "");
        html = html.replace("{{filterInactiveSelected}}", isInactiveFilter ? "selected" : "");

        // Inject current mode for the filter form
        html = html.replace("value=\"manage\"", "value=\"" + (mode != null ? mode : "manage") + "\"");

        // Add form section (only in manage mode)
        StringBuilder addForm = new StringBuilder();
        if (!isViewMode) {
            addForm.append("<div class='add-section'>");
            addForm.append(
                    "<h3 style='margin:0 0 20px 0; font-size:1.1rem; color:#334155;'><i class='fas fa-plus-circle' style='color:#6366f1; margin-right:8px;'></i> Add New Course</h3>");
            addForm.append("<form action='subjects' method='POST' enctype='multipart/form-data' class='form-grid'>");
            addForm.append("<input type='hidden' name='action' value='add'>");
            addForm.append("<input type='hidden' name='mode' value='manage'>");
            addForm.append(
                    "<div><label class='form-label'>Course Title</label><input type='text' name='skillName' class='form-input' placeholder='e.g. Advanced Data Structures' required></div>");
            addForm.append(
                    "<div><label class='form-label'>Cover Image</label><input type='file' name='file' class='form-input' style='padding:9px; background:#f8fafc;' accept='image/*'></div>");
            addForm.append(
                    "<button type='submit' class='btn-primary' style='padding:12px 24px;border-radius:8px;font-weight:600;border:none;cursor:pointer;font-size:0.95rem'>Create Course</button>");
            addForm.append("</form></div>");
        }
        html = html.replace("{{addFormSection}}", addForm.toString());

        // Build courses content
        List<Skill> skills = skillDAO.getAllSkills();
        List<Skill> activeSkills = new ArrayList<>();
        List<Skill> inactiveSkills = new ArrayList<>();

        if (skills != null) {
            for (Skill s : skills) {
                if ("active".equals(s.getStatus())) {
                    activeSkills.add(s);
                } else {
                    inactiveSkills.add(s);
                }
            }
        }

        StringBuilder content = new StringBuilder();

        // If specific filter is applied, we only show that list (logic handled below)
        // If NO filter (or analytics view), we show BOTH sections separately.

        boolean showActive = !isInactiveFilter; // Show active unless inactive filter on
        boolean showInactive = !isActiveFilter; // Show inactive unless active filter on
        boolean separateSections = !isActiveFilter && !isInactiveFilter; // If viewing ALL, separate them

        if (skills == null || skills.isEmpty()) {
            content.append(
                    "<div class='empty-state'><i class='fas fa-folder-open'></i><p>No courses created yet.</p></div>");
        } else {
            if (separateSections) {
                // Stacked View: Active then Inactive

                // Active Section
                content.append("<div class='active-section' style='margin-bottom:40px'>");
                content.append(
                        "<h2 style='font-size:1.25rem;color:#334155;margin:0 0 16px;display:flex;align-items:center;gap:10px'><span class='pulsing-dot pulse-green'></span>Active Courses</h2>");
                if (!activeSkills.isEmpty()) {
                    content.append("<div class='courses-grid'>");
                    for (Skill s : activeSkills) {
                        content.append(renderCourseCard(s, isViewMode, materialDAO));
                    }
                    content.append("</div>");
                } else {
                    content.append(
                            "<div style='padding:20px;background:#f8fafc;border-radius:8px;color:#64748b;'>No active courses.</div>");
                }
                content.append("</div>");

                // Inactive Section
                content.append("<div class='inactive-section'>");
                content.append(
                        "<h2 style='font-size:1.25rem;color:#334155;margin:0 0 16px;display:flex;align-items:center;gap:10px'><span class='pulsing-dot pulse-red'></span>Inactive Courses</h2>");
                if (!inactiveSkills.isEmpty()) {
                    content.append("<div class='courses-grid'>");
                    for (Skill s : inactiveSkills) {
                        content.append(renderCourseCard(s, isViewMode, materialDAO));
                    }
                    content.append("</div>");
                } else {
                    content.append(
                            "<div style='padding:20px;background:#f8fafc;border-radius:8px;color:#64748b;'>No inactive courses.</div>");
                }
                content.append("</div>");

            } else {
                // STANDARD GRID VIEW (Filtered)
                content.append("<div class='courses-grid'>");
                if (showActive) {
                    for (Skill s : activeSkills) {
                        content.append(renderCourseCard(s, isViewMode, materialDAO));
                    }
                }
                if (showInactive) {
                    for (Skill s : inactiveSkills) {
                        content.append(renderCourseCard(s, isViewMode, materialDAO));
                    }
                }
                content.append("</div>");
            }

            // Empty state handling for filters
            if (content.length() == 0) {
                String msg = "No courses found.";
                if (isActiveFilter)
                    msg = "No active courses found.";
                if (isInactiveFilter)
                    msg = "No inactive courses found.";
                content.append(
                        "<div class='empty-state' style='grid-column: 1/-1; text-align: center; padding: 40px; background: white; border-radius: 12px; border: 1px dashed #cbd5e1; color: #64748b;'><i class='fas fa-folder-open' style='font-size: 2rem; margin-bottom: 16px; display: block;'></i><p style='margin: 0; font-size: 1.1rem;'>"
                                + msg + "</p></div>");
            }
        }

        html = html.replace("{{coursesContent}}", content.toString());

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

    private String renderCourseCard(Skill s, boolean isViewMode, MaterialDAO materialDAO) {
        StringBuilder content = new StringBuilder();
        String img = (s.getImageUrl() != null && !s.getImageUrl().isEmpty()) ? "uploads/" + s.getImageUrl() : null;
        boolean isActive = "active".equals(s.getStatus());
        String badgeStyle = isActive ? "background:#dcfce7;color:#166534;" : "background:#f1f5f9;color:#64748b;";
        String badgeText = isActive ? "Active" : "Inactive";
        int moduleCount = materialDAO.getMaterialsBySkillId(s.getSkillId()).size();

        content.append("<div class='course-card'>");

        // Image
        if (img != null) {
            content.append("<div class='card-img' style='background-image:url(\"").append(img).append("\")'>");
            content.append("<div class='card-img-overlay'></div>");
        } else {
            content.append(
                    "<div class='card-img' style='background:linear-gradient(135deg,#e0e7ff,#f3e8ff);display:flex;align-items:center;justify-content:center'><i class='fas fa-book-open' style='font-size:3rem;color:#a5b4fc'></i>");
        }
        content.append("<div class='card-badge' style='").append(badgeStyle).append("'>").append(badgeText)
                .append("</div></div>");

        // Body
        content.append("<div class='card-body'>");
        content.append("<h3 class='course-title'>").append(s.getSkillName()).append("</h3>");
        content.append("<div class='course-meta'><i class='far fa-folder'></i> ").append(moduleCount)
                .append(" Modules</div>");

        // Actions
        content.append("<div class='card-action'>");
        if (isViewMode) {
            content.append("<a href='course_students?skillId=").append(s.getSkillId()).append("&skillName=")
                    .append(s.getSkillName())
                    .append("' class='btn-view'>View Analytics <i class='fas fa-arrow-right'></i></a>");
        } else {
            content.append("<div style='display:flex;gap:10px'>");
            content.append("<a href='materials?skillId=").append(s.getSkillId())
                    .append("' class='btn-manage btn-view' style='flex:1'>Manage Content</a>");

            // Toggle status
            String nextStatus = isActive ? "inactive" : "active";
            String toggleIcon = isActive ? "fa-eye-slash" : "fa-eye";
            content.append(
                    "<form action='subjects' method='POST' style='margin:0'><input type='hidden' name='action' value='toggleStatus'><input type='hidden' name='skillId' value='")
                    .append(s.getSkillId()).append("'><input type='hidden' name='newStatus' value='").append(nextStatus)
                    .append("'><input type='hidden' name='mode' value='manage'><button type='submit' class='btn-toggle'><i class='fas ")
                    .append(toggleIcon).append("'></i></button></form>");

            // Delete
            content.append(
                    "<form action='subjects' method='POST' style='margin:0' onsubmit=\"return confirm('Delete this course?')\"><input type='hidden' name='action' value='delete'><input type='hidden' name='skillId' value='")
                    .append(s.getSkillId())
                    .append("'><input type='hidden' name='mode' value='manage'><button type='submit' class='btn-delete'><i class='fas fa-trash'></i></button></form>");
            content.append("</div>");
        }
        content.append("</div></div></div>");
        return content.toString();
    }

    private String readTemplate(String filename) throws IOException {
        File file = new File("src/main/webapp/" + filename);
        try (FileInputStream fis = new FileInputStream(file)) {
            return new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty())
            return result;

        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                try {
                    result.put(
                            URLDecoder.decode(entry[0], StandardCharsets.UTF_8.name()),
                            URLDecoder.decode(entry[1], StandardCharsets.UTF_8.name()));
                } catch (Exception e) {
                }
            } else if (entry.length == 1) {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
