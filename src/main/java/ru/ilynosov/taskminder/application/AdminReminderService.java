package ru.ilynosov.taskminder.application;

import ru.ilynosov.taskminder.application.port.out.ReminderRepository;
import ru.ilynosov.taskminder.application.port.out.SchedulerPort;
import ru.ilynosov.taskminder.application.port.out.UserRepository;
import ru.ilynosov.taskminder.domain.model.Reminder;
import ru.ilynosov.taskminder.domain.model.ReminderStatus;
import ru.ilynosov.taskminder.domain.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AdminReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final SchedulerPort schedulerPort;

    public AdminReminderService(ReminderRepository reminderRepository,
                                UserRepository userRepository,
                                SchedulerPort schedulerPort) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.schedulerPort = schedulerPort;
    }

    public List<ReminderView> findAll() {
        return reminderRepository.findAll().stream()
                .map(reminder -> new ReminderView(
                        reminder.getId(),
                        userRepository.findById(reminder.getUserId())
                                .map(User::getTelegramId)
                                .orElse(0L),
                        reminder.getText(),
                        reminder.getScheduledAt(),
                        reminder.getStatus().name(),
                        reminder.getCreatedAt()
                ))
                .sorted(Comparator.comparing(ReminderView::scheduledAt))
                .toList();
    }

    public ReminderView create(CreateReminderCommand command) {
        User user = userRepository.findByTelegramId(command.telegramId())
                .orElseGet(() -> userRepository.save(
                        new User(
                                UUID.randomUUID(),
                                command.telegramId(),
                                command.timezone(),
                                LocalDateTime.now()
                        )
                ));

        Reminder reminder = new Reminder(
                UUID.randomUUID(),
                user.getId(),
                command.text(),
                command.scheduledAt(),
                LocalDateTime.now()
        );

        reminderRepository.save(reminder);
        schedulerPort.schedule(reminder);

        return toView(reminder, user.getTelegramId());
    }

    public ReminderView update(UUID reminderId, UpdateReminderCommand command) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found"));

        long telegramId = userRepository.findById(reminder.getUserId())
                .map(User::getTelegramId)
                .orElse(0L);

        if (reminder.getStatus() == ReminderStatus.ACTIVE) {
            schedulerPort.cancel(reminder);
        }

        reminder.updateText(command.text());
        reminder.reschedule(command.scheduledAt());
        reminder.setStatus(command.status());

        reminderRepository.update(reminder);

        if (reminder.getStatus() == ReminderStatus.ACTIVE) {
            schedulerPort.schedule(reminder);
        }

        return toView(reminder, telegramId);
    }

    public void delete(UUID reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found"));

        if (reminder.getStatus() == ReminderStatus.ACTIVE) {
            schedulerPort.cancel(reminder);
        }

        reminderRepository.delete(reminderId);
    }

    private ReminderView toView(Reminder reminder, long telegramId) {
        return new ReminderView(
                reminder.getId(),
                telegramId,
                reminder.getText(),
                reminder.getScheduledAt(),
                reminder.getStatus().name(),
                reminder.getCreatedAt()
        );
    }

    public record CreateReminderCommand(long telegramId,
                                        String timezone,
                                        String text,
                                        LocalDateTime scheduledAt) {
    }

    public record UpdateReminderCommand(String text,
                                        LocalDateTime scheduledAt,
                                        ReminderStatus status) {
    }

    public record ReminderView(UUID id,
                               long telegramId,
                               String text,
                               LocalDateTime scheduledAt,
                               String status,
                               LocalDateTime createdAt) {
    }
}
