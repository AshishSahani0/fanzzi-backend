package com.fanzzi.backend.gifts.enums;



public enum GiftType {

    ROSE("🌹", "Rose", 5),
    FIRE("🔥", "Fire", 20),
    CROWN("👑", "Crown", 100),
    DIAMOND("💎", "Diamond", 500),
    ROCKET("🚀", "Rocket", 1000);

    private final String emoji;
    private final String title;
    private final int price;

    GiftType(String emoji, String title, int price) {
        this.emoji = emoji;
        this.title = title;
        this.price = price;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getTitle() {
        return title;
    }

    public int getPrice() {
        return price;
    }
}
