package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;

import dao.StudentDAO;
import model.Student;

public class RegisterHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("RegisterHandler: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            handlePost(exchange);
        } else {
            // Allow GET for debugging
            String response = "DEBUG: Received " + exchange.getRequestMethod() + " on RegisterHandler";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String query = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseQuery(query);

        String name = params.get("name");
        String email = params.get("email");
        String password = params.get("password");

        StudentDAO dao = new StudentDAO();
        
        // Basic check if already exists
        if (dao.getStudentByEmail(email) != null) {
            redirect(exchange, "/register.html?error=EmailAlreadyExists");
            return;
        }

        // Add Student
        Student newStudent = new Student(0, name, email, password);
        int id = dao.addStudent(newStudent);

        if (id > 0) {
            redirect(exchange, "/login.html?success=Registered");
        } else {
            redirect(exchange, "/register.html?error=RegistrationFailed");
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;
        
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(
                    URLDecoder.decode(entry[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(entry[1], StandardCharsets.UTF_8)
                );
            }
        }
        return result;
    }
}
