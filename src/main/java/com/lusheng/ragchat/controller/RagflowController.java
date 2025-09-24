package com.lusheng.ragchat.controller;

import com.lusheng.ragchat.dto.CompletionRequest;
import com.lusheng.ragchat.service.RagflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ragflow")
public class RagflowController {

    private final RagflowService ragflowService;

    @Autowired
    public RagflowController(RagflowService ragflowService) {
        this.ragflowService = ragflowService;
    }

    @PostMapping("/completions")
    public Mono<ResponseEntity<Flux<String>>> streamCompletions(
            @RequestBody CompletionRequest request) {


        return ragflowService.getCompletions(request).map(responseTuple -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_EVENT_STREAM);

            // 【修复】: 检查 Tuple 的第一个元素不为 null 且不为空
            if (responseTuple.getT1() != null && !responseTuple.getT1().isEmpty()) {
                headers.add("X-Session-ID", responseTuple.getT1());
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(responseTuple.getT2());
        });
    }
}