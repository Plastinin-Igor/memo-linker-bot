package ru.plastinin.memo_linker_bot.configuration;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Словарь слов, которые не должны становится тегами
 * Список слов лежит в файле src/main/resources/static/StopWords.txt
 * <p>
 * Источник:
 * <p>
 * https://gist.github.com/menzenski/7047705
 * https://gist.github.com/sebleier/554280
 * <p>
 * (original source: http://code.google.com/p/stop-words/ ).
 *
 */

@Component
@NoArgsConstructor
@AllArgsConstructor
@Data
@Slf4j
public class StopWordsConfig {

    private Set<String> stopWords = new HashSet<>();

    @Value("classpath:static/StopWords.txt")
    private Resource staticResource;

    @PostConstruct
    public void init() throws IOException {
        List<String> words = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(staticResource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                stopWords.add(line.trim());
            }
            log.info("Файл со стоп-словами 'static/StopWords.txt' успешно прочитан. В словаре {} слов.", stopWords.size());
        } catch (IOException e) {
            // Если не получилось прочитать файл со стоп словами, запишем в лоб, но основные:
            log.error("Ошибка чтения файла 'static/StopWords.txt': {}", e.getMessage());
            stopWords.addAll(Set.of("и", "в", "во", "не", "что", "он", "на", "я", "с", "со",
                    "как", "а", "то", "все", "она", "так", "его", "но", "да", "ты", "к", "у",
                    "же", "вы", "за", "бы", "по", "только", "ее", "мне", "было", "вот", "от",
                    "меня", "еще", "нет", "о", "из", "ему", "теперь", "когда", "даже",
                    "ну", "вдруг", "ли", "если", "уже", "или", "ни", "быть", "был", "него",
                    "до", "вас", "нибудь", "опять", "уж", "вам", "ведь", "там", "потом",
                    "себя", "ничего", "ей", "может", "они", "тут", "где", "есть", "надо",
                    "ней", "для", "мы", "тебя", "их", "чем", "была", "сам", "чтоб", "без",
                    "будто", "чего", "раз", "тоже", "себе", "под", "будет", "ж", "тогда",
                    "кто", "этот", "того", "потому", "этого", "какой", "совсем", "ним",
                    "здесь", "этом", "один", "почти", "мой", "тем", "чтобы", "нее", "сейчас",
                    "были", "куда", "зачем", "всех", "никогда", "можно", "при", "наконец",
                    "два", "об", "другой", "хоть", "после", "над", "больше", "тот", "через",
                    "эти", "нас", "про", "всего", "них", "какая", "много", "разве", "три",
                    "эту", "моя", "впрочем", "хорошо", "свою", "этой", "перед", "иногда",
                    "лучше", "чуть", "том", "нельзя", "такой", "им", "более", "всегда",
                    "конечно", "всю", "между",
                    "the", "and", "a", "an", "in", "on", "at", "for", "to", "of", "with",
                    "by", "as", "is", "was", "are", "were", "be", "been", "being", "have",
                    "has", "had", "having", "do", "does", "did", "doing", "will", "would",
                    "shall", "should", "can", "could", "may", "might", "must", "this",
                    "that", "these", "those", "i", "you", "he", "she", "it", "we", "they",
                    "me", "him", "her", "us", "them", "my", "your", "his", "its", "our",
                    "their", "mine", "yours", "hers", "ours", "theirs"));
            log.info("Создали резервный словарь стоп-слов. В словаре {} слов.", stopWords.size());
        }
    }
}