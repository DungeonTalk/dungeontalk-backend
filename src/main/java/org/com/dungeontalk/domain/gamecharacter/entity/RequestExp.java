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
    private Integer level; // level 자체를 id로 줘서 Integer로 했습니다.

    @Column(name = "request_total_exp")
    private Long requestTotalExp; // totalExp는 많이 커질 수 있어서 long으로 줬습니다.

    @Column(name = "request_next_level_exp")
    private Integer requestNextLevelExp;

}
