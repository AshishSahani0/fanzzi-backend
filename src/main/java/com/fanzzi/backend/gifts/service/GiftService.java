package com.fanzzi.backend.gifts.service;

import com.fanzzi.backend.gifts.model.Gift;
import com.fanzzi.backend.gifts.repository.GiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GiftService {

    private final GiftRepository giftRepository;

    public List<Gift> getAvailableGifts() {
        return giftRepository.findByActiveTrue();
    }

    public Gift getGift(String giftId) {
        return giftRepository.findById(giftId)
                .orElseThrow(() -> new RuntimeException("Gift not found"));
    }
}