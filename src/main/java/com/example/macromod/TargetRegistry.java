package com.example.macromod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class TargetRegistry {

    public static final String PASSIVE_TOKEN = "__passive__";

    private static final Set<EntityType<?>> hostile = new HashSet<>();
    private static final Set<EntityType<?>> passive = new HashSet<>();
    private static final List<String> mobIds = new ArrayList<>();
    private static final Map<String, EntityType<?>> byId = new HashMap<>();
    private static boolean initialized = false;

    private TargetRegistry() {}

    public static synchronized void ensureInitialized() {
        if (initialized) return;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (Monster.class.isAssignableFrom(type.getBaseClass())) {
                hostile.add(type);
            } else {
                passive.add(type);
            }
        }
        initialized = true;
    }

    public static boolean isHostile(EntityType<?> type) {
        ensureInitialized();
        return hostile.contains(type);
    }

    public static boolean isPassive(EntityType<?> type) {
        ensureInitialized();
        return passive.contains(type);
    }

    public static Set<EntityType<?>> hostileTypes() {
        ensureInitialized();
        return Collections.unmodifiableSet(hostile);
    }

    public static Set<EntityType<?>> passiveTypes() {
        ensureInitialized();
        return Collections.unmodifiableSet(passive);
    }

    public static String shortName(EntityType<?> type) {
        return type.toShortString().toLowerCase(Locale.ROOT);
    }

    public static synchronized List<String> mobIds() {
        if (mobIds.isEmpty()) {
            Set<String> ids = new TreeSet<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                if (!isMob(type)) continue;
                ids.add(id(type));
            }
            mobIds.addAll(ids);
        }
        return Collections.unmodifiableList(mobIds);
    }

    public static String id(EntityType<?> type) {
        return EntityType.getKey(type).toString();
    }

    public static EntityType<?> resolveId(String token) {
        if (token == null || token.isBlank()) return null;
        String key = token.trim().toLowerCase(Locale.ROOT);
        ensureIds();
        EntityType<?> match = byId.get(key);
        if (match != null) return match;
        if (key.indexOf(':') < 0) {
            match = byId.get("minecraft:" + key);
            if (match != null) return match;
        }
        return null;
    }

    private static synchronized void ensureIds() {
        if (!byId.isEmpty()) return;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            byId.put(id(type).toLowerCase(Locale.ROOT), type);
        }
    }

    private static boolean isMob(EntityType<?> type) {
        if (type.getCategory() == MobCategory.MISC) return false;
        return LivingEntity.class.isAssignableFrom(type.getBaseClass());
    }
}
