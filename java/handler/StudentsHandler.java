package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dao.StudentDAO;
import dao.EnrollmentDAO;
import dao.MaterialDAO;
import dao.StudentProgressDAO;
import model.Student;
import model.StudentProgress;
import model.Skill;
import model.User;
import model.Admin;
import util.SimpleSessionManager;

/**
 * Handles Admin Students list page.
 * Reads HTML template and injects student data.
 */
public class StudentsHandler implements HttpHandler {

    private StudentDAO studentDAO = new StudentDAO();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    private MaterialDAO materialDAO = new MaterialDAO();
    private StudentProgressDAO progressDAO = new StudentProgressDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Session Check
        User user = SimpleSessionManager.getSessionUser(exchange);
        if (user == null || !(user instanceof Admin)) {
            exchange.getResponseHeaders().set("Location", "/admin_login.html");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String filter = null;
        String action = null;
        String idStr = null;
        String successMsg = null;

        String searchQuery = "";
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    if ("filter".equals(kv[0]))
                        filter = kv[1];
                    if ("action".equals(kv[0]))
                        action = kv[1];
                    if ("id".equals(kv[0]))
                        idStr = kv[1];
                    if ("success".equals(kv[0]))
                        successMsg = kv[1];
                    if ("q".equals(kv[0])) {
                        try {
                            searchQuery = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        // Handle delete action
        if ("delete".equals(action) && idStr != null) {
            int studentId = Integer.parseInt(idStr);
            studentDAO.deleteStudent(studentId);
            exchange.getResponseHeaders().set("Location", "/students?success=Student+deleted+successfully");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        // View student courses
        if ("view_courses".equals(action) && idStr != null) {
            int studentId = Integer.parseInt(idStr);
            sendStudentCourses(exchange, studentId);
            return;
        }

        // List students
        List<Student> students;
        String pageTitle;
        String badge;

        if ("active".equals(filter)) {
            students = enrollmentDAO.getAllActiveStudents();
            pageTitle = "Active Students";
            badge = "ACTIVE LEARNERS";
        } else if ("inactive".equals(filter)) {
            List<Student> all = studentDAO.getAllStudents();
            List<Student> active = enrollmentDAO.getAllActiveStudents();
            // Get IDs of active students
            java.util.Set<Integer> activeIds = new java.util.HashSet<>();
            for (Student s : active)
                activeIds.add(s.getId());

            // Filter all to get inactive
            students = new java.util.ArrayList<>();
            if (all != null) {
                for (Student s : all) {
                    if (!activeIds.contains(s.getId())) {
                        students.add(s);
                    }
                }
            }
            pageTitle = "Inactive Students";
            badge = "INACTIVE LEARNERS";
        } else {
            students = studentDAO.getAllStudents();
            pageTitle = "All Registered Students";
            badge = "ADMIN DIRECTORY";
        }

        // Apply Search Filter
        if (students != null && !searchQuery.isEmpty()) {
            List<Student> filtered = new java.util.ArrayList<>();
            String qLower = searchQuery.toLowerCase();
            for (Student s : students) {
                if ((s.getName() != null && s.getName().toLowerCase().contains(qLower)) ||
                        (s.getUsername() != null && s.getUsername().toLowerCase().contains(qLower))) {
                    filtered.add(s);
                }
            }
            students = filtered;
            pageTitle = "Search Results";
            badge = "FOUND " + students.size() + " STUDENTS";
        }

        // Read template
        String html = readTemplate("admin_students.html");
        html = html.replace("{{pageTitle}}", pageTitle);
        html = html.replace("{{badge}}", badge);
        html = html.replace("{{searchQuery}}", searchQuery);

        String clearBtn = "";
        if (!searchQuery.isEmpty()) {
            clearBtn = "<a href='students' class='btn-small' style='background:#f1f5f9;color:#64748b;border:1px solid #cbd5e1;height:38px;padding:0 16px;box-sizing:border-box;display:inline-flex;align-items:center;text-decoration:none;'>Clear</a>";
        }
        html = html.replace("{{clearFilterBtn}}", clearBtn);

        // Build table content
        StringBuilder content = new StringBuilder();
        if (students == null || students.isEmpty()) {
            content.append("<div class='empty-state'>");
            if ("inactive".equals(filter)) {
                content.append("<i class='fas fa-user-check'></i>");
                content.append("<p>No inactive students found.</p>");
                content.append(
                        "<p class='sub-text' style='margin-top:8px;font-size:0.9rem;color:#94a3b8'>All registered students are enrolled in at least one course.</p>");
            } else {
                content.append("<i class='fas fa-users-slash'></i>");
                content.append("<p>No students registered yet.</p>");
            }
            content.append("</div>");
        } else {
            content.append("<div class='table-container'>");
            content.append("<table>");
            content.append("<thead><tr>");
            content.append(
                    "<th style='width:100px;cursor:pointer' onclick='sortTable(0)'>ID <i class='fas fa-sort' style='font-size:0.8em;color:#cbd5e1;margin-left:4px'></i></th>");
            content.append(
                    "<th style='cursor:pointer' onclick='sortTable(1)'>Student Name <i class='fas fa-sort' style='font-size:0.8em;color:#cbd5e1;margin-left:4px'></i></th>");
            content.append(
                    "<th style='cursor:pointer' onclick='sortTable(2)'>Email Address <i class='fas fa-sort' style='font-size:0.8em;color:#cbd5e1;margin-left:4px'></i></th>");
            content.append("<th style='text-align:right'>Actions</th>");
            content.append("</tr></thead>");
            content.append("<tbody>");

            for (Student s : students) {
                String initials = s.getName() != null && s.getName().length() > 0
                        ? s.getName().substring(0, 1).toUpperCase()
                        : "?";

                content.append("<tr>");
                content.append("<td><span style='color:#64748b;font-family:monospace'>#").append(s.getId())
                        .append("</span></td>");
                content.append("<td><div class='user-cell'><div class='user-avatar'>").append(initials)
                        .append("</div><strong>").append(s.getName()).append("</strong></div></td>");
                content.append("<td>").append(s.getUsername()).append("</td>");
                content.append("<td style='text-align:right'>");
                content.append("<div style='display:flex;gap:10px;justify-content:flex-end'>");
                content.append("<a href='students?action=view_courses&id=").append(s.getId())
                        .append("' class='btn-view'>View Courses <i class='fas fa-chevron-right'></i></a>");
                content.append("<a href='students?action=delete&id=").append(s.getId())
                        .append("' onclick=\"return confirm('Are you sure you want to delete ")
                        .append(s.getName().replace("'", "\\'"))
                        .append("? This will also delete their enrollments and progress.')\" style='background:#fef2f2;border:1px solid #fecaca;color:#dc2626;padding:8px 16px;border-radius:8px;text-decoration:none;font-weight:600;font-size:0.9rem;display:inline-flex;align-items:center;gap:8px'><i class='fas fa-trash'></i></a>");
                content.append("</div></td>");
                content.append("</tr>");
            }

            content.append("</tbody></table></div>");
        }

        html = html.replace("{{tableContent}}", content.toString());

        // Generate toast HTML if success param present
        StringBuilder toastHtml = new StringBuilder();
        if (successMsg != null && !successMsg.isEmpty()) {
            toastHtml.append("<div class='toast-container'>");
            toastHtml.append("<div class='toast toast-success'>");
            toastHtml.append("<div class='toast-icon'><i class='fas fa-check'></i></div>");
            toastHtml.append("<div class='toast-content'>");
            toastHtml.append("<p class='toast-title'>Success</p>");
            toastHtml.append("<p class='toast-message'>").append(successMsg.replace("+", " ")).append("</p>");
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

    private void sendStudentCourses(HttpExchange exchange, int studentId) throws IOException {
        Student student = studentDAO.getStudentById(studentId);
        List<Skill> courses = enrollmentDAO.getEnrolledSkills(studentId);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Student Courses | Admin</title>");
        html.append("<link rel='stylesheet' type='text/css' href='css/style.css?v=3'>");
        html.append(
                "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap' rel='stylesheet'>");
        html.append(
                "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'>");
        html.append("<style>");
        html.append(
                ":root { --primary-soft: #e0e7ff; --primary-dark: #3730a3; --text-main: #1e293b; --text-muted: #64748b; --bg-main: #f8fafc; }");
        html.append(
                "body { display: block !important; padding: 0 !important; background-color: var(--bg-main) !important; color: var(--text-main); font-family: 'Inter', sans-serif; }");
        html.append(
                ".navbar { background: white; border-bottom: 1px solid #e2e8f0; padding: 0 40px; height: 70px; display: flex; justify-content: space-between; align-items: center; position: sticky; top: 0; z-index: 100; box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); }");
        html.append(
                ".nav-brand { font-size: 1.25rem; font-weight: 700; color: #0f172a; display: flex; align-items: center; gap: 12px; }");
        html.append(".nav-user { display: flex; align-items: center; gap: 24px; }");

        // CSS from admin_subjects.html
        html.append(
                ".courses-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 30px; }");
        html.append(
                ".course-card { background: white; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; transition: all 0.3s ease; display: flex; flex-direction: column; max-width: 400px; box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1); }");
        html.append(
                ".course-card:hover { transform: translateY(-4px); box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); }");
        html.append(
                ".card-img { height: 160px; background-size: cover; background-position: center; position: relative; display: flex; align-items: center; justify-content: center; }");
        html.append(
                ".card-img-overlay { position: absolute; inset: 0; background: linear-gradient(to bottom, transparent 0%, rgba(0, 0, 0, 0.3) 100%); }");
        html.append(
                ".card-badge { position: absolute; top: 12px; right: 12px; background: white; padding: 4px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); }");
        html.append(".card-body { padding: 20px; flex-grow: 1; display: flex; flex-direction: column; }");
        html.append(".course-title { font-size: 1.125rem; font-weight: 700; color: #0f172a; margin: 0 0 8px 0; }");
        html.append(
                ".course-meta { font-size: 0.875rem; color: #64748b; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; }");
        html.append(".card-action { margin-top: auto; }");
        html.append(
                ".btn-view { background: #eff6ff; color: #2563eb; border: 1px solid #dbeafe; display: block; width: 100%; padding: 12px; text-align: center; border-radius: 8px; font-weight: 600; text-decoration: none; }");
        html.append(".btn-view:hover { background: #dbeafe; }");

        html.append("</style></head><body>");

        // Navbar
        html.append("<nav class='navbar'>");
        html.append("  <div class='nav-brand'>");
        html.append(
                "    <div style='width: 32px; height: 32px; background: #6366f1; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white;'><i class='fas fa-cube'></i></div>");
        html.append("    <span>ProgressTracker <span style='color: #94a3b8; font-weight: 400;'>Admin</span></span>");
        html.append("  </div>");
        html.append("  <div class='nav-user'>");
        html.append("    <div style='text-align: right;'>");
        html.append("      <div style='font-weight: 600; font-size: 0.9rem;'>Administrator</div>");
        html.append("      <div style='font-size: 0.8rem; color: #64748b;'>Super User</div>");
        html.append("    </div>");
        html.append(
                "    <div style='width: 40px; height: 40px; background: #e2e8f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #64748b;'><i class='fas fa-user'></i></div>");
        html.append(
                "    <a href='/login.html' style='color: #ef4444; text-decoration: none; font-size: 1.1rem; margin-left: 10px;' title='Logout'><i class='fas fa-power-off'></i></a>");
        html.append("  </div>");
        html.append("</nav>");

        html.append("<div class='main-container' style='max-width: 1000px; margin: 40px auto; padding: 0 40px;'>");
        html.append(
                "<nav style='display:flex;align-items:center;gap:8px;font-size:0.9rem;margin-bottom:24px;color:#64748b'>");
        html.append(
                "<a href='admin_dashboard' style='color:#64748b;text-decoration:none'><i class='fas fa-home'></i></a>");
        html.append("<span style='color:#94a3b8'>/</span>");
        html.append("<a href='students' style='color:#64748b;text-decoration:none'>Students</a>");
        html.append("<span style='color:#94a3b8'>/</span>");
        html.append("<span style='color:#0f172a;font-weight:500'>").append(student.getUsername())
                .append("'s Courses</span>");
        html.append("</nav>");

        html.append("<h1 style='font-size: 1.75rem; font-weight: 700; color: #0f172a; margin: 0 0 10px;'>Courses for "
                + student.getName() + "</h1>");
        html.append("<p style='color: #64748b; margin-bottom: 30px;'>Viewing all enrolled courses.</p>");

        if (courses == null || courses.isEmpty()) {
            html.append(
                    "<div style='padding: 40px; text-align: center; background: white; border-radius: 12px; border: 1px dashed #cbd5e1; color:#64748b;'>Student is not enrolled in any courses.</div>");
        } else {
            html.append("<div class='courses-grid'>");
            for (Skill s : courses) {
                String imgUrl = (s.getImageUrl() != null && !s.getImageUrl().isEmpty()) ? "uploads/" + s.getImageUrl()
                        : null;
                boolean isActive = "active".equals(s.getStatus());
                String badgeStyle = isActive ? "background:#dcfce7;color:#166534;"
                        : "background:#f1f5f9;color:#64748b;";
                String badgeText = isActive ? "Active" : "Inactive";
                int moduleCount = materialDAO.getMaterialsBySkillId(s.getSkillId()).size();

                // Calculate Progress
                List<StudentProgress> allProgress = progressDAO.getProgressByStudentId(studentId);
                int completedMaterials = 0;
                for (StudentProgress sp : allProgress) {
                    if (s.getSkillName().equals(sp.getSkillName()) && "Completed".equalsIgnoreCase(sp.getStatus())) {
                        completedMaterials++;
                    }
                }
                int progressPercent = (moduleCount > 0) ? (completedMaterials * 100 / moduleCount) : 0;
                // Green if >= 80% (Rewarding), Orange if >= 40%, Blue otherwise
                String progressColor = progressPercent >= 80 ? "#10b981"
                        : (progressPercent >= 40 ? "#f59e0b" : "#4f46e5");

                html.append("<div class='course-card'>");

                // Image
                if (imgUrl != null) {
                    html.append("<div class='card-img' style='background-image:url(\"").append(imgUrl).append("\")'>");
                    html.append("<div class='card-img-overlay'></div>");
                } else {
                    html.append(
                            "<div class='card-img' style='background:linear-gradient(135deg,#e0e7ff,#f3e8ff);display:flex;align-items:center;justify-content:center'><i class='fas fa-book-open' style='font-size:3rem;color:#a5b4fc'></i>");
                }
                html.append("<div class='card-badge' style='").append(badgeStyle).append("'>").append(badgeText)
                        .append("</div></div>");

                // Body
                html.append("<div class='card-body'>");
                html.append("<h3 class='course-title'>").append(s.getSkillName()).append("</h3>");
                html.append("<div class='course-meta'><i class='far fa-folder'></i> ").append(moduleCount)
                        .append(" Modules</div>");

                // Progress Bar
                html.append("<div style='margin-bottom:20px;'>");
                html.append(
                        "<div style='display:flex;justify-content:space-between;font-size:0.8rem;color:#64748b;margin-bottom:4px;'>");
                html.append("<span>Progress</span><span>").append(progressPercent).append("%</span></div>");
                html.append("<div class='progress-track'><div class='progress-fill' style='width:")
                        .append(progressPercent).append("%;background:").append(progressColor).append("'></div></div>");
                html.append("</div>");

                // Action
                html.append("<div class='card-action'>");
                html.append("<a href='course_students?skillId=").append(s.getSkillId()).append("&skillName=")
                        .append(s.getSkillName())
                        .append("' class='btn-view'>View Analytics <i class='fas fa-arrow-right'></i></a>");
                html.append("</div>");

                html.append("</div></div>");
            }
            html.append("</div>");
        }

        html.append("</div></body></html>");

        byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
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
}
