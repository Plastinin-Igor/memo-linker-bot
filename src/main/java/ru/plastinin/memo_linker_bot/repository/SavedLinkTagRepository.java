package ru.plastinin.memo_linker_bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.plastinin.memo_linker_bot.module.SavedLinkTag;
import ru.plastinin.memo_linker_bot.module.User;

import java.util.List;

public interface SavedLinkTagRepository extends JpaRepository<SavedLinkTag, Long> {

    @Query("""
             select slt
               from SavedLinkTag slt
              inner join SavedLink sl on (sl.linkId = slt.savedLinkLinkId)
              where sl.user = ?1
            """)
    List<SavedLinkTag> findAllTags(User user);
}
