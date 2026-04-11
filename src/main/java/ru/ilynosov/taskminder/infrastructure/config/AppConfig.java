package ru.ilynosov.taskminder.infrastructure.config;

import io.github.cdimascio.dotenv.Dotenv;

public class AppConfig {

    private static final Dotenv dotenv = Dotenv.load();

    public static String getBotToken() {
        return dotenv.get("BOT_TOKEN");
    }

    public static String getBotUsername() {
        return dotenv.get("BOT_USERNAME");
    }

    public static String getDbUrl() {
        return dotenv.get("DB_URL");
    }

    public static String getDbUser() {
        return dotenv.get("DB_USER");
    }

    public static String getDbPassword() {
        return dotenv.get("DB_PASSWORD");
    }

    public static String getOllamaUrl() {
        return dotenv.get("OLLAMA_URL", "http://localhost:11434");
    }

    public static String getOllamaModel() {
        return dotenv.get("OLLAMA_MODEL", "llama3.1:8b");
    }
}