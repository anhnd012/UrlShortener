package com.appsdeveloperblog.ws.urlshorten.url.controller;

import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/urls")
public class UrlController {
    private final UrlService urlService;

    @PostMapping
    public ResponseEntity createShortUrl(@RequestBody @Valid CreateUrlRequest request) {
        CreateUrlResponse response = urlService.createShortUrl(request);
        return new ResponseEntity(response, HttpStatus.CREATED);
    }
}
