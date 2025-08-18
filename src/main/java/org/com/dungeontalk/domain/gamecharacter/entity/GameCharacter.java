package org.com.dungeontalk.domain.gamecharacter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.global.common.entity.BaseEntity;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "character")
public class GameCharacter extends BaseEntity {
//    @Id
//    private String id;

    @Column(name = "member_id")
    private String memberId;

    /**
     * 멤버와의 일대일 관계 (읽기 전용)
     * - member_id(FK)는 기존 필드 사용
     * - 실제 갱신은 member_id 컬럼으로만 하고, 본 연관은 읽기 전용
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Member member;

    @Column(name = "race_type_id")
    private String raceTypeId;

    @Column(name = "player_level")
    private Integer playerLevel;

    @Column(name = "total_exp")
    private Long totalExp;

    @Column(name = "unspent_points", nullable = false)
    private Integer unspentPoints = 0; // 추가로 스텟을 찍을 수 있는 남은 증가치

    @Column(name = "str")
    private Integer str;

    @Column(name = "wil")
    private Integer wil;

    @Column(name = "int")
    private Integer int_; // Java 예약어 int 피해서 int_로

    @Column(name = "wis")
    private Integer wis;

    @Column(name = "dex")
    private Integer dex;

    @Column(name = "luk")
    private Integer luk;

//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;

    /**
     * 종족 스탯(연산식) 읽기 전용 매핑
     * - race_type_id(FK)는 기존 필드 사용
     * - 실제 갱신은 race_type_id 컬럼으로만 하고, 본 연관은 읽기 전용
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_type_id", referencedColumnName = "id", insertable = false, updatable = false)
    private RaceStats raceStats;

    // 공식 계산시 변수 Map 변환
    public Map<String, Object> toVariableMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("str", this.str);
        map.put("wil", this.wil);
        map.put("int", this.int_);
        map.put("wis", this.wis);
        map.put("dex", this.dex);
        map.put("luk", this.luk);
        return map;
    }
}
