package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.URLDecoder;

import model.Student;
import model.User;
import model.StudentProgress;
import model.Skill;
import model.Material;
import util.SimpleSessionManager;

/**
 * Handles Student Dashboard requests.
 * Reads HTML templates and injects dynamic data.
 */
public class DashboardHandler implements HttpHandler {

    private dao.StudentProgressDAO progressDAO = new dao.StudentProgressDAO();
    private dao.SkillDAO skillDAO = new dao.SkillDAO();
    private dao.EnrollmentDAO enrollmentDAO = new dao.EnrollmentDAO();
    private dao.MaterialDAO materialDAO = new dao.MaterialDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // Session Check
        User user = SimpleSessionManager.getSessionUser(exchange);
        if (user == null || !(user instanceof Student)) {
            exchange.getResponseHeaders().set("Location", "/login.html");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange, (Student) user);
        } else {
            handleGet(exchange, (Student) user);
        }
    }

    private void handlePost(HttpExchange exchange, Student student) throws IOException {
        String query = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseQuery(query);

        int studentId = Integer.parseInt(params.get("studentId"));
        int materialId = Integer.parseInt(params.get("materialId"));
        String status = params.get("status");

        progressDAO.updateStatus(studentId, materialId, status);

        String view = params.get("view");
        String skillId = params.get("skillId");

        String redirectUrl = "/dashboard";
        if ("details".equals(view) && skillId != null) {
            redirectUrl += "?view=details&skillId=" + skillId;
        }

        exchange.getResponseHeaders().set("Location", redirectUrl);
        exchange.sendResponseHeaders(302, -1);
    }

    private void handleGet(HttpExchange exchange, Student student) throws IOException {
        String queryStr = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(queryStr);

        String view = params.get("view");
        int studentId = student.getId();
        String studentName = student.getName();

        String html = "";

        // Date
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));

        if (view == null || view.isEmpty()) {
            // Home view
            html = readTemplate("student_dashboard.html");
            html = html.replace("{{studentName}}", studentName);
            html = html.replace("{{today}}", today);

            // Stats
            int enrolledCount = enrollmentDAO.getEnrolledSkillIds(studentId).size();
            int totalCount = skillDAO.getAllSkills().size();

            html = html.replace("{{enrolledCoursesCount}}", String.valueOf(enrolledCount));
            html = html.replace("{{totalCoursesCount}}", String.valueOf(totalCount));
        } else if ("active".equals(view)) {
            html = renderActiveCourses(studentId, studentName);
        } else if ("available".equals(view)) {
            html = renderAvailableCourses(studentId, studentName);
        } else if ("details".equals(view)) {
            String skillIdStr = params.get("skillId");
            if (skillIdStr != null) {
                html = renderCourseDetails(studentId, studentName, Integer.parseInt(skillIdStr));
            } else {
                exchange.getResponseHeaders().set("Location", "/dashboard");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
        } else if ("preview".equals(view)) {
            String skillIdStr = params.get("skillId");
            if (skillIdStr != null) {
                html = renderCoursePreview(studentName, Integer.parseInt(skillIdStr));
            } else {
                exchange.getResponseHeaders().set("Location", "/dashboard");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
        }

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

    private String renderActiveCourses(int studentId, String studentName) throws IOException {
        String html = readTemplate("active_courses.html");
        html = html.replace("{{studentName}}", studentName);

        List<Integer> enrolledSkillIds = enrollmentDAO.getEnrolledSkillIds(studentId);
        List<Skill> allSkills = skillDAO.getAllSkills();
        List<Skill> activeSkills = new ArrayList<>();

        for (Skill s : allSkills) {
            if (enrolledSkillIds.contains(s.getSkillId())) {
                activeSkills.add(s);
            }
        }

        StringBuilder content = new StringBuilder();

        if (activeSkills.isEmpty()) {
            content.append("<div class='empty-state'>");
            content.append("<i class='fas fa-inbox'></i>");
            content.append("<p>You are not enrolled in any courses yet.</p>");
            content.append("<a href='dashboard?view=available'>Browse Catalog &rarr;</a>");
            content.append("</div>");
        } else {
            content.append("<div class='courses-grid'>");
            for (Skill s : activeSkills) {
                boolean isActive = "active".equals(s.getStatus());
                String onClick = isActive
                        ? "window.location.href='dashboard?view=details&skillId=" + s.getSkillId() + "'"
                        : "";
                String cursorStyle = isActive ? "cursor:pointer;" : "cursor:not-allowed; opacity:0.8;";

                content.append("<div class='course-card' onclick=\"").append(onClick).append("\" style='")
                        .append(cursorStyle).append("'>");

                String img = (s.getImageUrl() != null && !s.getImageUrl().isEmpty()) ? "uploads/" + s.getImageUrl()
                        : null;
                if (img != null) {
                    content.append("<div class='course-img' style='background-image:url(\"").append(img)
                            .append("\");'>");
                } else {
                    content.append(
                            "<div class='course-img' style='background: linear-gradient(135deg, #e0e7ff 0%, #fae8ff 100%);'>");
                }
                content.append("<div class='course-img-overlay'></div>");

                if (!isActive) {
                    content.append("<div class='course-badge' style='color:#64748b;'>INACTIVE</div>");
                } else {
                    content.append("<div class='course-badge' style='color:#15803d;'>ENROLLED</div>");
                }
                content.append("</div>");

                content.append("<div class='course-body'>");
                content.append("<h3 class='course-title'>").append(s.getSkillName()).append("</h3>");

                // Calculate Progress
                List<StudentProgress> allProgress = progressDAO.getProgressByStudentId(studentId);
                int completedMaterials = 0;
                int totalMaterials = materialDAO.getMaterialsBySkillId(s.getSkillId()).size();
                for (StudentProgress sp : allProgress) {
                    if (s.getSkillName().equals(sp.getSkillName()) && "Completed".equalsIgnoreCase(sp.getStatus())) {
                        completedMaterials++;
                    }
                }
                int progressPercent = (totalMaterials > 0) ? (completedMaterials * 100 / totalMaterials) : 0;
                // Green if >= 80% (Rewarding), Orange if >= 40%, Blue otherwise
                String progressColor = progressPercent >= 80 ? "#10b981"
                        : (progressPercent >= 40 ? "#f59e0b" : "#4f46e5");

                // Progress Bar
                content.append("<div style='margin-bottom:16px;'>");
                content.append(
                        "<div style='display:flex;justify-content:space-between;font-size:0.8rem;color:#64748b;margin-bottom:4px;'>");
                content.append("<span>Progress</span><span>").append(progressPercent).append("%</span></div>");
                content.append("<div class='progress-track'><div class='progress-fill' style='width:")
                        .append(progressPercent).append("%;background:").append(progressColor).append("'></div></div>");
                content.append("</div>");

                if (isActive) {
                    content.append("<p class='course-desc'>Click to access learning materials.</p>");
                    content.append(
                            "<div class='course-action'>Continue Learning <i class='fas fa-arrow-right'></i></div>");
                } else {
                    content.append(
                            "<p class='course-desc' style='color:#ef4444;'><i class='fas fa-lock'></i> Course Unavailable</p>");
                    content.append("<div class='course-action' style='color:#cbd5e1;'>Locked</div>");
                }
                content.append("</div></div>");
            }
            content.append("</div>");
        }

        html = html.replace("{{coursesContent}}", content.toString());
        return html;
    }

    private String renderAvailableCourses(int studentId, String studentName) throws IOException {
        String html = readTemplate("available_courses.html");
        html = html.replace("{{studentName}}", studentName);

        List<Integer> enrolledSkillIds = enrollmentDAO.getEnrolledSkillIds(studentId);
        List<Skill> allSkills = skillDAO.getAllSkills();
        List<Skill> availableSkills = new ArrayList<>();

        for (Skill s : allSkills) {
            if (!enrolledSkillIds.contains(s.getSkillId())) {
                availableSkills.add(s);
            }
        }

        StringBuilder content = new StringBuilder();

        if (availableSkills.isEmpty()) {
            content.append("<div class='empty-state'>");
            content.append("<i class='fas fa-check-circle'></i>");
            content.append("<p>You have enrolled in all available courses!</p>");
            content.append("<a href='dashboard?view=active'>Go to Active Courses &rarr;</a>");
            content.append("</div>");
        } else {
            content.append("<div class='courses-grid'>");
            for (Skill s : availableSkills) {
                boolean isActive = "active".equals(s.getStatus());

                content.append("<div class='course-card'>");

                String img = (s.getImageUrl() != null && !s.getImageUrl().isEmpty()) ? "uploads/" + s.getImageUrl()
                        : null;
                if (img != null) {
                    content.append("<div class='course-img' style='background-image:url(\"").append(img)
                            .append("\");'>");
                } else {
                    content.append(
                            "<div class='course-img' style='background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);'><i class='fas fa-globe' style='font-size:3rem; color:#10b981; opacity:0.6;'></i>");
                }
                content.append("<div class='course-img-overlay'></div>");

                if (!isActive) {
                    content.append("<div class='course-badge' style='color:#64748b;'>INACTIVE</div>");
                } else {
                    content.append("<div class='course-badge' style='color:#0f172a;'>NEW</div>");
                }
                content.append("</div>");

                content.append("<div class='course-body'>");
                content.append("<h3 class='course-title'>").append(s.getSkillName()).append("</h3>");
                content.append("<p class='course-desc'>Master this subject by enrolling today.</p>");

                content.append("<div class='btn-group' style='display:flex;gap:12px;'>");
                content.append("<a href='dashboard?view=preview&skillId=").append(s.getSkillId())
                        .append("' class='btn btn-outline' style='flex:1;'>Preview</a>");

                if (isActive) {
                    content.append("<form action='enroll' method='POST' style='flex:1; margin:0;'>");
                    content.append("<input type='hidden' name='skillId' value='").append(s.getSkillId()).append("'>");
                    content.append("<button type='submit' class='btn btn-primary' style='width:100%;'>Enroll</button>");
                    content.append("</form>");
                } else {
                    content.append("<button class='btn btn-disabled' style='flex:1;' disabled>Inactive</button>");
                }
                content.append("</div></div></div>");
            }
            content.append("</div>");
        }

        html = html.replace("{{coursesContent}}", content.toString());
        return html;
    }

    private String renderCoursePreview(String studentName, int skillId) throws IOException {
        String skillName = "Course Preview";
        for (Skill s : skillDAO.getAllSkills()) {
            if (s.getSkillId() == skillId) {
                skillName = s.getSkillName();
                break;
            }
        }

        List<Material> materials = materialDAO.getMaterialsBySkillId(skillId);

        // Read template
        String html = readTemplate("course_preview.html");
        html = html.replace("{{studentName}}", studentName);
        html = html.replace("{{skillName}}", skillName);

        // Build materials content exactly like original servlet
        StringBuilder content = new StringBuilder();

        if (materials.isEmpty()) {
            content.append("<div class='empty-state'>No materials available for preview.</div>");
        } else {
            content.append("<div class='materials-list'>");

            for (Material m : materials) {
                String typeIcon = "fa-file";
                String typeName = "Module";
                String iconColor = "#64748b";
                String iconBg = "#f1f5f9";

                if ("TEXT".equals(m.getType())) {
                    typeIcon = "fa-align-left";
                    typeName = "Reading";
                    iconColor = "#3b82f6";
                    iconBg = "#eff6ff";
                } else if ("LINK".equals(m.getType())) {
                    typeIcon = "fa-link";
                    typeName = "External Link";
                    iconColor = "#10b981";
                    iconBg = "#ecfdf5";
                } else if ("FILE".equals(m.getType())) {
                    typeIcon = "fa-file-pdf";
                    typeName = "Document";
                    iconColor = "#f43f5e";
                    iconBg = "#fff1f2";
                } else if ("IMAGE".equals(m.getType())) {
                    typeIcon = "fa-image";
                    typeName = "Diagram";
                    iconColor = "#8b5cf6";
                    iconBg = "#f5f3ff";
                }

                content.append("<div class='material-card'>");

                // Header
                content.append("<div class='material-header'>");
                content.append("<div class='material-info'>");
                content.append("<h3>").append(m.getTitle()).append("</h3>");
                content.append("<div class='material-type'><i class='fas ").append(typeIcon).append("'></i> ")
                        .append(typeName).append("</div>");
                content.append("</div>");
                content.append("<span class='preview-badge'>PREVIEW</span>");
                content.append("</div>");

                // Body
                content.append("<div class='material-body'>");
                content.append("<div class='material-icon' style='background:").append(iconBg).append(";color:")
                        .append(iconColor).append("'><i class='fas ").append(typeIcon).append("'></i></div>");
                content.append("<div class='material-details'>");
                content.append("<div class='title'>").append(m.getTitle()).append("</div>");
                content.append("<div class='hint'>Preview content available.</div>");
                content.append("</div>");

                // Action button based on type
                if ("TEXT".equals(m.getType())) {
                    String safeTitle = m.getTitle() != null ? m.getTitle().replace("'", "\\'") : "Text Content";
                    String base64Content = "";
                    if (m.getResourceUrl() != null) {
                        try {
                            base64Content = java.util.Base64.getEncoder()
                                    .encodeToString(m.getResourceUrl().getBytes("UTF-8"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    content.append("<button onclick=\"openTextModal('").append(safeTitle).append("', '")
                            .append(base64Content).append("')\" class='btn-action'>Read Content</button>");
                } else if ("LINK".equals(m.getType())) {
                    String href = m.getResourceUrl();
                    if (href != null && !href.startsWith("http"))
                        href = "http://" + href;
                    content.append("<a href='").append(href).append(
                            "' target='_blank' class='btn-action'>Visit Link <i class='fas fa-external-link-alt' style='font-size:0.8em;margin-left:4px'></i></a>");
                } else if ("FILE".equals(m.getType()) || "IMAGE".equals(m.getType())) {
                    String href = "uploads/"
                            + (m.getResourceUrl() != null ? m.getResourceUrl().replace(" ", "%20") : "");
                    String btnText = "IMAGE".equals(m.getType()) ? "View Image" : "Download File";
                    content.append("<a href='").append(href).append("' target='_blank' class='btn-action'>")
                            .append(btnText)
                            .append(" <i class='fas fa-download' style='font-size:0.8em;margin-left:4px'></i></a>");
                }

                content.append("</div>"); // End material-body
                content.append("</div>"); // End material-card
            }

            content.append("</div>"); // End materials-list

            // Enroll section
            content.append("<div class='enroll-section'>");
            content.append("<h3>Ready to start learning?</h3>");
            content.append("<form action='enroll' method='POST' style='display:inline-block'>");
            content.append("<input type='hidden' name='skillId' value='").append(skillId).append("'>");
            content.append("<button type='submit' class='btn-enroll'>Enroll in Course</button>");
            content.append("</form>");
            content.append("</div>");
        }

        html = html.replace("{{materialsContent}}", content.toString());
        return html;
    }

    private String renderCourseDetails(int studentId, String studentName, int skillId) throws IOException {
        String skillName = "Course Details";
        for (Skill s : skillDAO.getAllSkills()) {
            if (s.getSkillId() == skillId) {
                skillName = s.getSkillName();
                break;
            }
        }

        List<StudentProgress> allProgress = progressDAO.getProgressByStudentId(studentId);
        List<StudentProgress> courseMaterials = new ArrayList<>();
        for (StudentProgress sp : allProgress) {
            if (sp.getSkillName().equals(skillName)) {
                courseMaterials.add(sp);
            }
        }

        double totalWeight = 0;
        double completedWeight = 0;
        for (StudentProgress sp : courseMaterials) {
            totalWeight += sp.getMaterialWeight();
            if ("Completed".equalsIgnoreCase(sp.getStatus())) {
                completedWeight += sp.getMaterialWeight();
            }
        }
        double readiness = (totalWeight > 0) ? (completedWeight / totalWeight) * 100 : 0;
        String readinessColor = (readiness >= 100) ? "#10b981" : "#4f46e5";

        // Read template
        String html = readTemplate("course_details.html");
        html = html.replace("{{studentName}}", studentName);
        html = html.replace("{{skillName}}", skillName);
        html = html.replace("{{readiness}}", String.format("%.0f", readiness));
        html = html.replace("{{readinessColor}}", readinessColor);

        // Build materials content exactly like original servlet
        StringBuilder content = new StringBuilder();

        if (courseMaterials.isEmpty()) {
            content.append(
                    "<div class='empty-state'>No learning materials assigned to this course yet. Check back later!</div>");
        } else {
            content.append("<div class='materials-list' style='display: flex; flex-direction: column; gap: 20px;'>");

            for (StudentProgress sp : courseMaterials) {
                boolean completed = "Completed".equalsIgnoreCase(sp.getStatus());

                content.append("<div class='material-card' style='background: white; border: 1px solid ")
                        .append(completed ? "#86efac" : "#e2e8f0")
                        .append("; border-radius: 12px; overflow: hidden; transition: all 0.2s;'>");

                // Header Bar
                content.append("<div class='material-header' style='padding: 20px 30px; background: ")
                        .append(completed ? "#f0fdf4" : "#f8fafc").append("; border-bottom: 1px solid ")
                        .append(completed ? "#bbf7d0" : "#e2e8f0")
                        .append("; display: flex; justify-content: space-between; align-items: center;'>");
                content.append("<div>");
                content.append("<h3 style='margin: 0 0 5px 0; font-size: 1.1rem; color: #1e293b;'>")
                        .append(sp.getMaterialTitle()).append("</h3>");
                content.append(
                        "<div style='display: flex; align-items: center; gap: 15px; font-size: 0.85rem; color: #64748b;'>");
                content.append("<span><i class='fas fa-weight-hanging'></i> Weight: ").append(sp.getMaterialWeight())
                        .append("%</span>");

                String typeIcon = "fa-file";
                String typeName = "Module";
                if ("TEXT".equals(sp.getType())) {
                    typeIcon = "fa-align-left";
                    typeName = "Reading";
                } else if ("LINK".equals(sp.getType())) {
                    typeIcon = "fa-link";
                    typeName = "External Link";
                } else if ("FILE".equals(sp.getType())) {
                    typeIcon = "fa-file-pdf";
                    typeName = "Document";
                } else if ("IMAGE".equals(sp.getType())) {
                    typeIcon = "fa-image";
                    typeName = "Diagram";
                }

                content.append("<span><i class='fas ").append(typeIcon).append("'></i> ").append(typeName)
                        .append("</span>");
                content.append("</div>");
                content.append("</div>");

                // Action
                content.append("<form action='dashboard' method='POST' style='margin: 0;'>");
                content.append("<input type='hidden' name='studentId' value='").append(studentId).append("'>");
                content.append("<input type='hidden' name='materialId' value='").append(sp.getMaterialId())
                        .append("'>");
                content.append("<input type='hidden' name='view' value='details'>");
                content.append("<input type='hidden' name='skillId' value='").append(skillId).append("'>");
                String nextStatus = completed ? "Not Started" : "Completed";
                content.append("<input type='hidden' name='status' value='").append(nextStatus).append("'>");

                if (completed) {
                    content.append(
                            "<button type='submit' style='background: white; color: #15803d; border: 1px solid #15803d; padding: 8px 16px; border-radius: 6px; font-size: 0.85rem; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px;'><i class='fas fa-check-circle'></i> Completed</button>");
                } else {
                    content.append(
                            "<button type='submit' style='background: white; color: #64748b; border: 1px solid #cbd5e1; padding: 8px 16px; border-radius: 6px; font-size: 0.85rem; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.2s;' onmouseover=\"this.style.borderColor='#94a3b8';this.style.color='#475569'\" onmouseout=\"this.style.borderColor='#cbd5e1';this.style.color='#64748b'\"><i class='far fa-circle'></i> Mark Complete</button>");
                }
                content.append("</form>");
                content.append("</div>");

                // Content Body
                if (sp.getType() != null && !sp.getType().isEmpty() && !"MODULE".equals(sp.getType())) {
                    String icon = "fa-file";
                    String color = "#64748b";
                    String bgColor = "#f1f5f9";
                    if ("TEXT".equals(sp.getType())) {
                        icon = "fa-align-left";
                        color = "#3b82f6";
                        bgColor = "#eff6ff";
                    } else if ("LINK".equals(sp.getType())) {
                        icon = "fa-link";
                        color = "#10b981";
                        bgColor = "#ecfdf5";
                    } else if ("FILE".equals(sp.getType())) {
                        icon = "fa-file-pdf";
                        color = "#f43f5e";
                        bgColor = "#fff1f2";
                    } else if ("IMAGE".equals(sp.getType())) {
                        icon = "fa-image";
                        color = "#8b5cf6";
                        bgColor = "#f5f3ff";
                    }

                    content.append("<div style='padding: 20px 30px; display: flex; align-items: center; gap: 20px;'>");
                    content.append("<div style='width: 48px; height: 48px; background: ").append(bgColor).append(
                            "; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: ")
                            .append(color).append("; font-size: 1.2rem;'><i class='fas ").append(icon)
                            .append("'></i></div>");

                    content.append("<div style='flex-grow: 1;'>");
                    content.append("<div style='font-weight: 600; color: #334155; margin-bottom: 4px;'>")
                            .append(sp.getMaterialTitle()).append("</div>");
                    content.append(
                            "<div style='font-size: 0.85rem; color: #94a3b8;'>Access this resource to complete the module.</div>");
                    content.append("</div>");

                    // Resource Action
                    if ("TEXT".equals(sp.getType())) {
                        String safeTitle = sp.getMaterialTitle() != null ? sp.getMaterialTitle().replace("'", "\\'")
                                : "Text Content";
                        String base64Content = "";
                        if (sp.getResourceUrl() != null) {
                            try {
                                base64Content = java.util.Base64.getEncoder()
                                        .encodeToString(sp.getResourceUrl().getBytes("UTF-8"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        content.append("<button onclick=\"openTextModal('").append(safeTitle).append("', '")
                                .append(base64Content)
                                .append("')\" style='background: white; border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 16px; cursor: pointer; font-size: 0.9rem; color: #475569; font-weight: 500; transition: 0.2s;' onmouseover=\"this.style.backgroundColor='#f8fafc'\" onmouseout=\"this.style.backgroundColor='white'\">View Content</button>");
                    } else if ("LINK".equals(sp.getType())) {
                        String href = sp.getResourceUrl();
                        if (href != null && !href.startsWith("http"))
                            href = "http://" + href;
                        content.append("<a href='").append(href).append(
                                "' target='_blank' style='text-decoration: none; background: white; border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 16px; cursor: pointer; font-size: 0.9rem; color: #475569; font-weight: 500; display: inline-block; transition: 0.2s;' onmouseover=\"this.style.backgroundColor='#f8fafc'\" onmouseout=\"this.style.backgroundColor='white'\">Open Link <i class='fas fa-external-link-alt' style='font-size:0.8em; margin-left:4px;'></i></a>");
                    } else if ("FILE".equals(sp.getType()) || "IMAGE".equals(sp.getType())) {
                        String href = "uploads/"
                                + (sp.getResourceUrl() != null ? sp.getResourceUrl().replace(" ", "%20") : "");
                        String btnText = "IMAGE".equals(sp.getType()) ? "View Image" : "Download File";
                        content.append("<a href='").append(href).append(
                                "' target='_blank' style='text-decoration: none; background: white; border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 16px; cursor: pointer; font-size: 0.9rem; color: #475569; font-weight: 500; display: inline-block; transition: 0.2s;' onmouseover=\"this.style.backgroundColor='#f8fafc'\" onmouseout=\"this.style.backgroundColor='white'\">")
                                .append(btnText)
                                .append(" <i class='fas fa-download' style='font-size:0.8em; margin-left:4px;'></i></a>");
                    }

                    content.append("</div>");
                } else {
                    content.append(
                            "<div style='padding: 20px 30px; color: #94a3b8; font-style: italic; font-size: 0.9rem;'>No downloadable resources attached.</div>");
                }

                content.append("</div>"); // End Module
            }
            content.append("</div>");
        }

        html = html.replace("{{materialsContent}}", content.toString());
        return html;
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
            }
        }
        return result;
    }
}
