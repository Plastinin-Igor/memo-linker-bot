package ru.plastinin.memo_linker_bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.plastinin.memo_linker_bot.module.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> getUserByUserId(UUID userId);

    Optional<User> getUserByChatId(Long chatId);

}
