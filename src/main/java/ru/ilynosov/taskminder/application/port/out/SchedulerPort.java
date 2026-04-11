package ru.ilynosov.taskminder.application.port.out;

import ru.ilynosov.taskminder.domain.model.Reminder;

public interface SchedulerPort {

    void schedule(Reminder reminder);

    void cancel(Reminder reminder);
}