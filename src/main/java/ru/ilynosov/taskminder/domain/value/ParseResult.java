package ru.ilynosov.taskminder.domain.value;

import java.time.LocalDate;
import java.time.LocalTime;

public class ParseResult {

    private final Intent intent;
    private final LocalDate date;
    private final LocalTime time;
    private final String text;
    private final double confidence;
    private final boolean usedLLM;
    private final boolean needsClarification;
    private final Integer deleteIndex;
    private final String rawInput;

    public ParseResult(Intent intent,
                       LocalDate date,
                       LocalTime time,
                       String text,
                       double confidence,
                       boolean usedLLM,
                       boolean needsClarification,
                       Integer deleteIndex,
                       String rawInput
    ) {

        this.intent = intent;
        this.date = date;
        this.time = time;
        this.text = text;
        this.confidence = confidence;
        this.usedLLM = usedLLM;
        this.needsClarification = needsClarification;
        this.deleteIndex = deleteIndex;
        this.rawInput = rawInput;
    }

    public Intent getIntent() {
        return intent;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getText() {
        return text;
    }

    public double getConfidence() {
        return confidence;
    }

    public boolean isUsedLLM() {
        return usedLLM;
    }

    public boolean needsClarification() {
        return needsClarification;
    }

    public String getRawInput() {
        return rawInput;
    }

    public Integer getDeleteIndex() {
        return deleteIndex;
    }
}