package ru.ilynosov.taskminder.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.ilynosov.taskminder.application.port.out.ReminderRepository;
import ru.ilynosov.taskminder.application.port.out.SchedulerPort;
import ru.ilynosov.taskminder.application.port.out.UserRepository;
import ru.ilynosov.taskminder.domain.model.Reminder;
import ru.ilynosov.taskminder.domain.model.ReminderStatus;
import ru.ilynosov.taskminder.domain.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminReminderServiceTest {

    private InMemoryReminderRepository reminderRepository;
    private InMemoryUserRepository userRepository;
    private FakeScheduler scheduler;
    private AdminReminderService adminReminderService;

    @BeforeEach
    void setUp() {
        reminderRepository = new InMemoryReminderRepository();
        userRepository = new InMemoryUserRepository();
        scheduler = new FakeScheduler();
        adminReminderService = new AdminReminderService(reminderRepository, userRepository, scheduler);
    }

    @Test
    void createsReminderAndSchedulesIt() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(3);

        AdminReminderService.ReminderView result = adminReminderService.create(
                new AdminReminderService.CreateReminderCommand(
                        123456789L,
                        "UTC",
                        "Подготовить слайды",
                        scheduledAt
                )
        );

        assertEquals(1, reminderRepository.findAll().size());
        assertEquals("Подготовить слайды", result.text());
        assertEquals(123456789L, result.telegramId());
        assertTrue(scheduler.scheduledIds.contains(result.id()));
    }

    @Test
    void updatesReminderAndReschedulesActiveTask() {
        User user = new User(UUID.randomUUID(), 42L, "UTC", LocalDateTime.now().minusDays(1));
        userRepository.save(user);

        Reminder reminder = new Reminder(
                UUID.randomUUID(),
                user.getId(),
                "Старый текст",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().minusHours(1)
        );
        reminderRepository.save(reminder);

        LocalDateTime newScheduledAt = LocalDateTime.now().plusDays(1);

        AdminReminderService.ReminderView updated = adminReminderService.update(
                reminder.getId(),
                new AdminReminderService.UpdateReminderCommand(
                        "Новый текст",
                        newScheduledAt,
                        ReminderStatus.ACTIVE
                )
        );

        Reminder stored = reminderRepository.findById(reminder.getId()).orElseThrow();

        assertEquals("Новый текст", stored.getText());
        assertEquals(newScheduledAt, stored.getScheduledAt());
        assertEquals(ReminderStatus.ACTIVE, stored.getStatus());
        assertEquals(42L, updated.telegramId());
        assertTrue(scheduler.cancelledIds.contains(reminder.getId()));
        assertTrue(scheduler.scheduledIds.contains(reminder.getId()));
    }

    @Test
    void deletesReminderAndCancelsActiveTask() {
        User user = new User(UUID.randomUUID(), 51L, "UTC", LocalDateTime.now().minusDays(1));
        userRepository.save(user);

        Reminder reminder = new Reminder(
                UUID.randomUUID(),
                user.getId(),
                "Удаляемая задача",
                LocalDateTime.now().plusHours(5),
                LocalDateTime.now()
        );
        reminderRepository.save(reminder);

        adminReminderService.delete(reminder.getId());

        assertTrue(reminderRepository.findById(reminder.getId()).isEmpty());
        assertTrue(scheduler.cancelledIds.contains(reminder.getId()));
    }

    @Test
    void findsAllSortedByScheduledTime() {
        User user = new User(UUID.randomUUID(), 77L, "UTC", LocalDateTime.now().minusDays(1));
        userRepository.save(user);

        Reminder later = new Reminder(
                UUID.randomUUID(),
                user.getId(),
                "Поздняя",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now()
        );
        Reminder earlier = new Reminder(
                UUID.randomUUID(),
                user.getId(),
                "Ранняя",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now()
        );

        reminderRepository.save(later);
        reminderRepository.save(earlier);

        List<AdminReminderService.ReminderView> reminders = adminReminderService.findAll();

        assertEquals(2, reminders.size());
        assertEquals("Ранняя", reminders.get(0).text());
        assertEquals("Поздняя", reminders.get(1).text());
    }

    @Test
    void throwsWhenUpdatingUnknownReminder() {
        assertThrows(IllegalArgumentException.class, () -> adminReminderService.update(
                UUID.randomUUID(),
                new AdminReminderService.UpdateReminderCommand(
                        "Текст",
                        LocalDateTime.now().plusHours(1),
                        ReminderStatus.ACTIVE
                )
        ));
    }

    private static class InMemoryReminderRepository implements ReminderRepository {

        private final Map<UUID, Reminder> storage = new HashMap<>();

        @Override
        public void save(Reminder reminder) {
            storage.put(reminder.getId(), reminder);
        }

        @Override
        public Optional<Reminder> findById(UUID id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<Reminder> findAll() {
            return storage.values().stream()
                    .sorted(Comparator.comparing(Reminder::getScheduledAt))
                    .toList();
        }

        @Override
        public List<Reminder> findActiveByUser(UUID userId) {
            return storage.values().stream()
                    .filter(reminder -> reminder.getUserId().equals(userId))
                    .filter(Reminder::isActive)
                    .toList();
        }

        @Override
        public List<Reminder> findAllActive() {
            return storage.values().stream()
                    .filter(Reminder::isActive)
                    .toList();
        }

        @Override
        public void update(Reminder reminder) {
            storage.put(reminder.getId(), reminder);
        }

        @Override
        public void updateStatus(UUID reminderId, String status) {
            Reminder reminder = storage.get(reminderId);
            if (reminder != null) {
                reminder.setStatus(ReminderStatus.valueOf(status));
            }
        }

        @Override
        public void delete(UUID reminderId) {
            storage.remove(reminderId);
        }
    }

    private static class InMemoryUserRepository implements UserRepository {

        private final Map<UUID, User> byId = new HashMap<>();
        private final Map<Long, User> byTelegramId = new HashMap<>();

        @Override
        public Optional<User> findByTelegramId(long telegramId) {
            return Optional.ofNullable(byTelegramId.get(telegramId));
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public User save(User user) {
            byId.put(user.getId(), user);
            byTelegramId.put(user.getTelegramId(), user);
            return user;
        }
    }

    private static class FakeScheduler implements SchedulerPort {

        private final List<UUID> scheduledIds = new ArrayList<>();
        private final List<UUID> cancelledIds = new ArrayList<>();

        @Override
        public void schedule(Reminder reminder) {
            scheduledIds.add(reminder.getId());
        }

        @Override
        public void cancel(Reminder reminder) {
            cancelledIds.add(reminder.getId());
        }
    }
}
