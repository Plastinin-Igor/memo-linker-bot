package ru.plastinin.memo_linker_bot.module;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "saved_link_tags")
@Setter
@Getter
public class SavedLinkTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "saved_link_link_id")
    private UUID savedLinkLinkId;

    @Column(name = "tags")
    private String tag;

}
