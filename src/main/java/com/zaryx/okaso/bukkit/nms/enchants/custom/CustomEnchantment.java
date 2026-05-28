package com.zaryx.okaso.bukkit.nms.enchants.custom;

import com.zaryx.okaso.bukkit.placeholder.PlaceholderContext;
import com.zaryx.okaso.bukkit.utility.Text;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CustomEnchantment {

    private final String key;
    private final String displayName;
    private final int maxLevel;
    private final boolean treasure;
    private final boolean cursed;
    private final boolean obtainableFromEnchantingTable;
    private final List<String> loreLines;
    private final Set<String> aliases;
    private final Set<String> conflicts;
    private final Set<Material> allowedMaterials;
    private final List<CustomEnchantmentHook> hooks;

    private CustomEnchantment(Builder builder) {
        this.key = normalize(builder.key);
        this.displayName = builder.displayName == null ? formatKey(this.key) : builder.displayName;
        this.maxLevel = Math.max(1, builder.maxLevel);
        this.treasure = builder.treasure;
        this.cursed = builder.cursed;
        this.obtainableFromEnchantingTable = builder.obtainableFromEnchantingTable;
        this.loreLines = Collections.unmodifiableList(new ArrayList<String>(builder.loreLines));
        this.aliases = Collections.unmodifiableSet(new LinkedHashSet<String>(builder.aliases));
        this.conflicts = Collections.unmodifiableSet(new LinkedHashSet<String>(builder.conflicts));
        this.allowedMaterials = Collections.unmodifiableSet(new LinkedHashSet<Material>(builder.allowedMaterials));
        this.hooks = Collections.unmodifiableList(new ArrayList<CustomEnchantmentHook>(builder.hooks));
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isTreasure() {
        return treasure;
    }

    public boolean isCursed() {
        return cursed;
    }

    public boolean isObtainableFromEnchantingTable() {
        return obtainableFromEnchantingTable;
    }

    public List<String> getLoreLines() {
        return loreLines;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public Set<String> getConflicts() {
        return conflicts;
    }

    public Set<Material> getAllowedMaterials() {
        return allowedMaterials;
    }

    public List<CustomEnchantmentHook> getHooks() {
        return hooks;
    }

    public boolean canApplyTo(ItemStack item) {
        if (item == null) {
            return false;
        }

        return canApplyTo(item.getType());
    }

    public boolean canApplyTo(Material material) {
        if (material == null) {
            return false;
        }

        return allowedMaterials.isEmpty() || allowedMaterials.contains(material);
    }

    public boolean canAppearInEnchantingTable(ItemStack item) {
        return obtainableFromEnchantingTable && canApplyTo(item);
    }

    public boolean matches(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }

        if (key.equals(normalized)) {
            return true;
        }

        for (String alias : aliases) {
            if (alias.equals(normalized)) {
                return true;
            }
        }

        return false;
    }

    public List<String> formatLoreLines(int level) {
        PlaceholderContext context = new PlaceholderContext()
                .add("key", key)
                .add("name", displayName)
                .add("level", level)
                .add("max_level", maxLevel)
                .add("roman", toRoman(level))
                .add("treasure", treasure)
                .add("cursed", cursed);

        List<String> formatted = new ArrayList<String>();
        List<String> templates = loreLines.isEmpty()
                ? Collections.singletonList("&7%name% %roman%")
                : loreLines;

        for (String line : templates) {
            formatted.add(Text.text(applyPlaceholders(line, context)));
        }

        return formatted;
    }

    public void fireHooks(CustomEnchantmentEffectContext context) {
        if (context == null || hooks.isEmpty()) {
            return;
        }

        for (CustomEnchantmentHook hook : hooks) {
            if (hook == null) {
                continue;
            }

            hook.handle(context);
        }
    }

    public static Builder builder(String key) {
        return new Builder(key);
    }

    private static String applyPlaceholders(String line, PlaceholderContext context) {
        if (line == null) {
            return "";
        }

        String result = line;
        for (java.util.Map.Entry<String, String> entry : context.getValues().entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private static String formatKey(String key) {
        if (key == null || key.isEmpty()) {
            return "Custom Enchantment";
        }

        StringBuilder builder = new StringBuilder();
        for (String part : key.split("[_\\-. ]+")) {
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }

        return builder.length() == 0 ? key : builder.toString();
    }

    private static String toRoman(int number) {
        if (number <= 0) {
            return "0";
        }

        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        int remaining = number;

        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                remaining -= values[i];
                result.append(symbols[i]);
            }
        }

        return result.length() == 0 ? String.valueOf(number) : result.toString();
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Builder {

        private final String key;
        private String displayName;
        private int maxLevel = 1;
        private boolean treasure;
        private boolean cursed;
        private boolean obtainableFromEnchantingTable;
        private final List<String> loreLines = new ArrayList<String>();
        private final Set<String> aliases = new LinkedHashSet<String>();
        private final Set<String> conflicts = new LinkedHashSet<String>();
        private final Set<Material> allowedMaterials = new LinkedHashSet<Material>();
        private final List<CustomEnchantmentHook> hooks = new CopyOnWriteArrayList<CustomEnchantmentHook>();

        private Builder(String key) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("key must not be blank");
            }

            this.key = key;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = Math.max(1, maxLevel);
            return this;
        }

        public Builder treasure(boolean treasure) {
            this.treasure = treasure;
            return this;
        }

        public Builder cursed(boolean cursed) {
            this.cursed = cursed;
            return this;
        }

        public Builder obtainableFromEnchantingTable(boolean obtainableFromEnchantingTable) {
            this.obtainableFromEnchantingTable = obtainableFromEnchantingTable;
            return this;
        }

        public Builder canAppearInEnchantingTable(boolean obtainableFromEnchantingTable) {
            return obtainableFromEnchantingTable(obtainableFromEnchantingTable);
        }

        public Builder allowedMaterials(Material... materials) {
            if (materials != null) {
                for (Material material : materials) {
                    if (material != null) {
                        this.allowedMaterials.add(material);
                    }
                }
            }
            return this;
        }

        public Builder allowedItems(Material... materials) {
            return allowedMaterials(materials);
        }

        public Builder disallowedMaterials(Material... materials) {
            if (materials != null) {
                for (Material material : materials) {
                    if (material != null) {
                        this.allowedMaterials.remove(material);
                    }
                }
            }
            return this;
        }

        public Builder allowAllItems() {
            this.allowedMaterials.clear();
            return this;
        }

        public Builder hook(CustomEnchantmentHook hook) {
            if (hook != null) {
                this.hooks.add(hook);
            }
            return this;
        }

        public Builder hooks(CustomEnchantmentHook... hooks) {
            if (hooks != null) {
                for (CustomEnchantmentHook hook : hooks) {
                    hook(hook);
                }
            }
            return this;
        }

        public Builder lore(String... loreLines) {
            if (loreLines != null) {
                for (String line : loreLines) {
                    if (line != null) {
                        this.loreLines.add(line);
                    }
                }
            }
            return this;
        }

        public Builder alias(String alias) {
            String normalized = normalize(alias);
            if (normalized != null) {
                this.aliases.add(normalized);
            }
            return this;
        }

        public Builder aliases(String... aliases) {
            if (aliases != null) {
                for (String alias : aliases) {
                    alias(alias);
                }
            }
            return this;
        }

        public Builder conflict(String enchantmentKey) {
            String normalized = normalize(enchantmentKey);
            if (normalized != null) {
                this.conflicts.add(normalized);
            }
            return this;
        }

        public Builder conflicts(String... enchantmentKeys) {
            if (enchantmentKeys != null) {
                for (String enchantmentKey : enchantmentKeys) {
                    conflict(enchantmentKey);
                }
            }
            return this;
        }

        public CustomEnchantment build() {
            return new CustomEnchantment(this);
        }
    }
}
