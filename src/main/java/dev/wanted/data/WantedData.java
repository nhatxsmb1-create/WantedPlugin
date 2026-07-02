package dev.wanted.data;

import java.util.UUID;

public class WantedData {
    private final UUID uuid;
    private String name;
    private int score;
    private int pvpKills;
    private int mobKills;
    private int wantedLevel;
    private double extraBounty;
    private int totalWantedKills;
    private int seasonScore;

    public WantedData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getScore() { return score; }
    public int getPvpKills() { return pvpKills; }
    public int getMobKills() { return mobKills; }
    public int getWantedLevel() { return wantedLevel; }
    public double getExtraBounty() { return extraBounty; }
    public int getTotalWantedKills() { return totalWantedKills; }
    public int getSeasonScore() { return seasonScore; }
    public boolean isWanted() { return wantedLevel > 0; }

    public void setScore(int score) { this.score = Math.max(0, score); }
    public void addScore(int amount) { this.score = Math.max(0, this.score + amount); }
    public void setPvpKills(int v) { this.pvpKills = v; }
    public void addPvpKill() { this.pvpKills++; }
    public void setMobKills(int v) { this.mobKills = v; }
    public void addMobKill() { this.mobKills++; }
    public void setWantedLevel(int level) { this.wantedLevel = Math.max(0, level); }
    public void addExtraBounty(double amount) { this.extraBounty += amount; }
    public void clearExtraBounty() { this.extraBounty = 0; }
    public void addWantedKill() { this.totalWantedKills++; }
    public void setSeasonScore(int v) { this.seasonScore = v; }
    public void addSeasonScore(int v) { this.seasonScore += v; }
    public void resetSeason() { this.seasonScore = 0; }
}
