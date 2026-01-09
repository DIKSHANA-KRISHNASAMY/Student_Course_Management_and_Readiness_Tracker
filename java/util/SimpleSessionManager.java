package util;

import model.User;
import com.sun.net.httpserver.HttpExchange;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages user sessions since we are not using Servlet containers.
 * Demonstrates ENCAPSULATION of session state.
 */
public class SimpleSessionManager {
    // Stores Session ID -> User object
    private static final Map<String, User> sessions = new HashMap<>();

    public static String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        return sessionId;
    }

    public static User getUser(String sessionId) {
        return sessions.get(sessionId);
    }

    public static void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }

    // Helper to get user directly from HttpExchange
    public static User getSessionUser(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) return null;
        for (String pair : cookie.split(";")) {
            String[] entry = pair.trim().split("=");
            if (entry.length == 2 && "sessionId".equals(entry[0])) {
                return sessions.get(entry[1]);
            }
        }
        return null;
    }
}
