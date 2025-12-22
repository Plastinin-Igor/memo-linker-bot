package ru.plastinin.memo_linker_bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.plastinin.memo_linker_bot.configuration.StopWordsConfig;
import ru.plastinin.memo_linker_bot.constants.MessageConstants;
import ru.plastinin.memo_linker_bot.exception.ServiceException;
import ru.plastinin.memo_linker_bot.module.SavedLink;
import ru.plastinin.memo_linker_bot.module.SavedLinkTag;
import ru.plastinin.memo_linker_bot.module.User;
import ru.plastinin.memo_linker_bot.repository.SavedLinkRepository;
import ru.plastinin.memo_linker_bot.repository.SavedLinkTagRepository;
import ru.plastinin.memo_linker_bot.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class MemoLinkerBotService {

    private final UserRepository userRepository;
    private final SavedLinkRepository savedLinkRepository;
    private final SavedLinkTagRepository savedLinkTagRepository;

    private final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");

    // Стоп-слова (русские и английские) используем для создания авто-тегов
    private final StopWordsConfig stopWordsConfig;

    private final MessageSource messageSource;

    final int MAX_MESSAGE_LENGTH = 4000; // Telegram limit
    final int MAX_LINKS_IN_MESSAGE = 10;

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
        String text = MessageConstants.WELCOME_MESSAGE;
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
    @Transactional
    public String saveCommandHandler(Long chatId, String[] message) {
        try {
            //Проверим, что кроме команды /save есть еще что-то
            if (message.length <= 1) {
                return MessageConstants.MESSAGE_ERROR_NO_URL_TO_SAVE;
            }

            // Найдем пользователя
            User user = getUser(chatId);

            // Проверим, не сохранялась ли данная ссылка ранее
            Optional<SavedLink> link = savedLinkRepository.findByOriginUrlAndUser(message[1], user);
            if (link.isPresent()) {
                String textErr = MessageConstants.MESSAGE_ERROR_DATA_DUPLICATION;
                return String.format(textErr, link.get().getCreatedAt().format(customFormatter));
            }

            SavedLink savedLink = parseUrl(message[1]);
            savedLink.setUser(user);
            savedLink.setCreatedAt(LocalDateTime.now());

            // Обработаем пользовательские теги (авто-теги по тексту собираются в методе parseUrl)
            Set<String> tags = savedLink.getTags();
            for (int i = 2; i < message.length; i++) {
                if (message[i].startsWith("#")) {
                    tags.add(message[i].replace("#", ""));
                }
            }
            // Соберем теги в строку для ответа
            StringBuilder tagsToString = new StringBuilder();
            for (String tag : tags) {
                tagsToString.append(" #")
                        .append(tag);
            }

            // Обработаем случаи, когда не удалось получить описание страницы
            if (savedLink.getTitle() == null || savedLink.getTitle().isEmpty() || savedLink.getTitle().isBlank()) {
                if (message.length >= 3 && !message[2].isEmpty()) {
                    StringBuilder title = new StringBuilder();
                    for (int i = 2; i < message.length; i++) {
                        if (message[i].startsWith("#")) {
                            continue; // Это теги, их надо обработать отдельно
                        }
                        title.append(message[i]);
                        title.append(" ");
                    }
                    savedLink.setTitle(title.toString().replace("\"", ""));
                    savedLinkRepository.save(savedLink);
                } else {
                    return MessageConstants.MESSAGE_ERROR_SAVE;
                }
            } else {
                savedLinkRepository.save(savedLink);
            }
            //Ссылку удалось сохранить. Осталось сообщить об этом
            String text = MessageConstants.MESSAGE_OK_SAVE;
            // Описание ссылки
            String description;
            if (savedLink.getDescription() == null || savedLink.getDescription().isEmpty()
                    || savedLink.getDescription().isBlank()) {
                description = savedLink.getTitle() + "...";
            } else if (savedLink.getDescription().length() >= 300) {
                description = savedLink.getDescription().substring(0, 300);
            } else {
                description = savedLink.getDescription();
            }
            return String.format(text, savedLink.getTitle(), description, tagsToString.toString());
        } catch (Exception e) {
            log.error("Ошибка обработки команды SAVE: {}", e.getMessage());
            return MessageConstants.MESSAGE_ERROR_SAVE;
        }
    }

    /**
     * Обработчик команды /list
     *
     * @return String
     */
    public String listCommandHandler(Long chatId) {
        // Найдем пользователя
        User user = getUser(chatId);

        // Отсортированный по дате добавления список ссылок пользователя
        List<SavedLink> collections = savedLinkRepository.findAllByUserOrderByCreatedAtLimit(user, 50);

        if (collections.isEmpty()) {
            return MessageConstants.EMPTY_BASE; // в базе еще нет данных
        }

        //Составим список ссылок в одно сообщение
        StringBuilder messageText = new StringBuilder("🔎 Вот список ваших ссылок:\n\n");
        for (SavedLink savedLink : collections) {
            messageText.append("🏷️ ")
                    .append("<a href=\"")
                    .append(savedLink.getOriginUrl())
                    .append("\">")
                    .append(savedLink.getTitle())
                    .append("</a>")
                    .append("\n");

            // ПРОВЕРКА ДЛИНЫ
            if (messageText.length() > MAX_MESSAGE_LENGTH - 200) {
                messageText.append("\n\n... (сообщение обрезано)");
                break;
            }

        }
        return messageText.toString();
    }

    /**
     * Облако тегов
     *
     * @param chatId Long
     * @return String
     */
    public String tagsListCommandHandler(Long chatId) {
        User user = getUser(chatId);
        // Запишем теги в карту вместе с их количеством
        Map<String, Integer> tagFrequency = new HashMap<>();
        for (SavedLinkTag savedLinkTag : savedLinkTagRepository.findAllTags(user)) {
            tagFrequency.put(savedLinkTag.getTag(), tagFrequency.getOrDefault(savedLinkTag.getTag(), 0) + 1);
        }

        if (tagFrequency.isEmpty()) {
            return MessageConstants.EMPTY_BASE; // в базе еще нет данных
        }

        // Сортируем теги по частоте использования (от большего к меньшему)
        List<Map.Entry<String, Integer>> sortedTags = new ArrayList<>(tagFrequency.entrySet());
        sortedTags.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Находим максимальное количество для нормализации
        int maxCount = sortedTags.stream()
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(1);

        StringBuilder messageText = new StringBuilder();
        messageText.append("<b>☁️ 🏷️ Облако тегов:</b>\n\n");

        for (Map.Entry<String, Integer> entry : sortedTags) {
            String tag = entry.getKey();
            int count = entry.getValue();

            // Определяем размер тега на основе частоты
            String formattedTag = formatTagByFrequency(tag, count, maxCount);

            messageText.append(formattedTag)
                    .append(" (")
                    .append(count)
                    .append(")")
                    .append("  ");

            // ПРОВЕРКА ДЛИНЫ
            if (messageText.length() > MAX_MESSAGE_LENGTH - 200) {
                messageText.append("\n\n... (сообщение обрезано)");
                break;
            }
        }

        messageText.append("\n\n<i>Всего тегов: ").append(sortedTags.size()).append("</i>");

        return messageText.toString();

    }

    /**
     * Поиск ссылок по ключевым словам
     *
     * @param chatId Long
     * @return результат поиска
     */
    @Transactional
    public String findCommandHandler(Long chatId, String[] message) {
        try {
            //Проверим, что кроме команды /find есть еще что-то
            if (message.length <= 1) {
                return MessageConstants.MESSAGE_ERROR_FIND_LINKS;
            }
            //Найдем пользователя
            User user = getUser(chatId);
            //Соберем ключевые слова в поисковую строку, а теги в List
            StringBuilder findText = new StringBuilder(); // поиск по заголовку и описанию
            List<String> findTeg = new ArrayList<>(); // поиск по тегам
            for (int i = 1; i < message.length; i++) {
                if (message[i].startsWith("#")) {
                    findTeg.add(message[i].replace("#", ""));
                } else {
                    findText.append("%").append(message[i].toLowerCase()).append("%");
                }
            }

            // Поиск по описанию
            Set<SavedLink> links = new LinkedHashSet<>(savedLinkRepository.findSavedLink(user, findText.toString()));
            // Поиск по тегам
            links.addAll(savedLinkRepository.findAllByUserAndTagsIn(user, findTeg));

            // Если данные не найдены, то сообщим об этом
            if (links.isEmpty()) {
                return MessageConstants.MESSAGE_NO_DATA_FOUND;
            }
            int qnt = links.size();
            int linkCount = 0;
            //Составим список ссылок в одно сообщение
            StringBuilder messageText = new StringBuilder("🔎 Вот ссылки, которые найдены по вашему запросу (" + qnt + "):\n\n");
            for (SavedLink savedLink : links) {
                if (linkCount++ >= MAX_LINKS_IN_MESSAGE) {
                    messageText.append("\n\n... и ещё ")
                            .append(links.size() - MAX_LINKS_IN_MESSAGE)
                            .append(" ссылок");
                    break;
                }
                // Добавим к сообщению теги
                StringBuilder tags = new StringBuilder();
                for (String tag : savedLink.getTags()) {
                    tags.append("#").append(tag).append(" ");
                }
                //текст сообщения
                messageText.append("🏷️ ")
                        .append("<a href=\"")
                        .append(savedLink.getOriginUrl())
                        .append("\">")
                        .append(savedLink.getTitle())
                        .append("</a>")
                        .append("\n")
                        .append(savedLink.getDescription())
                        .append("\n")
                        .append(tags)
                        .append("\n\n");

                // ПРОВЕРКА ДЛИНЫ
                if (messageText.length() > MAX_MESSAGE_LENGTH - 200) {
                    messageText.append("\n\n... (сообщение обрезано)");
                    break;
                }
            }
            return messageText.toString();
        } catch (Exception e) {
            log.error("Ошибка поиска ссылок: {}", e.getMessage());
            return MessageConstants.MESSAGE_ERROR_FIND_LINKS;
        }
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
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)                    // Таймаут подключения: 30 секунд
                    .maxBodySize(2 * 1024 * 1024)      // Макс. размер страницы: 2 МБ
                    .ignoreContentType(true)           // Игнорируем Content-Type
                    .ignoreHttpErrors(true);           // Не падать на HTTP ошибках


            Document doc = connection
                    .execute()   // Выполняем запрос
                    .parse();    // Парсим документ из Response

            // Найдем заголовок, описание и ссылку картинки на странице
            savedLink = SavedLink
                    .builder()
                    .originUrl(url)
                    .title(doc.title())
                    .description(doc.select("meta[name=description]").attr("content"))
                    .imageUrl(doc.select("meta[property=og:image]").attr("content"))
                    .build();

            // Возьмем топ-10 слов, которые встречаются на странице и сделаем из них хештеги для быстрого поиска
            String text = doc.text().toLowerCase();
            Set<String> tags = collectTags(text);
            // Если коллекция тегов не пустая, то сохраняем ее
            if (!tags.isEmpty()) {
                savedLink.setTags(tags);
            }
            return savedLink;
        } catch (Exception e) {
            log.error("Error parsing url: {}", e.getMessage());
            return SavedLink.builder().originUrl(url).build();
        }
    }

    /**
     * Метод находит топ 10 слов на странице
     * и добавляет их в хештеги для быстрого поиска
     *
     * @param text текст страницы
     * @return Set коллекция хештегов
     */
    private Set<String> collectTags(String text) {
        Set<String> tags = new HashSet<>();

        if (text == null || text.isEmpty()) {
            return tags;
        }

        // Если текст слишком большой - берём только начало
        // Большие статьи обычно имеют основной контент в начале
        final int MAX_TEXT_LENGTH = 100000; // 100K символов достаточно
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
            log.debug("Текст страницы сокращён до {} символов", MAX_TEXT_LENGTH);
        }

        // 2. ПОТОКОВАЯ ОБРАБОТКА СО SCANNER
        Map<String, Integer> wordFrequency = new HashMap<>();
        final Set<String> stopWords = stopWordsConfig.getStopWords();
        final int MAX_UNIQUE_WORDS = 500; // Не будем хранить все уникальные слова

        try (Scanner scanner = new Scanner(text)) {
            // Разделитель: всё, что не является буквой, апострофом или дефисом
            scanner.useDelimiter("[^\\p{L}\\p{M}'-]+");

            while (scanner.hasNext() && wordFrequency.size() < MAX_UNIQUE_WORDS) {
                String word = scanner.next().toLowerCase();

                // Проверка условий: длина и не стоп-слово
                if (word.length() >= 3 && !stopWords.contains(word)) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }

        // 3. ВЫБОР ТОП-10 СЛОВ
        // Используем PriorityQueue для эффективного поиска топ-N
        if (!wordFrequency.isEmpty()) {
            // Минимальная куча для хранения топ-10
            PriorityQueue<Map.Entry<String, Integer>> topWords =
                    new PriorityQueue<>(Map.Entry.comparingByValue());

            for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
                topWords.offer(entry);
                if (topWords.size() > 10) {
                    topWords.poll(); // Удаляем элемент с наименьшей частотой
                }
            }

            // Переносим результаты в Set
            while (!topWords.isEmpty()) {
                tags.add(topWords.poll().getKey());
            }
        }

        log.debug("Сгенерировано {} тегов из {} уникальных слов",
                tags.size(), wordFrequency.size());
        return tags;
    }


    /**
     * Поиск пользователя в системе
     *
     * @param chatId long
     * @return user
     */
    private User getUser(Long chatId) {
        return userRepository.getUserByChatId(chatId)
                .orElseThrow(() -> new ServiceException("Пользователь не найден в системе"));
    }

    /**
     * Форматирование тега
     *
     * @param tag      тег
     * @param count    количество в системе
     * @param maxCount максимальное количество
     * @return String формат тега: большой жирный шрифт, жирный шрифт, обычный шрифт, курсив
     */
    private String formatTagByFrequency(String tag, int count, int maxCount) {
        double percentage = (double) count / maxCount;

        if (percentage >= 0.7) {
            // Самые частые теги - большой жирный шрифт
            return "<b><u>#" + tag + "</u></b>";
        } else if (percentage >= 0.4) {
            // Средние по частоте - жирный шрифт
            return "<b>#" + tag + "</b>";
        } else if (percentage >= 0.2) {
            // Реже используемые - обычный шрифт
            return "#" + tag;
        } else {
            // Самые редкие - курсив
            return "<i>#" + tag + "</i>";
        }
    }

}
