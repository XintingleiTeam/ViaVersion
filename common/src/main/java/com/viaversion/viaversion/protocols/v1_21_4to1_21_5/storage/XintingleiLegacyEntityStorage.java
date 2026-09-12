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

import com.viaversion.viaversion.api.connection.StorableObject;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Tracks the one fixed Happy Ghast Legacy entity while it crosses protocol versions that do not
 * have a Happy Ghast entity type. This is an opt-in compatibility path for the Xintinglei server;
 * it is intentionally not a general-purpose custom entity registry.
 */
public final class XintingleiLegacyEntityStorage implements StorableObject {
    public static final String ENTITY_ID_PROPERTY = "Xintinglei.HappyGhastLegacyEntityId";

    private final IntSet happyGhasts = new IntOpenHashSet();

    public static int configuredEntityTypeId() {
        return Integer.getInteger(ENTITY_ID_PROPERTY, -1);
    }

    public static boolean isConfiguredTypeIdSafe(final int configuredTypeId, final int vanillaMappingSize) {
        return configuredTypeId >= vanillaMappingSize;
    }

    public void addHappyGhast(final int entityId) {
        happyGhasts.add(entityId);
    }

    public boolean isHappyGhast(final int entityId) {
        return happyGhasts.contains(entityId);
    }

    public void remove(final int entityId) {
        happyGhasts.remove(entityId);
    }

    public void clear() {
        happyGhasts.clear();
    }
}
