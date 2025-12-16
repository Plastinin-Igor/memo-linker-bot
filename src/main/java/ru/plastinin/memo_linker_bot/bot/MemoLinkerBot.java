package ru.plastinin.memo_linker_bot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.plastinin.memo_linker_bot.constants.MessageConstants;
import ru.plastinin.memo_linker_bot.service.MemoLinkerBotService;


@Component
@Slf4j
public class MemoLinkerBot extends TelegramLongPollingBot {

    @Autowired
    MemoLinkerBotService botService;

    @Autowired
    private CharacterEncodingFilter characterEncodingFilter;

    @Value("${bot.username}")
    private String botUsername;

    private static final String START = "/start";
    private static final String HELP = "/help";
    private static final String SAVE = "/save";
    private static final String LIST = "/list";
    private static final String TAGS = "/tags";
    private static final String FIND = "/find";


    public MemoLinkerBot(@Value("${bot.token}") String botToken) {
        super(botToken);
    }

    /**
     * Основная логика бота
     *
     * @param update Update
     */
    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getChat().getUserName();
        Message msg = update.getMessage();

        // Обработка текстовых сообщений
        if (!update.hasMessage() || !msg.hasText()) {
            return;
        }
        String[] message = update.getMessage().getText().split(" ");
        switch (message[0]) {
            case START -> {
                startCommand(chatId, userName);
                log.info("START from username: {}, chatId: {}.", userName, chatId);
            }
            case SAVE -> {
                saveCommand(chatId, message);
                log.info("SAVE from username: {}, chatId: {}.", userName, chatId);
            }
            case LIST -> {
                listCommand(chatId);
                log.info("LIST from username: {}, chatId: {}.", userName, chatId);
            }
            case TAGS -> {
                tagsCommand(chatId);
                log.info("TAGS from username: {}, chatId: {}.", userName, chatId);
            }
            case FIND -> {
                findCommand(chatId, message);
                log.info("FIND from username: {}, chatId: {}.", userName, chatId);
            }
            case HELP -> {
                helpCommand(chatId);
                log.info("HELP from username: {}, chatId: {}.", userName, chatId);
            }
            default -> {
                sendMessage(chatId, "Команда не поддерживается");
                log.info("The command is not supported. Username: {}, chatId: {}.", userName, chatId);
            }
        }

    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }


    /**
     * Обработчик команды /Start
     *
     */
    private void startCommand(Long chatId, String userName) {
        String formatedText = botService.startCommandHandler(chatId, userName);
        sendMessage(chatId, formatedText);
    }

    /**
     * Обработчик команды /save
     *
     */
    private void saveCommand(Long chatId, String[] message) {
        String text = botService.saveCommandHandler(chatId, message);
        sendMessage(chatId, text);
    }

    /**
     * Обработчик команды /list
     * Список всех сохраненных ссылок
     *
     */
    private void listCommand(Long chatId) {
        String text = botService.listCommandHandler(chatId);
        sendMessage(chatId, text);
    }

    /**
     * Список всех тегов
     *
     */
    private void tagsCommand(Long chatId) {
        String text = botService.tagsListCommandHandler(chatId);
        sendMessage(chatId, text);
    }

    /**
     * Обработчик команды Find - поиск ссылок по ключевым словам
     *
     * @param chatId Long
     */
    private void findCommand(Long chatId, String[] message) {
        String text = botService.findCommandHandler(chatId, message);
        sendMessage(chatId, text);
    }

    /**
     * Обработчик команды /help
     *
     */
    private void helpCommand(Long chatId) {
        String text = MessageConstants.HELP_MESSAGE;
        sendMessage(chatId, text);
    }

    /**
     * Отправка сообщения в чат
     *
     * @param chatId Long
     * @param text   String
     */
    private void sendMessage(Long chatId, String text) {
        var chatIdStr = String.valueOf(chatId);
        var sendMessage = new SendMessage(chatIdStr, text);
        sendMessage.setParseMode("HTML");
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending message", e);
        }
    }

}
