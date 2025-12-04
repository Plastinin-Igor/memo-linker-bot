package ru.plastinin.memo_linker_bot.configuration;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.plastinin.memo_linker_bot.bot.MemoLinkerBot;

@Configuration
public class MemoLinkerBotConfiguration {

    @Bean
    public TelegramBotsApi telegramBotsApi(MemoLinkerBot memoLinkerBot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(memoLinkerBot);
        return api;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }
}
