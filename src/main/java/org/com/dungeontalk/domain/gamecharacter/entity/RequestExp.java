package org.com.dungeontalk.domain.gamecharacter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "request_exp")
public class RequestExp {

    @Id
    @Column(name = "level")
    private String level;

    @Column(name = "request_total_exp")
    private Long requestTotalExp;

    @Column(name = "request_next_level_exp")
    private Integer requestNextLevelExp;

}
