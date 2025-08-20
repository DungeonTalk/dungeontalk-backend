package org.com.dungeontalk.domain.worldtype.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.worldtype.dto.request.WorldTypeCreateRequest;
import org.com.dungeontalk.domain.worldtype.dto.request.WorldTypeUpdateRequest;
import org.com.dungeontalk.domain.worldtype.dto.response.WorldTypeResponse;
import org.com.dungeontalk.domain.worldtype.service.WorldTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 세계관 관리 컨트롤러
 */
@RestController
@RequestMapping("/v1/world-types")
@RequiredArgsConstructor
public class WorldTypeController {

    private final WorldTypeService worldTypeService;

    /**
     * 모든 세계관 조회 (관리자용)
     */
    @GetMapping("/admin")
    public ResponseEntity<List<WorldTypeResponse>> getAllWorldTypes() {
        List<WorldTypeResponse> worldTypes = worldTypeService.getAllWorldTypes();
        return ResponseEntity.ok(worldTypes);
    }

    /**
     * 활성화된 세계관만 조회 (일반 사용자용)
     */
    @GetMapping
    public ResponseEntity<List<WorldTypeResponse>> getActiveWorldTypes() {
        List<WorldTypeResponse> worldTypes = worldTypeService.getActiveWorldTypes();
        return ResponseEntity.ok(worldTypes);
    }

    /**
     * 특정 세계관 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorldTypeResponse> getWorldType(@PathVariable Long id) {
        WorldTypeResponse worldType = worldTypeService.getWorldType(id);
        return ResponseEntity.ok(worldType);
    }

    /**
     * 코드로 세계관 조회
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<WorldTypeResponse> getWorldTypeByCode(@PathVariable String code) {
        WorldTypeResponse worldType = worldTypeService.getWorldTypeByCode(code);
        return ResponseEntity.ok(worldType);
    }

    /**
     * 새로운 세계관 생성 (관리자용)
     */
    @PostMapping("/admin")
    public ResponseEntity<WorldTypeResponse> createWorldType(@Valid @RequestBody WorldTypeCreateRequest request) {
        WorldTypeResponse createdWorldType = worldTypeService.createWorldType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWorldType);
    }

    /**
     * 세계관 수정 (관리자용)
     */
    @PutMapping("/admin/{id}")
    public ResponseEntity<WorldTypeResponse> updateWorldType(
            @PathVariable Long id, 
            @Valid @RequestBody WorldTypeUpdateRequest request) {
        WorldTypeResponse updatedWorldType = worldTypeService.updateWorldType(id, request);
        return ResponseEntity.ok(updatedWorldType);
    }

    /**
     * 세계관 활성화/비활성화 토글 (관리자용)
     */
    @PatchMapping("/admin/{id}/toggle")
    public ResponseEntity<WorldTypeResponse> toggleWorldTypeStatus(@PathVariable Long id) {
        WorldTypeResponse worldType = worldTypeService.toggleWorldTypeStatus(id);
        return ResponseEntity.ok(worldType);
    }

    /**
     * 세계관 삭제 (관리자용)
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteWorldType(@PathVariable Long id) {
        worldTypeService.deleteWorldType(id);
        return ResponseEntity.noContent().build();
    }
}