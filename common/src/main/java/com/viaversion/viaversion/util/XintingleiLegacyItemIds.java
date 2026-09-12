/* Xintinglei custom compatibility support. */
package com.viaversion.viaversion.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps 1.21.4 Fabric item IDs from being converted to air by newer mappings. */
public final class XintingleiLegacyItemIds {
    private static final Pattern RAW_ID = Pattern.compile("\\\"raw_id\\\"\\s*:\\s*(\\d+)");
    private static final IntSet IDS = loadSection("items", "blocks");
    private static final IntSet BLOCK_STATE_IDS = loadSection("block_states", null);

    private XintingleiLegacyItemIds() {
    }

    public static boolean isLegacy(final int id) {
        return IDS.contains(id);
    }

    public static boolean isLegacyBlockState(final int id) {
        return BLOCK_STATE_IDS.contains(id);
    }

    private static IntSet loadSection(final String sectionName, final String nextSectionName) {
        final IntSet ids = new IntOpenHashSet();
        final Path root = Path.of(System.getProperty("user.dir", "."));
        final List<Path> candidates = List.of(
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
                final int sectionStart = json.indexOf('"' + sectionName + '"');
                final int nextSectionStart = nextSectionName == null ? -1 : json.indexOf('"' + nextSectionName + '"');
                if (sectionStart < 0) {
                    continue;
                }
                final String section = json.substring(sectionStart,
                    nextSectionStart > sectionStart ? nextSectionStart : json.length());
                final Matcher matcher = RAW_ID.matcher(section);
                while (matcher.find()) {
                    ids.add(Integer.parseInt(matcher.group(1)));
                }
                if (!ids.isEmpty()) {
                    break;
                }
            } catch (IOException | NumberFormatException ignored) {
                // Keep normal ViaVersion behavior when the optional manifest is absent.
            }
        }
        return ids;
    }
}
