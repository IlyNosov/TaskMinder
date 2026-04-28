package ru.ilynosov.taskminder.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.ilynosov.taskminder.application.AdminReminderService;
import ru.ilynosov.taskminder.domain.model.ReminderStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class TaskMinderWebServer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminReminderService adminReminderService;
    private final HttpServer server;

    public TaskMinderWebServer(AdminReminderService adminReminderService, int port) {
        this.adminReminderService = adminReminderService;

        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start web server", e);
        }

        this.server.setExecutor(Executors.newCachedThreadPool());
        registerContexts();
    }

    public void start() {
        server.start();
    }

    private void registerContexts() {
        server.createContext("/api/reminders", this::handleReminders);
        server.createContext("/static/", this::serveStatic);
        server.createContext("/", exchange -> servePage(exchange, "web/index.html"));
    }

    private void handleReminders(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(method) && "/api/reminders".equals(path)) {
                List<ReminderResponse> reminders = adminReminderService.findAll().stream()
                        .map(ReminderResponse::from)
                        .toList();
                writeJson(exchange, 200, reminders);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/api/reminders".equals(path)) {
                ReminderPayload payload = objectMapper.readValue(exchange.getRequestBody(), ReminderPayload.class);
                var reminder = adminReminderService.create(payload.toCreateCommand());
                writeJson(exchange, 201, ReminderResponse.from(reminder));
                return;
            }

            if (path.startsWith("/api/reminders/")) {
                UUID reminderId = UUID.fromString(path.substring("/api/reminders/".length()));

                if ("PUT".equalsIgnoreCase(method)) {
                    ReminderPayload payload = objectMapper.readValue(exchange.getRequestBody(), ReminderPayload.class);
                    var reminder = adminReminderService.update(reminderId, payload.toUpdateCommand());
                    writeJson(exchange, 200, ReminderResponse.from(reminder));
                    return;
                }

                if ("DELETE".equalsIgnoreCase(method)) {
                    adminReminderService.delete(reminderId);
                    writeEmpty(exchange, 204);
                    return;
                }
            }

            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            writeJson(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private void servePage(HttpExchange exchange, String resourcePath) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        if (!"/".equals(path)) {
            writeJson(exchange, 404, Map.of("error", "Not found"));
            return;
        }

        serveResource(exchange, resourcePath, "text/html; charset=utf-8");
    }

    private void serveStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        String path = exchange.getRequestURI().getPath();

        if ("/static/styles.css".equals(path)) {
            serveResource(exchange, "web/styles.css", "text/css; charset=utf-8");
            return;
        }

        if ("/static/app.js".equals(path)) {
            serveResource(exchange, "web/app.js", "application/javascript; charset=utf-8");
            return;
        }

        writeJson(exchange, 404, Map.of("error", "Not found"));
    }

    private void serveResource(HttpExchange exchange, String resourcePath, String contentType) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                writeJson(exchange, 404, Map.of("error", "Resource not found"));
                return;
            }

            byte[] body = inputStream.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private void writeEmpty(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

    public static class ReminderPayload {
        public Long telegramId;
        public String timezone;
        public String text;
        public String scheduledAt;
        public String status;

        public AdminReminderService.CreateReminderCommand toCreateCommand() {
            if (telegramId == null) {
                throw new IllegalArgumentException("telegramId is required");
            }
            if (scheduledAt == null || scheduledAt.isBlank()) {
                throw new IllegalArgumentException("scheduledAt is required");
            }

            return new AdminReminderService.CreateReminderCommand(
                    telegramId,
                    timezone == null || timezone.isBlank() ? "UTC" : timezone,
                    text,
                    LocalDateTime.parse(scheduledAt)
            );
        }

        public AdminReminderService.UpdateReminderCommand toUpdateCommand() {
            if (scheduledAt == null || scheduledAt.isBlank()) {
                throw new IllegalArgumentException("scheduledAt is required");
            }
            if (status == null || status.isBlank()) {
                throw new IllegalArgumentException("status is required");
            }

            return new AdminReminderService.UpdateReminderCommand(
                    text,
                    LocalDateTime.parse(scheduledAt),
                    ReminderStatus.valueOf(status)
            );
        }
    }

    public record ReminderResponse(String id,
                                   long telegramId,
                                   String text,
                                   String scheduledAt,
                                   String status,
                                   String createdAt) {

        public static ReminderResponse from(AdminReminderService.ReminderView reminder) {
            return new ReminderResponse(
                    reminder.id().toString(),
                    reminder.telegramId(),
                    reminder.text(),
                    reminder.scheduledAt().toString(),
                    reminder.status(),
                    reminder.createdAt().toString()
            );
        }
    }
}
