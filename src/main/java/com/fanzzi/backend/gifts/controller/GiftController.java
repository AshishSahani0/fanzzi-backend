package com.fanzzi.backend.gifts.controller;

import com.fanzzi.backend.gifts.model.Gift;
import com.fanzzi.backend.gifts.service.GiftService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gifts")
@RequiredArgsConstructor
public class GiftController {

    private final GiftService giftService;

    @GetMapping
    public List<Gift> getGifts() {
        return giftService.getAvailableGifts();
    }
}