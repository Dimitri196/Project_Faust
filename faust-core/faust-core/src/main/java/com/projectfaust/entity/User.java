package com.projectfaust.entity;

import com.projectfaust.entity.enums.ClearanceLevel;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String role;

    private boolean isAdmin; // Global bypass

    @Enumerated(EnumType.STRING)
    private ClearanceLevel clearance;

    private String status; // e.g., "OPERATIONAL"

    @ElementCollection(fetch = FetchType.EAGER) // EAGER zajistí, že se data načtou hned s uživatelem
    @CollectionTable(
            name = "user_tech_stack",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "technology") // Název sloupce s konkrétním stringem (např. "Java")
    private List<String> techStack;

    @Column(nullable = false)
    private String password; // Musí být v entitě pro login, ale NIKDY v ProfileResponse
}
