package org.com.dungeontalk.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.com.dungeontalk.global.common.entity.BaseEntity;

@Entity
@Table(name = "member")
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseEntity {

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", length = 20, unique = true)
    private String name;

    @Column(name = "nick_name",  unique = true)
    private String nickName;
}
