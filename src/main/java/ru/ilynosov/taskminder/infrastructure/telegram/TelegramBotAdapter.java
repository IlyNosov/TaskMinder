package ru.ilynosov.taskminder.infrastructure.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.ilynosov.taskminder.application.ReminderService;
import ru.ilynosov.taskminder.application.port.in.NLUService;
import ru.ilynosov.taskminder.domain.value.Intent;
import ru.ilynosov.taskminder.domain.value.ParseResult;

import java.time.LocalDate;
import java.time.LocalTime;

public class TelegramBotAdapter extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final ReminderService reminderService;
    private final NLUService nluService;

    public TelegramBotAdapter(String botUsername,
                              String botToken,
                              ReminderService reminderService,
                              NLUService nluService) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.reminderService = reminderService;
        this.nluService = nluService;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            long telegramId = update.getMessage().getFrom().getId();
            String text = update.getMessage().getText();

            try {

                ParseResult result = nluService.parse(text);
                String response = reminderService.handle(telegramId, result);
                send(telegramId, response);

            } catch (Exception e) {
                send(telegramId, "Error: " + e.getMessage());
            }
        }
    }

    private void send(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}