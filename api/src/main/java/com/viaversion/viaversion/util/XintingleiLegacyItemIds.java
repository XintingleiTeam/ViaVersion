/* Xintinglei custom compatibility support. */
package com.viaversion.viaversion.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Keeps 1.21.4 Fabric item IDs from being converted to air by newer mappings. */
public final class XintingleiLegacyItemIds {
    public static final int ITEM_OFFSET = 32768;
    public static final int BLOCK_STATE_OFFSET = 131072;
    private static final IntSet IDS = loadSection("items");
    private static final IntSet BLOCK_IDS = loadSection("blocks");
    private static final IntSet BLOCK_STATE_IDS = loadSection("block_states");

    private XintingleiLegacyItemIds() {
    }

    public static boolean isLegacy(final int id) {
        return IDS.contains(id);
    }

    public static boolean isLegacyBlockState(final int id) {
        return BLOCK_STATE_IDS.contains(id);
    }

    // Only raw IDs on the 1.21.4 boundary represent Fabric entries. Intermediate
    // vanilla versions may reuse the same numbers (e.g. the 1.21.9 template = 1409).
    public static int clientItem(final String sourceVersion, final int id) {
        if (id >= ITEM_OFFSET && IDS.contains(id - ITEM_OFFSET)) return id;
        return "1.21.4".equals(sourceVersion) && IDS.contains(id) ? ITEM_OFFSET + id : -1;
    }

    public static int serverItem(final String sourceVersion, final int id) {
        if (id < ITEM_OFFSET || !IDS.contains(id - ITEM_OFFSET)) return -1;
        return "1.21.4".equals(sourceVersion) ? id - ITEM_OFFSET : id;
    }

    public static int clientBlockState(final String sourceVersion, final int id) {
        if (id >= BLOCK_STATE_OFFSET && BLOCK_STATE_IDS.contains(id - BLOCK_STATE_OFFSET)) return id;
        return "1.21.4".equals(sourceVersion) && BLOCK_STATE_IDS.contains(id) ? BLOCK_STATE_OFFSET + id : -1;
    }

    public static int clientBlock(final String sourceVersion, final int id) {
        if (id >= ITEM_OFFSET && BLOCK_IDS.contains(id - ITEM_OFFSET)) return id;
        return "1.21.4".equals(sourceVersion) && BLOCK_IDS.contains(id) ? ITEM_OFFSET + id : -1;
    }

    public static int serverBlock(final String sourceVersion, final int id) {
        if (id < ITEM_OFFSET || !BLOCK_IDS.contains(id - ITEM_OFFSET)) return -1;
        return "1.21.4".equals(sourceVersion) ? id - ITEM_OFFSET : id;
    }

    private static IntSet loadSection(final String sectionName) {
        final IntSet ids = new IntOpenHashSet();
        final Path root = Path.of(System.getProperty("user.dir", "."));
        final List<Path> candidates = List.of(
            Path.of(System.getProperty("xintinglei.compatManifest", root.resolve("plugins/Geyser-Velocity/extensions/xintinglei-mod-compat/xintinglei-mod-compat.json").toString())),
            root.resolve("plugins/Geyser-Velocity/extensions/xintinglei-mod-compat/xintinglei-mod-compat.json"),
            root.resolve("plugins/Geyser-Velocity/xintinglei-mod-compat.json"),
            root.resolve("plugins/ViaVersion/xintinglei-mod-compat.json"),
            root.resolve("xintinglei-mod-compat.json"),
            root.resolve("config/xintinglei-mod-compat.json")
        );
        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                final String json = Files.readString(candidate, StandardCharsets.UTF_8);
                final var section = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray(sectionName);
                if (section == null) {
                    continue;
                }
                final IntSet parsed = new IntOpenHashSet();
                for (var entry : section) {
                    parsed.add(entry.getAsJsonObject().get("raw_id").getAsInt());
                }
                ids.addAll(parsed);
                if (!ids.isEmpty()) {
                    break;
                }
            } catch (IOException | RuntimeException ignored) {
                // Keep normal ViaVersion behavior when the optional manifest is absent.
            }
        }
        return ids;
    }
}
