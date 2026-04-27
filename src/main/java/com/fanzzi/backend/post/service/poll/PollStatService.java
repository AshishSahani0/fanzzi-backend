package com.fanzzi.backend.post.service.poll;

import com.fanzzi.backend.post.model.PollOptionStat;
import com.fanzzi.backend.post.repository.PollOptionStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollStatService {

    private final PollOptionStatRepository statRepository;

    // =====================================
    // 🔥 BULK MERGE (RECOMMENDED)
    // =====================================
    public void mergeStats(String postId, Map<String, Long> deltas) {

        if (postId == null || deltas == null || deltas.isEmpty()) return;

        try {

            // 1️⃣ Fetch all existing stats
            List<PollOptionStat> existing =
                    statRepository.findByPostId(postId);

            Map<String, PollOptionStat> existingMap =
                    existing.stream()
                            .collect(Collectors.toMap(
                                    PollOptionStat::getOptionId,
                                    Function.identity(),
                                    (a, b) -> a
                            ));

            List<PollOptionStat> toSave = new ArrayList<>();
            Instant now = Instant.now();

            // 2️⃣ Apply deltas
            for (Map.Entry<String, Long> entry : deltas.entrySet()) {

                String optionId = entry.getKey();
                long delta = entry.getValue();

                if (delta == 0) continue;

                PollOptionStat stat = existingMap.get(optionId);

                if (stat == null) {
                    stat = new PollOptionStat();
                    stat.setPostId(postId);
                    stat.setOptionId(optionId);
                    stat.setVotes(Math.max(delta, 0));
                } else {
                    long newVotes = stat.getVotes() + delta;
                    if (newVotes < 0) newVotes = 0;

                    stat.setVotes(newVotes);
                }

                stat.setUpdatedAt(now);
                toSave.add(stat);
            }

            // 3️⃣ Save in bulk
            if (!toSave.isEmpty()) {
                statRepository.saveAll(toSave);
            }

        } catch (Exception e) {
            log.warn("Poll stat merge failed postId={}", postId, e);
        }
    }

    // =====================================
    // ⚠️ SINGLE SET (ADMIN / DEBUG ONLY)
    // =====================================
    public void setVotes(String postId, String optionId, long votes) {

        if (postId == null || optionId == null) return;

        if (votes < 0) votes = 0;

        try {
            PollOptionStat stat = statRepository
                    .findByPostIdAndOptionId(postId, optionId)
                    .orElseGet(() -> {
                        PollOptionStat s = new PollOptionStat();
                        s.setPostId(postId);
                        s.setOptionId(optionId);
                        return s;
                    });

            stat.setVotes(votes);
            stat.setUpdatedAt(Instant.now());

            statRepository.save(stat);

        } catch (Exception e) {
            log.warn("Poll stat set failed postId={} optionId={}",
                    postId, optionId, e);
        }
    }

    // =====================================
    // 🔥 BULK SAVE (LEGACY SUPPORT)
    // =====================================
    public void saveAll(List<PollOptionStat> stats) {

        if (stats == null || stats.isEmpty()) return;

        try {
            statRepository.saveAll(stats);
        } catch (Exception e) {
            log.warn("Bulk poll stat save failed", e);
        }
    }
}