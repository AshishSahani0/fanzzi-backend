//package com.fanzzi.backend.media.status;
//
//import com.fanzzi.backend.channel.status.dto.CreateChannelStatusRequest;
//import com.fanzzi.backend.channel.status.dto.StatusMedia;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class ChannelStatusMediaService {
//
//    private final ChannelStatusStorageService storage;
//
//    // =====================================================
//    // 🎥 PROCESS + VALIDATE MEDIA
//    // =====================================================
//    public List<StatusMedia> processMedia(
//            String channelId,
//            List<CreateChannelStatusRequest.StatusMediaRequest> media
//    ) {
//
//        if (media == null || media.isEmpty()) return List.of();
//
//        List<StatusMedia> result = new ArrayList<>();
//
//        for (var m : media) {
//
//            validateKey(channelId, m.getMediaKey());
//
//            String key = m.getMediaKey();
//
//            /// 🔥 VIDEO NORMALIZATION (FUTURE)
//            if (isVideo(key)) {
//                key = normalizeVideo(key); // placeholder
//            }
//
//            result.add(
//                    StatusMedia.builder()
//                            .mediaType(m.getMediaType())
//                            .mediaKey(key)
//                            .duration(m.getDuration())
//                            .build()
//            );
//        }
//
//        return result;
//    }
//
//    // =====================================================
//    // 🌍 BUILD CDN URLS
//    // =====================================================
//    public Map<String, String> resolvePublicUrls(List<String> keys) {
//
//        if (keys == null || keys.isEmpty()) return Map.of();
//
//        return keys.stream()
//                .collect(Collectors.toMap(
//                        k -> k,
//                        storage::getChannelStatusUrl
//                ));
//    }
//
//    // =====================================================
//    // 🗑 DELETE MEDIA
//    // =====================================================
//    public void delete(String key) {
//        storage.deleteChannelStatusMedia(key);
//    }
//
//    // =====================================================
//    // HELPERS
//    // =====================================================
//    private void validateKey(String channelId, String key) {
//        if (key == null || !key.startsWith("status/channels/" + channelId)) {
//            throw new RuntimeException("Invalid media key");
//        }
//    }
//
//    private boolean isVideo(String key) {
//        return key.toLowerCase().endsWith(".mp4");
//    }
//
//    private String normalizeVideo(String key) {
//        /// 🔥 FUTURE: ffmpeg processing
//        return key;
//    }
//}
