package com.fanzzi.backend.post.util;

public final class FeedBucketUtil {

    public static final int POSTS_PER_BUCKET = 50_000;

    // Optional safety cap (future-proof)
    private static final long MAX_BUCKET_ID = Long.MAX_VALUE / POSTS_PER_BUCKET;

    private FeedBucketUtil() {}

    // ========================================
    // BUCKET FROM SEQ
    // ========================================
    public static long calculateBucket(long seq) {

        if (seq < 0) {
            throw new IllegalArgumentException("Sequence cannot be negative");
        }

        return seq / POSTS_PER_BUCKET;
    }

    // ========================================
    // BUCKET START SEQ
    // ========================================
    public static long bucketStartSeq(long bucketId) {

        validateBucket(bucketId);

        return safeMultiply(bucketId, POSTS_PER_BUCKET);
    }

    // ========================================
    // BUCKET END SEQ
    // ========================================
    public static long bucketEndSeq(long bucketId) {

        validateBucket(bucketId);

        long nextStart = safeMultiply(bucketId + 1, POSTS_PER_BUCKET);

        return nextStart - 1;
    }

    // ========================================
    // PREVIOUS BUCKET
    // ========================================
    public static long previousBucket(long bucketId) {

        validateBucket(bucketId);

        return bucketId == 0 ? 0 : bucketId - 1;
    }

    // ========================================
    // NEXT BUCKET
    // ========================================
    public static long nextBucket(long bucketId) {

        validateBucket(bucketId);

        if (bucketId >= MAX_BUCKET_ID) {
            throw new IllegalStateException("Bucket overflow");
        }

        return bucketId + 1;
    }

    // ========================================
    // SAME BUCKET CHECK
    // ========================================
    public static boolean isSameBucket(long seq1, long seq2) {

        if (seq1 < 0 || seq2 < 0) {
            throw new IllegalArgumentException("Sequence cannot be negative");
        }

        return calculateBucket(seq1) == calculateBucket(seq2);
    }

    // ========================================
    // RANGE CHECK
    // ========================================
    public static boolean isSeqInBucket(long seq, long bucketId) {

        if (seq < 0) {
            throw new IllegalArgumentException("Sequence cannot be negative");
        }

        validateBucket(bucketId);

        long start = bucketStartSeq(bucketId);
        long end = bucketEndSeq(bucketId);

        return seq >= start && seq <= end;
    }

    // ========================================
    // SAFE MULTIPLY (CRITICAL)
    // ========================================
    private static long safeMultiply(long a, long b) {

        if (a > 0 && b > 0 && a > Long.MAX_VALUE / b) {
            throw new ArithmeticException("Overflow in multiplication");
        }

        return a * b;
    }

    // ========================================
    // VALIDATION
    // ========================================
    private static void validateBucket(long bucketId) {

        if (bucketId < 0) {
            throw new IllegalArgumentException("BucketId cannot be negative");
        }

        if (bucketId > MAX_BUCKET_ID) {
            throw new IllegalArgumentException("BucketId too large");
        }
    }
}