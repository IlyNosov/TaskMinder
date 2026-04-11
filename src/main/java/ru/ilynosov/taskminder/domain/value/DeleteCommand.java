package ru.ilynosov.taskminder.domain.value;

public class DeleteCommand {

    private final int index;

    public DeleteCommand(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}