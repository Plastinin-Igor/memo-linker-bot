package ru.plastinin.memo_linker_bot.module;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "saved_links")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class SavedLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID linkId;

    @Column(name = "origin_url")
    private String originUrl;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ElementCollection
    private Set<String> tags = new HashSet<>();

}
