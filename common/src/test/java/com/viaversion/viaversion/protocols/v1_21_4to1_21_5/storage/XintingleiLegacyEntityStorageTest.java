/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viaversion.protocols.v1_21_4to1_21_5.storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class XintingleiLegacyEntityStorageTest {

    @Test
    void rejectsVanillaEntityTypeIds() {
        Assertions.assertFalse(XintingleiLegacyEntityStorage.isConfiguredTypeIdSafe(148, 149));
        Assertions.assertTrue(XintingleiLegacyEntityStorage.isConfiguredTypeIdSafe(149, 149));
    }

    @Test
    void tracksEntityLifecycle() {
        final XintingleiLegacyEntityStorage storage = new XintingleiLegacyEntityStorage();
        storage.addHappyGhast(42);
        Assertions.assertTrue(storage.isHappyGhast(42));

        storage.remove(42);
        Assertions.assertFalse(storage.isHappyGhast(42));

        storage.addHappyGhast(42);
        storage.clear();
        Assertions.assertFalse(storage.isHappyGhast(42));
    }
}
