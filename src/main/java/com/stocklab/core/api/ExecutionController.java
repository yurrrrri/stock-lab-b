package com.stocklab.core.api;

import com.stocklab.core.api.dto.ExecutionResponse;
import com.stocklab.core.api.security.AuthenticatedUserId;
import com.stocklab.core.domain.matching.service.ExecutionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionQueryService executionQueryService;

    @GetMapping
    public ResponseEntity<List<ExecutionResponse>> getMyExecutions(@AuthenticatedUserId Long userId) {
        return ResponseEntity.ok(executionQueryService.getExecutionsByUser(userId));
    }
}
