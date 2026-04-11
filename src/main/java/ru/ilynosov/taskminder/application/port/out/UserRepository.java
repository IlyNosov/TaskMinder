package ru.ilynosov.taskminder.application.port.out;

import ru.ilynosov.taskminder.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByTelegramId(long telegramId);

    User save(User user);
}