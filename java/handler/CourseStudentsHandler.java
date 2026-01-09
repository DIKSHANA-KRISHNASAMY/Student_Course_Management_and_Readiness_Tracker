package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;

import dao.EnrollmentDAO;
import dao.StudentProgressDAO;
import model.Student;
import model.StudentProgress;
import model.User;
import model.Admin;
import util.SimpleSessionManager;

public class CourseStudentsHandler implements HttpHandler {

    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private StudentProgressDAO progressDAO = new StudentProgressDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        User user = SimpleSessionManager.getSessionUser(exchange);
        if (user == null || !(user instanceof Admin)) {
            exchange.getResponseHeaders().set("Location", "/admin_login.html");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);

        String skillIdParam = params.get("skillId");
        if (skillIdParam == null) {
            exchange.getResponseHeaders().set("Location", "subjects");
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        
        int skillId = Integer.parseInt(skillIdParam);
        String skillName = params.get("skillName") != null ? params.get("skillName") : "Course";

        List<Student> students = enrollmentDAO.getEnrolledStudentsBySkill(skillId);
        int totalEnrolled = students.size();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'><head>");
        html.append("<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Analytics: " + skillName + " | Admin</title>");
        html.append("<link rel='stylesheet' type='text/css' href='css/style.css?v=3'>");
        html.append("<link href='https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap' rel='stylesheet'>");
        html.append("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'>");
        html.append("<style>");
        html.append(":root { --primary-soft: #e0e7ff; --primary-dark: #3730a3; --text-main: #1e293b; --text-muted: #64748b; --bg-main: #f8fafc; }");
        html.append("body { display: block !important; padding: 0 !important; background-color: var(--bg-main) !important; color: var(--text-main); font-family: 'Inter', sans-serif; }");
        
        // Navbar
        html.append(".navbar { background: white; border-bottom: 1px solid #e2e8f0; padding: 0 40px; height: 70px; display: flex; justify-content: space-between; align-items: center; position: sticky; top: 0; z-index: 100; box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); }");
        html.append(".nav-brand { font-size: 1.25rem; font-weight: 700; color: #0f172a; display: flex; align-items: center; gap: 12px; }");
        html.append(".nav-user { display: flex; align-items: center; gap: 24px; }");
        // Stat Card
        html.append(".stat-card { background: white; padding: 24px; border-radius: 12px; border: 1px solid #e2e8f0; display: flex; align-items: center; gap: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }");
        html.append(".stat-icon { width: 56px; height: 56px; border-radius: 12px; background: #e0e7ff; color: #4338ca; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; }");
        html.append(".stat-info h3 { margin: 0; font-size: 2rem; font-weight: 700; color: #0f172a; }");
        html.append(".stat-info p { margin: 0; font-size: 0.9rem; color: #64748b; font-weight: 500; }");
        
        // Table
        html.append(".table-container { background: white; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.05); margin-top: 30px; }");
        html.append("th { background: #f8fafc; padding: 16px 24px; text-align: left; font-weight: 600; color: #475569; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid #e2e8f0; }");
        html.append("td { padding: 16px 24px; border-bottom: 1px solid #f1f5f9; font-size: 0.95rem; color: #334155; }");
        html.append("tr:last-child td { border-bottom: none; }");
        html.append(".user-cell { display: flex; align-items: center; gap: 12px; }");
        html.append(".user-avatar { width: 32px; height: 32px; border-radius: 50%; background: #f1f5f9; color: #94a3b8; display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: 600; }");
        
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        // Navbar
        html.append("<nav class='navbar'>");
        html.append("  <div class='nav-brand' style='font-size: 1.25rem; font-weight: 700; color: #0f172a; display: flex; align-items: center; gap: 12px;'>");
        html.append("    <div style='width: 32px; height: 32px; background: #6366f1; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white;'><i class='fas fa-cube'></i></div>");
        html.append("    <span>ProgressTracker <span style='color: #94a3b8; font-weight: 400;'>Admin</span></span>");
        html.append("  </div>");
        html.append("  <div class='nav-user'>");
        html.append("    <div style='text-align: right;'>");
        html.append("      <div style='font-weight: 600; font-size: 0.9rem;'>Administrator</div>");
        html.append("      <div style='font-size: 0.8rem; color: #64748b;'>Super User</div>");
        html.append("    </div>");
        html.append("    <div style='width: 40px; height: 40px; background: #e2e8f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #64748b;'><i class='fas fa-user'></i></div>");
        html.append("    <a href='/login.html' style='color: #ef4444; text-decoration: none; font-size: 1.1rem; margin-left: 10px;' title='Logout'><i class='fas fa-power-off'></i></a>");
        html.append("  </div>");
        html.append("</nav>");
        
        html.append("<div class='main-container' style='max-width: 1400px; margin: 40px auto; padding: 0 40px;'>");
        
        // Breadcrumbs Navigation
        html.append("<nav style='display:flex;align-items:center;gap:8px;font-size:0.9rem;margin-bottom:24px;color:#64748b'>");
        html.append("<a href='admin_dashboard' style='color:#64748b;text-decoration:none'><i class='fas fa-home'></i></a>");
        html.append("<span style='color:#94a3b8'>/</span>");
        html.append("<a href='subjects?mode=view' style='color:#64748b;text-decoration:none'>Course Analytics</a>");
        html.append("<span style='color:#94a3b8'>/</span>");
        html.append("<span style='color:#0f172a;font-weight:500'>").append(skillName).append("</span>");
        html.append("</nav>");
        
        // Header
        html.append("<div style='display:flex; justify-content:space-between; align-items:flex-end; margin-bottom: 40px;'>");
        html.append("<div>");
        html.append("<div style='display:flex; align-items:center; gap:12px; margin-bottom:8px;'>");
        html.append("<span style='background:#e0e7ff; color:#4338ca; padding:4px 10px; border-radius:20px; font-size:0.75rem; font-weight:700;'>COURSE ANALYTICS</span>");
        html.append("</div>");
        html.append("<h1 style='font-size: 2rem; font-weight: 700; color: #0f172a; margin: 0;'>" + skillName + "</h1>");
        html.append("</div>");
        html.append("<div>");
        html.append("</div>");
        html.append("</div>");

        // Stats Grid
        html.append("<div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 24px; margin-bottom: 30px;'>");
        html.append("<div class='stat-card'>");
        html.append("<div class='stat-icon'><i class='fas fa-users'></i></div>");
        html.append("<div class='stat-info'><h3>" + totalEnrolled + "</h3><p>Total Enrolled Students</p></div>");
        html.append("</div>");
        // Placeholder for future stats
        html.append("<div class='stat-card' style='opacity:0.6; padding:0; border:2px dashed #e2e8f0; background:none; display:flex; align-items:center; justify-content:center; color:#94a3b8;'>");
        html.append("More analytics coming soon...");
        html.append("</div>");
        html.append("</div>");

        if (students.isEmpty()) {
            html.append("<div style='padding: 60px; text-align: center; background: white; border-radius: 12px; border: 1px dashed #cbd5e1;'>");
            html.append("<i class='fas fa-users-slash' style='font-size: 3rem; color: #cbd5e1; margin-bottom: 20px;'></i>");
            html.append("<p style='color: #64748b; font-size: 1.1rem;'>No students enrolled in this course yet.</p>");
            html.append("</div>");
        } else {
            html.append("<div class='table-container'>");
            html.append("<table style='width: 100%; border-collapse: collapse;'>");
            html.append("<thead>");
            html.append("<tr>");
            html.append("<th style='width:30%;'>Student Name</th>");
            html.append("<th style='width:30%;'>Email Address</th>");
            html.append("<th style='text-align:center;'>Progress</th>");
            html.append("<th style='text-align:right;'>Readiness Score</th>");
            html.append("</tr>");
            html.append("</thead>");
            html.append("<tbody>");

            for (Student s : students) {
                // Fetch ALL progress for student, then filter by this skill
                List<StudentProgress> allProgress = progressDAO.getProgressByStudentId(s.getId());
                
                double totalWeight = 0;
                double completedWeight = 0;
                int totalMaterials = 0;
                int completedMaterials = 0;
                
                for (StudentProgress sp : allProgress) {
                    if (skillName.equals(sp.getSkillName())) { 
                        totalMaterials++;
                        totalWeight += sp.getMaterialWeight();
                        if ("Completed".equalsIgnoreCase(sp.getStatus())) {
                            completedWeight += sp.getMaterialWeight();
                            completedMaterials++;
                        }
                    }
                }
                
                double readiness = (totalWeight > 0) ? (completedWeight / totalWeight) * 100 : 0;
                String initials = s.getName().length() > 0 ? s.getName().substring(0, 1).toUpperCase() : "?";

                html.append("<tr>");
                html.append("<td><div class='user-cell'><div class='user-avatar'>" + initials + "</div><strong>" + s.getName() + "</strong></div></td>");
                html.append("<td>" + s.getUsername() + "</td>");
                
                // Material Progress Bar
                int matPercent = (totalMaterials > 0) ? (completedMaterials * 100 / totalMaterials) : 0;
                // Green if >= 80% (Rewarding), Orange if >= 40%, Blue otherwise
                String matColor = matPercent >= 80 ? "#10b981" : (matPercent >= 40 ? "#3b82f6" : "#64748b");
                html.append("<td style='vertical-align:middle'>");
                html.append("<div style='display:flex;justify-content:space-between;font-size:0.75rem;margin-bottom:4px;color:#64748b'><span>" + completedMaterials + "/" + totalMaterials + "</span><span>" + matPercent + "%</span></div>");
                html.append("<div class='progress-track' style='margin-top:0;height:6px'><div class='progress-fill' style='width:" + matPercent + "%;background:" + matColor + "'></div></div>");
                html.append("</td>");
                
                // Readiness Score Bar
                String color = readiness >= 80 ? "#16a34a" : (readiness >= 40 ? "#ea580c" : "#4f46e5");
                html.append("<td style='vertical-align:middle; width:200px'>");
                html.append("<div style='display:flex;justify-content:space-between;font-size:0.85rem;margin-bottom:4px;font-weight:600;color:#0f172a'><span>Score</span><span>" + String.format("%.0f", readiness) + "%</span></div>");
                html.append("<div class='progress-track' style='margin-top:0'><div class='progress-fill' style='width:" + readiness + "%;background:" + color + "'></div></div>");
                html.append("</td>");
                html.append("</tr>");
            }
            html.append("</tbody>");
            html.append("</table>");
            html.append("</div>");
        }
        html.append("</div>"); // End container
        html.append("</body></html>");

        byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
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
                } catch (Exception e) {}
            }
        }
        return result;
    }
}
