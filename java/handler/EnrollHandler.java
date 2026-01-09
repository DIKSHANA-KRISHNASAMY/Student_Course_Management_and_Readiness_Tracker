package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;

import dao.StudentProgressDAO;
import dao.EnrollmentDAO;
import model.Student;
import model.User;
import util.SimpleSessionManager;

/**
 * Handles course enrollment.
 */
public class EnrollHandler implements HttpHandler {

    private StudentProgressDAO progressDAO = new StudentProgressDAO();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // Session Check
        User user = SimpleSessionManager.getSessionUser(exchange);
        if (user == null || !(user instanceof Student)) {
            exchange.getResponseHeaders().set("Location", "/login.html");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        Student student = (Student) user;
        
        // Parse form data
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseQuery(body);
        
        String skillIdStr = params.get("skillId");
        if (skillIdStr != null) {
            int skillId = Integer.parseInt(skillIdStr);
            
            // Add to ENROLLMENT table
            enrollmentDAO.enroll(student.getId(), skillId);
            
            // Add progress entries for all materials in this skill
            progressDAO.enrollStudentInSkill(student.getId(), skillId);
        }

        // Redirect to active courses
        exchange.getResponseHeaders().set("Location", "/dashboard?view=active");
        exchange.sendResponseHeaders(302, -1);
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
