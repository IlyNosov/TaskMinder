package ru.ilynosov.taskminder.infrastructure.scheduler;

import ru.ilynosov.taskminder.application.port.out.ReminderRepository;
import ru.ilynosov.taskminder.application.port.out.SchedulerPort;
import ru.ilynosov.taskminder.domain.model.Reminder;
import ru.ilynosov.taskminder.domain.model.ReminderStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class ReminderScheduler implements SchedulerPort {

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(2);

    private final ReminderRepository reminderRepository;

    // Храним активные задачи, чтобы можно было отменять
    private final Map<UUID, ScheduledFuture<?>> scheduledTasks =
            new ConcurrentHashMap<>();

    public ReminderScheduler(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    // Восстановление при старте
    public void restoreActiveReminders() {
        reminderRepository.findAllActive()
                .forEach(this::schedule);
    }

    @Override
    public void schedule(Reminder reminder) {

        long delay = Duration.between(
                LocalDateTime.now(),
                reminder.getScheduledAt()
        ).toMillis();

        if (delay < 0) {
            delay = 0;
        }

        ScheduledFuture<?> future = executor.schedule(() -> {

            System.out.println("Reminder: " + reminder.getText());

            reminderRepository.updateStatus(
                    reminder.getId(),
                    ReminderStatus.DONE.name()
            );

            scheduledTasks.remove(reminder.getId());

        }, delay, TimeUnit.MILLISECONDS);

        scheduledTasks.put(reminder.getId(), future);
    }

    @Override
    public void cancel(Reminder reminder) {

        ScheduledFuture<?> future =
                scheduledTasks.get(reminder.getId());

        if (future != null) {
            future.cancel(false);
            scheduledTasks.remove(reminder.getId());

            reminderRepository.updateStatus(
                    reminder.getId(),
                    ReminderStatus.CANCELLED.name()
            );
        }
    }
}