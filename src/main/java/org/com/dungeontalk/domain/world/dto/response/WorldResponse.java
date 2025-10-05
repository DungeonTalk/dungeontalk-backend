package org.com.dungeontalk.domain.world.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.dungeontalk.domain.world.entity.World;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorldResponse {
    private Long id;
    private String worldName;
    private Integer clearExp;

    public static WorldResponse from(World world) {
        return new WorldResponse(world.getId(), world.getWorldName(), world.getClearExp());
    }
}