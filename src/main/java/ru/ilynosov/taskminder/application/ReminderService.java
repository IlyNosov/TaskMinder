package ru.ilynosov.taskminder.application;

import ru.ilynosov.taskminder.application.port.out.*;
import ru.ilynosov.taskminder.domain.model.*;
import ru.ilynosov.taskminder.domain.value.*;
import ru.ilynosov.taskminder.infrastructure.util.DateFormatter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final SchedulerPort schedulerPort;
    private final LlmMatchingPort llmMatchingPort;

    public ReminderService(
            ReminderRepository reminderRepository,
            UserRepository userRepository,
            SchedulerPort schedulerPort,
            LlmMatchingPort llmMatchingPort
    ) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.schedulerPort = schedulerPort;
        this.llmMatchingPort = llmMatchingPort;
    }

    public String handle(long telegramId, ParseResult result) {

        switch (result.getIntent()) {

            case CREATE:
                return createReminder(telegramId, result);

            case LIST:
                return listReminders(telegramId);

            case DELETE:
                return deleteReminder(telegramId, result);

            default:
                return "Не понял запрос 🤔";
        }
    }

    private String delete(Reminder reminder) {

        schedulerPort.cancel(reminder);

        reminderRepository.updateStatus(
                reminder.getId(),
                ReminderStatus.CANCELLED.name()
        );

        return "Задача удалена ✅";
    }

    private String createReminder(long telegramId, ParseResult result) {

        if (result.needsClarification())
            return "Уточни время напоминания ⏰";

        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> userRepository.save(
                        new User(
                                UUID.randomUUID(),
                                telegramId,
                                "UTC",
                                LocalDateTime.now()
                        )
                ));

        LocalDateTime scheduledAt =
                LocalDateTime.of(result.getDate(), result.getTime());

        Reminder reminder = new Reminder(
                UUID.randomUUID(),
                user.getId(),
                result.getText(),
                scheduledAt,
                LocalDateTime.now()
        );

        reminderRepository.save(reminder);
        schedulerPort.schedule(reminder);

        return "Напоминание создано ✅";
    }

    private String listReminders(long telegramId) {

        var userOpt = userRepository.findByTelegramId(telegramId);

        if (userOpt.isEmpty())
            return "У тебя пока нет задач 🎉";

        List<Reminder> reminders =
                reminderRepository.findActiveByUser(userOpt.get().getId());

        if (reminders.isEmpty())
            return "У тебя нет активных напоминаний ✨";

        StringBuilder sb = new StringBuilder("📋 Твои напоминания:\n\n");

        int index = 1;

        for (Reminder r : reminders) {
            sb.append(DateFormatter.format(r.getScheduledAt()));
        }

        return sb.toString();
    }

    private String deleteReminder(long telegramId, ParseResult result) {

        var userOpt = userRepository.findByTelegramId(telegramId);

        if (userOpt.isEmpty())
            return "У тебя нет задач";

        List<Reminder> reminders =
                reminderRepository.findActiveByUser(userOpt.get().getId());

        if (reminders.isEmpty())
            return "Нет активных задач";

        // если указан индекс
        if (result.getDeleteIndex() != null) {

            int index = result.getDeleteIndex();

            if (index < 1 || index > reminders.size())
                return "Неверный номер задачи";

            Reminder reminder = reminders.get(index - 1);
            return delete(reminder);
        }

        // если индекс не указан - LLM fallback
        var matchedId = llmMatchingPort.matchReminder(
                result.getRawInput(),
                reminders
        );

        if (matchedId.isEmpty())
            return "Не смог понять какую задачу удалить 🤔";

        Reminder reminder = reminders.stream()
                .filter(r -> r.getId().equals(matchedId.get()))
                .findFirst()
                .orElse(null);

        if (reminder == null)
            return "Задача не найдена";

        return delete(reminder);
    }
}