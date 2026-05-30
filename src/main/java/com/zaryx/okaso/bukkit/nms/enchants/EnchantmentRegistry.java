package com.zaryx.okaso.bukkit.nms.enchants;

import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EnchantmentRegistry {

    private EnchantmentRegistry() {}

    public static Enchantment resolve(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String normalized = normalize(name);
        for (String candidate : aliasesOf(normalized)) {
            Enchantment enchantment = Enchantment.getByName(candidate);
            if (enchantment != null) {
                return enchantment;
            }
        }

        return Enchantment.getByName(normalized);
    }

    public static boolean exists(String name) {
        return resolve(name) != null;
    }

    public static String canonicalName(String name) {
        if (name == null) return "";
        return normalize(name);
    }

    public static List<String> aliasesOf(String canonicalName) {
        String name = normalize(canonicalName);
        Set<String> aliases = new LinkedHashSet<String>();
        aliases.add(name);

        if (name.equals("SHARPNESS") || name.equals("DAMAGE_ALL")) aliases.addAll(Arrays.asList("DAMAGE_ALL", "SHARPNESS"));
        if (name.equals("SMITE") || name.equals("DAMAGE_UNDEAD")) aliases.addAll(Arrays.asList("DAMAGE_UNDEAD", "SMITE"));
        if (name.equals("BANE_OF_ARTHROPODS") || name.equals("DAMAGE_ARTHROPODS")) aliases.addAll(Arrays.asList("DAMAGE_ARTHROPODS", "BANE_OF_ARTHROPODS"));

        if (name.equals("PROTECTION") || name.equals("PROTECTION_ENVIRONMENTAL")) aliases.addAll(Arrays.asList("PROTECTION_ENVIRONMENTAL", "PROTECTION"));
        if (name.equals("FIRE_PROTECTION") || name.equals("PROTECTION_FIRE")) aliases.addAll(Arrays.asList("PROTECTION_FIRE", "FIRE_PROTECTION"));
        if (name.equals("FEATHER_FALLING") || name.equals("PROTECTION_FALL")) aliases.addAll(Arrays.asList("PROTECTION_FALL", "FEATHER_FALLING"));
        if (name.equals("BLAST_PROTECTION") || name.equals("PROTECTION_EXPLOSIONS")) aliases.addAll(Arrays.asList("PROTECTION_EXPLOSIONS", "BLAST_PROTECTION"));
        if (name.equals("PROJECTILE_PROTECTION") || name.equals("PROTECTION_PROJECTILE")) aliases.addAll(Arrays.asList("PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION"));

        if (name.equals("POWER") || name.equals("ARROW_DAMAGE")) aliases.addAll(Arrays.asList("ARROW_DAMAGE", "POWER"));
        if (name.equals("PUNCH") || name.equals("ARROW_KNOCKBACK")) aliases.addAll(Arrays.asList("ARROW_KNOCKBACK", "PUNCH"));
        if (name.equals("FLAME") || name.equals("ARROW_FIRE")) aliases.addAll(Arrays.asList("ARROW_FIRE", "FLAME"));
        if (name.equals("INFINITY") || name.equals("ARROW_INFINITE")) aliases.addAll(Arrays.asList("ARROW_INFINITE", "INFINITY"));
        if (name.equals("LUCK_OF_THE_SEA") || name.equals("LUCK")) aliases.addAll(Arrays.asList("LUCK", "LUCK_OF_THE_SEA"));
        if (name.equals("LURE") || name.equals("LURE")) aliases.add("LURE");

        if (name.equals("EFFICIENCY") || name.equals("DIG_SPEED")) aliases.addAll(Arrays.asList("DIG_SPEED", "EFFICIENCY"));
        if (name.equals("UNBREAKING") || name.equals("DURABILITY")) aliases.addAll(Arrays.asList("DURABILITY", "UNBREAKING"));
        if (name.equals("FORTUNE") || name.equals("LOOT_BONUS_BLOCKS")) aliases.addAll(Arrays.asList("LOOT_BONUS_BLOCKS", "FORTUNE"));
        if (name.equals("LOOTING") || name.equals("LOOT_BONUS_MOBS")) aliases.addAll(Arrays.asList("LOOT_BONUS_MOBS", "LOOTING"));

        if (name.equals("MENDING")) aliases.add("MENDING");
        if (name.equals("VANISHING_CURSE") || name.equals("VANISHING")) aliases.addAll(Arrays.asList("VANISHING_CURSE", "VANISHING"));
        if (name.equals("BINDING_CURSE") || name.equals("BINDING")) aliases.addAll(Arrays.asList("BINDING_CURSE", "BINDING"));
        if (name.equals("SOUL_SPEED")) aliases.add("SOUL_SPEED");
        if (name.equals("SWIFT_SNEAK")) aliases.add("SWIFT_SNEAK");

        return new ArrayList<String>(aliases);
    }

    private static String normalize(String name) {
        return name.trim().toUpperCase().replace(' ', '_');
    }
}
