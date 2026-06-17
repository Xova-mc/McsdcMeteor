package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.system.ServerStorage;

import java.util.ArrayList;
import java.util.List;

public final class BrowseSearchState {
    public static final BrowseSearchState LAST = new BrowseSearchState();

    public SearchFlag visited = SearchFlag.ANY;
    public SearchFlag modded = SearchFlag.ANY;
    public SearchFlag whitelist = SearchFlag.ANY;
    public SearchFlag cracked = SearchFlag.ANY;
    public SearchFlag griefed = SearchFlag.ANY;
    public SearchFlag saved = SearchFlag.ANY;
    public SearchFlag active = SearchFlag.YES;
    public SearchFlag hasHistory = SearchFlag.ANY;
    public SearchFlag hasNotes = SearchFlag.ANY;
    public SearchFlag defaultMotd = SearchFlag.ANY;
    public SearchFlag communityMotd = SearchFlag.ANY;
    public SearchFlag creativeMotd = SearchFlag.ANY;
    public SearchFlag bigotryMotd = SearchFlag.ANY;
    public SearchFlag furryMotd = SearchFlag.ANY;
    public SearchFlag lgbtMotd = SearchFlag.ANY;
    public boolean hideOffline = true;
    public boolean advancedMotd = false;
    public SearchVersion version = SearchVersion.ANY;
    public List<ServerStorage> results = new ArrayList<>();
    public String statusMessage = "";

    public BrowseSearchState copy() {
        BrowseSearchState other = new BrowseSearchState();
        other.copyFrom(this);
        return other;
    }

    public void copyFrom(BrowseSearchState other) {
        visited = other.visited;
        modded = other.modded;
        whitelist = other.whitelist;
        cracked = other.cracked;
        griefed = other.griefed;
        saved = other.saved;
        active = other.active;
        hasHistory = other.hasHistory;
        hasNotes = other.hasNotes;
        defaultMotd = other.defaultMotd;
        communityMotd = other.communityMotd;
        creativeMotd = other.creativeMotd;
        bigotryMotd = other.bigotryMotd;
        furryMotd = other.furryMotd;
        lgbtMotd = other.lgbtMotd;
        hideOffline = other.hideOffline;
        advancedMotd = other.advancedMotd;
        version = other.version;
        results = new ArrayList<>(other.results);
        statusMessage = other.statusMessage;
    }

    public Object resolveVersion() {
        if (version.number != -1) return version.number;
        return null;
    }

    public boolean allCoreFlagsAny() {
        for (SearchFlag flag : List.of(
            visited, griefed, modded, saved, whitelist, active, cracked
        )) {
            if (flag.bool != null) return false;
        }
        return true;
    }

    public String summary() {
        List<String> parts = new ArrayList<>();

        addFlagSummary(parts, "Active", active);
        addFlagSummary(parts, "Visited", visited);
        addFlagSummary(parts, "Modded", modded);
        addFlagSummary(parts, "Cracked", cracked);

        if (version != SearchVersion.ANY) {
            parts.add("Ver: " + version.version);
        }

        if (hideOffline) parts.add("Online only");

        if (parts.isEmpty()) return "All servers (pick filters to narrow results)";

        String text = String.join(" · ", parts);
        return text.length() > 72 ? text.substring(0, 69) + "..." : text;
    }

    private static void addFlagSummary(List<String> parts, String name, SearchFlag flag) {
        if (flag != SearchFlag.ANY) parts.add(name + ": " + flag.label());
    }
}
