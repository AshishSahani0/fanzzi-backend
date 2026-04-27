//package com.fanzzi.backend.wallets.stars.monetization;
//
//import com.fanzzi.backend.security.SecurityUtil;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/content")
//@RequiredArgsConstructor
//public class ContentPurchaseController {
//
//    private final ContentPurchaseService purchaseService;
//
//    @PostMapping("/unlock/{postId}")
//    public void unlockPost(
//            @PathVariable String postId,
//            @RequestParam String ownerId,
//            @RequestParam String channelId,
//            @RequestParam long price
//    ) {
//
//        String buyerId = SecurityUtil.getCurrentUserId();
//
//        purchaseService.purchase(
//                buyerId,
//                ownerId,
//                channelId,
//                postId,
//                price
//        );
//    }
//}
