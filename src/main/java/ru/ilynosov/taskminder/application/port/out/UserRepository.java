package ru.ilynosov.taskminder.application.port.out;

import ru.ilynosov.taskminder.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByTelegramId(long telegramId);

    Optional<User> findById(UUID id);

    User save(User user);
}
