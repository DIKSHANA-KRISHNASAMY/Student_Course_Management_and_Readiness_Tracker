package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import dao.SkillDAO;
import dao.StudentDAO;
import model.Skill;
import model.Student;
import model.User;
import model.Admin;
import util.SimpleSessionManager;

/**
 * Handles Admin Dashboard requests.
 * Reads HTML template and injects dynamic data.
 */
public class AdminDashboardHandler implements HttpHandler {

    private StudentDAO studentDAO = new StudentDAO();
    private SkillDAO skillDAO = new SkillDAO();
    private dao.EnrollmentDAO enrollmentDAO = new dao.EnrollmentDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Session Check
        User user = SimpleSessionManager.getSessionUser(exchange);
        if (user == null || !(user instanceof Admin)) {
            exchange.getResponseHeaders().set("Location", "/admin_login.html");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        // Fetch Stats from Database
        int studentCount = 0;
        int totalCourseCount = 0;
        int activeCourseCount = 0;
        int activeStudentCount = 0;
        
        try {
            List<Student> students = studentDAO.getAllStudents();
            studentCount = students != null ? students.size() : 0;
            
            List<Skill> skills = skillDAO.getAllSkills();
            totalCourseCount = skills != null ? skills.size() : 0;
            
            if (skills != null) {
                for(Skill s : skills) {
                    if("active".equals(s.getStatus())) {
                        activeCourseCount++;
                    }
                }
            }
            
            activeStudentCount = enrollmentDAO.getTotalUniqueEnrolledStudents();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get today's date
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));

        // Read HTML Template
        File templateFile = new File("src/main/webapp/admin_dashboard.html");
        String html = "";
        try (FileInputStream fis = new FileInputStream(templateFile)) {
            html = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Inject Dynamic Data (replace placeholders)
        // Inject Dynamic Data (replace placeholders)
        html = html.replace("{{today}}", today);
        html = html.replace("{{studentCount}}", String.valueOf(studentCount));
        html = html.replace("{{activeStudentCount}}", String.valueOf(activeStudentCount));
        html = html.replace("{{inactiveStudentCount}}", String.valueOf(studentCount - activeStudentCount));
        html = html.replace("{{totalCourseCount}}", String.valueOf(totalCourseCount));
        html = html.replace("{{activeCourseCount}}", String.valueOf(activeCourseCount));
        html = html.replace("{{inactiveCourseCount}}", String.valueOf(totalCourseCount - activeCourseCount));

        // Send Response
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
