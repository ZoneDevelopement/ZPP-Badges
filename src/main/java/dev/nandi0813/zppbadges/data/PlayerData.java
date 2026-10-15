package dev.nandi0813.zppbadges.data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PlayerData {

    private final UUID uuid;
    private final Set<String> unlocked = Collections.synchronizedSet(new LinkedHashSet<>());
    private final List<String> active = new CopyOnWriteArrayList<>();
    private volatile int eventWins;
    private volatile int ffaKills;
    private volatile boolean dirty;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Set<String> getUnlocked() {
        return unlocked;
    }

    public List<String> getActive() {
        return active;
    }

    public boolean isUnlocked(String badgeId) {
        return unlocked.contains(badgeId);
    }

    public boolean isActive(String badgeId) {
        return active.contains(badgeId);
    }

    public int getEventWins() {
        return eventWins;
    }

    public void setEventWins(int eventWins) {
        this.eventWins = eventWins;
    }

    public synchronized void incrementEventWins() {
        eventWins++;
        dirty = true;
    }

    public int getFfaKills() {
        return ffaKills;
    }

    public void setFfaKills(int ffaKills) {
        this.ffaKills = ffaKills;
    }

    public synchronized void incrementFfaKills() {
        ffaKills++;
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

}
