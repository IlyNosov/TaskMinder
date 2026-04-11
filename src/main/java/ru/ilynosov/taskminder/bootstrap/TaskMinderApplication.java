package ru.ilynosov.taskminder.bootstrap;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.ilynosov.taskminder.application.ReminderService;
import ru.ilynosov.taskminder.application.port.in.NLUService;
import ru.ilynosov.taskminder.infrastructure.ai.*;
import ru.ilynosov.taskminder.infrastructure.config.AppConfig;
import ru.ilynosov.taskminder.infrastructure.config.DataSourceConfig;
import ru.ilynosov.taskminder.infrastructure.persistence.PostgresReminderRepository;
import ru.ilynosov.taskminder.infrastructure.persistence.PostgresUserRepository;
import ru.ilynosov.taskminder.infrastructure.scheduler.ReminderScheduler;
import ru.ilynosov.taskminder.infrastructure.telegram.TelegramBotAdapter;

import javax.sql.DataSource;

public class TaskMinderApplication {

    public static void main(String[] args) {
        try {
            System.out.println("Starting TaskMinder...");

            DataSource dataSource = initDataSource();
            var reminderRepository = new PostgresReminderRepository(dataSource);
            var userRepository = new PostgresUserRepository(dataSource);

            var timeParser = new TimeExpressionParser();
            var intentDetector = new IntentDetector();

            var ruleParser = new RuleBasedParser(timeParser, intentDetector);

            var scheduler = new ReminderScheduler(reminderRepository);
            scheduler.restoreActiveReminders();

            var ollamaClient = new OllamaClient();

            var llmRewriter = new LlmRewriteServiceImpl(ollamaClient);
            var llmMatcher = new LlmMatchingServiceImpl(ollamaClient);

            var nluService = new NLUServiceImpl(
                    ruleParser,
                    llmRewriter
            );

            var reminderService = new ReminderService(
                    reminderRepository,
                    userRepository,
                    scheduler,
                    llmMatcher
            );

            // Telegram
            startTelegramBot(reminderService, nluService);

            System.out.println("TaskMinder started successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Application failed to start.");
        }
    }

    private static DataSource initDataSource() {
        DataSource dataSource = DataSourceConfig.createDataSource();
        DataSourceConfig.migrate(dataSource);
        return dataSource;
    }

    private static NLUService buildNluService() {
        var timeParser = new TimeExpressionParser();
        var intentDetector = new IntentDetector();
        var ruleParser = new RuleBasedParser(timeParser, intentDetector);
        return new NLUServiceImpl(
                ruleParser,
                new LlmRewriteServiceImpl(new OllamaClient())
        );
    }

    private static void startTelegramBot(ReminderService reminderService, NLUService nluService) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        TelegramBotAdapter bot = new TelegramBotAdapter(
                AppConfig.getBotUsername(),
                AppConfig.getBotToken(),
                reminderService,
                nluService
        );

        botsApi.registerBot(bot);
    }
}