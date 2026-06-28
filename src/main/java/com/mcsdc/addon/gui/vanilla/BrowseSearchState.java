package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonObject;
import com.mcsdc.addon.system.MOTD;
import com.mcsdc.addon.system.ServerSearchBuilder;
import com.mcsdc.addon.system.ServerStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
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

    @Nullable
    public JsonObject toSearchJson() {
        Object ver = resolveVersion();
        if (ver instanceof String s && s.isEmpty()) return null;

        HashMap<MOTD, Boolean> motds = null;
        if (advancedMotd) {
            motds = new HashMap<>();
            motds.put(MOTD.DEFAULT, defaultMotd.bool);
            motds.put(MOTD.COMMUNITY, communityMotd.bool);
            motds.put(MOTD.CREATIVE, creativeMotd.bool);
            motds.put(MOTD.BIGOTRY, bigotryMotd.bool);
            motds.put(MOTD.FURRY, furryMotd.bool);
            motds.put(MOTD.LGBT, lgbtMotd.bool);
        }

        ServerSearchBuilder.Extra extra = new ServerSearchBuilder.Extra(hasHistory.bool, hasNotes.bool, motds);
        ServerSearchBuilder.Flags flags = new ServerSearchBuilder.Flags(
            visited.bool, griefed.bool, modded.bool, saved.bool,
            whitelist.bool, active.bool, cracked.bool
        );
        ServerSearchBuilder.Search search = new ServerSearchBuilder.Search(
            new ServerSearchBuilder.Version(ver), flags, extra
        );
        return ServerSearchBuilder.createJson(search);
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
