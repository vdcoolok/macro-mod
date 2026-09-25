package com.example.macromod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class TargetRegistry {

    private static final Set<EntityType<?>> hostile = new HashSet<>();
    private static final Set<EntityType<?>> passive = new HashSet<>();
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
}
