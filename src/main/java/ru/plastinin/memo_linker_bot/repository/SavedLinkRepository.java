package ru.plastinin.memo_linker_bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.plastinin.memo_linker_bot.module.SavedLink;
import ru.plastinin.memo_linker_bot.module.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedLinkRepository extends JpaRepository<SavedLink, UUID> {

    Optional<SavedLink> findByOriginUrlAndUser(String originUrl, User user);

    @Query("""
            select s
              from SavedLink s
             where s.user = ?1
               and (lower(s.description) like '%?2%' or lower(s.title) like '%?2%')
            """)
    List<SavedLink> findSavedLink(User user, String findTerm);

}
