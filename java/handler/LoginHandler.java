package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;

import dao.StudentDAO;
import dao.AdminDAO;
import model.User;
import util.SimpleSessionManager;

/**
 * Handles Login POST requests.
 * Demonstrates POLYMORPHISM - handles both Admin and Student logins.
 */
public class LoginHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            handlePost(exchange);
        } else {
            // Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String query = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseQuery(query);

        String email = params.get("email");
        String username = params.get("username"); 
        String password = params.get("password");
        
        // Determine is this is an Admin login attempt?
        // Admin login form usually sends 'username', student sends 'email'
        boolean isAdminAttempt = (username != null);

        User user = null;
        String error = null;
        String redirectOnFail = isAdminAttempt ? "/admin_login.html" : "/login.html";

        if (isAdminAttempt) {
            AdminDAO adminDAO = new AdminDAO();
            if (!adminDAO.checkAdminExists(username)) {
                error = "InvalidUsername";
            } else {
                model.Admin admin = adminDAO.validate(username, password);
                if (admin == null) {
                    error = "InvalidPassword"; 
                } else {
                    // Success
                    user = admin;
                }
            }
        } else {
            // Student Login
             StudentDAO studentDAO = new StudentDAO();
             user = studentDAO.getStudentByEmail(email);
             if (user == null) {
                 error = "invalid";
             } else if (!user.getPassword().equals(password)) {
                 user = null;
                 error = "invalid";
             }
        }

        if (user != null && error == null) {
            // Login Success
            String sessionId = SimpleSessionManager.createSession(user);
            
            // Set Cookie
            String cookieHeader = "sessionId=" + sessionId + "; Path=/; HttpOnly";
            exchange.getResponseHeaders().add("Set-Cookie", cookieHeader);
            
            // Redirect
            String redirectUrl = user.getDashboardPath();
            exchange.getResponseHeaders().set("Location", redirectUrl);
            exchange.sendResponseHeaders(302, -1);
        } else {
            // Login Failed
            String target = redirectOnFail + "?error=" + (error != null ? error : "invalid");
            exchange.getResponseHeaders().set("Location", target);
            exchange.sendResponseHeaders(302, -1);
        }
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
