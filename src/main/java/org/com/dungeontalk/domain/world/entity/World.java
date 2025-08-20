package org.com.dungeontalk.domain.world.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "world")
public class World {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "world_id")
    private Long id;

    @Column(name = "world_name")
    private String worldName;

    @Column(name = "clear_exp")
    private String clearExp;

}