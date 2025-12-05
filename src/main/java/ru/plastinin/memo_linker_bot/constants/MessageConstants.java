package ru.plastinin.memo_linker_bot.constants;


public class MessageConstants {

    public final static String WELCOME_MESSAGE = """
            📎 📝 🗂 📂 📋 📁 📄 📑 🏷 💾 🚀 🌏
            
            <b>@%s, добро пожаловать в бот "Хранитель ссылок"!</b>
            
            Здесь вы сможете сохранять полезные ссылки
            
            Команды:
            Начало работы 🚀 /start
            Справка 🔍 /help
            
            Вы зарегистрированы в боте %s
            """;

    public final static String MESSAGE_ERROR_NO_URL_TO_SAVE = """
            📝 Для сохранения ссылки воспользуйтесь командой:
            
            /save https://example.com/article
            или
            /save https://example.com/article "Описание"
            """;

    public final static String MESSAGE_ERROR_DATA_DUPLICATION = """
             ❌ Не удалось сохранить.
            
             Эта ссылка уже хранится в базе данных.
             Вы добавляли ее %s
            """;


    public final static String MESSAGE_ERROR_SAVE = """
            ❌ Не удалось сохранить ссылку.
            ↩️ Воспользуйтесь командой:
            
            <i>/save https://example.com/article "Описание" #programming #java #github</i>
            
            """;

    public final static String MESSAGE_OK_SAVE = """
            ✅ <b>Сохранено</b>: "%s"
            
            📝 %s
            
            🏷️ %s
            
            """;

    public final static String HELP_MESSAGE = """
            <b>📚 Справка по командам:</b>
            
            🚀 /start - Начать работу с ботом
            💾 /save [ссылка] [описание] [#теги] - Сохранить ссылку
            📋 /list - Показать все сохраненные ссылки
            🏷️ /tags - Показать облако тегов
            🔍 /search [#тег] - Поиск по тегам
            ❓ /help - Эта справка
            
            <i>Примеры:</i>
            /save https://example.com "Полезная статья" #программирование #java
            /search #java
            """;

}
