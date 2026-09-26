package com.example.macromod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class TargetRegistry {

    public static final String PASSIVE_TOKEN = "__passive__";

    private static final Set<EntityType<?>> hostile = new HashSet<>();
    private static final Set<EntityType<?>> passive = new HashSet<>();
    private static final List<String> mobs = new ArrayList<>();
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

    public static synchronized List<String> mobNames() {
        if (mobs.isEmpty()) {
            Set<String> names = new TreeSet<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                if (!isMob(type)) continue;
                String name = type.toShortString().toLowerCase(Locale.ROOT);
                if (name == null || name.isBlank()) continue;
                names.add(name);
            }
            mobs.addAll(names);
        }
        return Collections.unmodifiableList(mobs);
    }

    private static boolean isMob(EntityType<?> type) {
        if (type.getCategory() == MobCategory.MISC) return false;
        return LivingEntity.class.isAssignableFrom(type.getBaseClass());
    }
}
