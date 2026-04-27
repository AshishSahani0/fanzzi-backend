package com.fanzzi.backend.gifts.repository;

import com.fanzzi.backend.gifts.model.Gift;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GiftRepository extends MongoRepository<Gift, String> {

    List<Gift> findByActiveTrue();

}