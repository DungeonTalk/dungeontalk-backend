package org.com.dungeontalk.domain.stat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.com.dungeontalk.global.common.entity.BaseEntity;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "race_stats")
public class RaceStats extends BaseEntity {
//    @Id
//    private String id;

    @Column(name = "race")
    private String race;

    @Column(name = "hp")
    private String hp;

    @Column(name = "mp")
    private String mp;

    @Column(name = "physical_attack")
    private String physicalAttack;

    @Column(name = "magic_attack")
    private String magicAttack;

    @Column(name = "evasion_rate")
    private String evasionRate;

    @Column(name = "accuracy")
    private String accuracy;

    @Column(name = "dice_odds")
    private String diceOdds;

//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
}
