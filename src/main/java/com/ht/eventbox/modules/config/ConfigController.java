package com.ht.eventbox.modules.config;

import com.ht.eventbox.config.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/public/api/v1/configs")
@RequiredArgsConstructor
@CrossOrigin
public class ConfigController {
    private final ConfigService configService;

    @GetMapping("/client-config")
    public ResponseEntity<Response<ClientConfigResponse>> getClientConfig() {
        var res = configService.getClientConfig();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
