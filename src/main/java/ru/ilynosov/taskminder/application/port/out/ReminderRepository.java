package ru.ilynosov.taskminder.application.port.out;

import ru.ilynosov.taskminder.domain.model.Reminder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository {

    void save(Reminder reminder);

    Optional<Reminder> findById(UUID id);

    List<Reminder> findAll();

    List<Reminder> findActiveByUser(UUID userId);

    List<Reminder> findAllActive();

    void update(Reminder reminder);

    void updateStatus(UUID reminderId, String status);

    void delete(UUID reminderId);
}
