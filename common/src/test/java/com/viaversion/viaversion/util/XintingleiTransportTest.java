package com.viaversion.viaversion.util;

import com.viaversion.viaversion.api.data.*;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.type.types.chunk.*;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class XintingleiTransportTest {
    static final String[] VERSIONS = {"1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.9", "1.21.11", "26.1", "26.2", "26.3"};
    static final List<Probe> CHAIN = new ArrayList<>();

    @BeforeAll
    static void setup() throws Exception {
        Path fixture = Files.createTempFile("xtl-production-ids", ".json");
        try (var input = XintingleiTransportTest.class.getResourceAsStream("/xintinglei-production.json")) {
            Files.copy(Objects.requireNonNull(input), fixture, StandardCopyOption.REPLACE_EXISTING);
        }
        System.setProperty("xintinglei.compatManifest", fixture.toString());
        assertTrue(XintingleiLegacyItemIds.isLegacy(1409));
        System.clearProperty("xintinglei.compatManifest");
        Files.delete(fixture);
        MappingDataLoader.loadGlobalIdentifiers();
        for (int i = 0; i < VERSIONS.length - 1; i++) CHAIN.add(new Probe(VERSIONS[i], VERSIONS[i + 1]));
    }

    static class Probe extends MappingDataBase {
        Probe(String from, String to) {
            super(from, to);
            var loader = MappingDataLoader.INSTANCE;
            var data = loader.loadNBT("mappings-" + from + "to" + to + ".nbt");
            var a = loader.loadNBT("identifiers-" + from + ".nbt");
            var b = loader.loadNBT("identifiers-" + to + ".nbt");
            itemMappings = loadFullOrBiMappings(data, a, b, "items");
            blockStateMappings = loader.loadMappings(data, "blockstates");
        }
    }

    @Test
    void everyVanillaItemRetainsIdentityAndRoundTrips() {
        var loader = MappingDataLoader.INSTANCE;
        var initial = loader.identifiersFromGlobalIds(loader.loadNBT("identifiers-1.21.4.nbt"), "items");
        for (int raw = 0; raw < initial.size(); raw++) {
            int id = raw;
            for (int hop = 0; hop < CHAIN.size(); hop++) {
                int expected = CHAIN.get(hop).getItemMappings().getNewId(id);
                id = CHAIN.get(hop).getNewItemId(id);
                // Compare with upstream's table (which also contains legitimate renames).
                assertEquals(expected, id, "Vanilla " + initial.get(raw) + " hop " + hop);
            }
            for (int hop = CHAIN.size() - 1; hop >= 0; hop--) id = CHAIN.get(hop).getOldItemId(id);
            assertEquals(raw, id);
        }
        int template = 1322;
        for (Probe step : CHAIN) template = step.getNewItemId(template);
        var latest = loader.identifiersFromGlobalIds(loader.loadNBT("identifiers-26.3.nbt"), "items");
        assertEquals(initial.get(1322), latest.get(template), "Smithing template must not collide with legacy mod items");
    }

    @Test
    void allProductionItemsAndBlockStatesUseSeparateTransportIds() throws Exception {
        var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(
            Objects.requireNonNull(getClass().getResourceAsStream("/xintinglei-production.json")), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        for (var entry : json.getAsJsonArray("items")) {
            int raw = entry.getAsJsonObject().get("raw_id").getAsInt(), id = raw;
            for (Probe step : CHAIN) {
                id = step.getNewItemId(id);
                assertEquals(32768 + raw, id);
            }
            for (int hop = CHAIN.size() - 1; hop >= 0; hop--) id = CHAIN.get(hop).getOldItemId(id);
            assertEquals(raw, id);
        }
        for (var entry : json.getAsJsonArray("block_states")) {
            int raw = entry.getAsJsonObject().get("raw_id").getAsInt(), id = raw;
            for (Probe step : CHAIN) {
                id = step.getNewBlockStateId(id);
                assertEquals(131072 + raw, id);
            }
        }
    }

    @Test
    void everyVanillaBlockStateUsesUpstreamMapping() {
        for (int raw = 0; raw < CHAIN.get(0).getBlockStateMappings().size(); raw++) {
            int id = raw;
            for (Probe step : CHAIN) {
                int expected = step.getBlockStateMappings().getNewId(id);
                if (expected < 0) break; // Vanilla removes some states between versions.
                id = step.getNewBlockStateId(id);
                assertEquals(expected, id, "Vanilla block " + raw);
            }
        }
    }

    @Test
    void directAndLocalChunkPalettesDoNotTruncateTransportStates() {
        for (int size : new int[]{1, 16, 257, 4096}) {
            var palette = new DataPaletteImpl(4096);
            for (int i = 0; i < 4096; i++) palette.setIdAt(i, 131072 + 28000 + (i % size));
            for (var codec : List.of(new PaletteType1_18(PaletteType.BLOCKS, 15), new PaletteType1_21_5(PaletteType.BLOCKS, 15))) {
                var buffer = Unpooled.buffer();
                try {
                    codec.write(buffer, palette);
                    assertEquals(codec.serializedSize(palette), buffer.readableBytes());
                    var decoded = codec.read(buffer);
                    for (int i = 0; i < 4096; i++) assertEquals(palette.idAt(i), decoded.idAt(i));
                    assertEquals(0, buffer.readableBytes());
                } finally { buffer.release(); }
            }
        }
    }
}
