package ru.plastinin.memo_linker_bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import ru.plastinin.memo_linker_bot.exception.ServiceException;
import ru.plastinin.memo_linker_bot.module.SavedLink;
import ru.plastinin.memo_linker_bot.module.User;
import ru.plastinin.memo_linker_bot.repository.SavedLinkRepository;
import ru.plastinin.memo_linker_bot.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {

    private final UserRepository userRepository;
    private final SavedLinkRepository savedLinkRepository;

    private final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");

    /**
     * Обработчик команды /start
     * Добавляет нового пользователя в базу
     * Если пользователь еже есть в системе, то возвращает дату и время регистрации
     *
     * @return string
     */
    public String startCommandHandler(Long chatId, String userName) {
        User user;
        //Ищем в базе пользователя, если нет, то добавляем нового
        user = userRepository.getUserByChatId(chatId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .chatId(chatId)
                            .username(userName)
                            .createdAd(LocalDateTime.now())
                            .build();
                    userRepository.save(newUser);
                    return newUser;
                });
        // Текст сообщения
        String text = """
                
                <b>@%s, добро пожаловать в бот "Хранитель ссылок"!</b>
                
                Здесь вы сможете сохранять полезные ссылки
                
                
                <b>Команды:</b>
                Начало работы 🚀 /start
                Справка 🔍 /help
                
                <i>Вы зарегистрированы в боте %s</i>
                """;
        String dateTimeReg = user.getCreatedAd().format(customFormatter);
        return String.format(text, userName, dateTimeReg);
    }

    /**
     * Обработчик команды /save
     *
     * @param chatId  chatId
     * @param message String[]
     * @return String
     */
    public String saveCommandHandler(Long chatId, String[] message) {
        User user = userRepository.getUserByChatId(chatId)
                .orElseThrow(() -> new ServiceException("Пользователь не найден в системе"));
        SavedLink savedLink = parseUrl(message[1]);
        savedLink.setUser(user);
        if (savedLink.getTitle() == null || savedLink.getTitle().isEmpty() || savedLink.getTitle().isBlank()) {
            if (message.length >= 3 && !message[2].isEmpty()) {
                savedLink.setTitle(message[2]);
                savedLinkRepository.save(savedLink);
            } else {
                return """
                        🛑 Не удалось сохранить ссылку.
                        
                        ↩️ Воспользуйтесь командой: /save https://example.com/article "Описание"
                        """;
            }
        } else {
            savedLinkRepository.save(savedLink);
        }
        String text = """
                ✅ Сохранено
                
                📝 %s
                """;
        return String.format(text, savedLink.getTitle());
    }

    /**
     * Парсинг страницы
     *
     * @param url ссылка
     * @return SavedLink
     */
    private SavedLink parseUrl(String url) {
        SavedLink savedLink;
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36") // прикинемся браузером...
                    .timeout(10000)
                    .get();
            savedLink = SavedLink
                    .builder()
                    .originUrl(url)
                    .title(doc.title())
                    .description(doc.select("meta[name=description]").attr("content"))
                    .imageUrl(doc.select("meta[property=og:image]").attr("content"))
                    .build();
            return savedLink;
        } catch (Exception e) {
            log.error("Error parsing url: {}", e.getMessage());
            return SavedLink.builder().originUrl(url).build();
        }
    }
}
