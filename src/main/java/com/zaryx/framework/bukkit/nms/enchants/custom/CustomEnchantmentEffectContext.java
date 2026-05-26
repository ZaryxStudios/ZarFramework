package com.zaryx.framework.bukkit.nms.enchants.custom;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.Locale;

public class CustomEnchantmentEffectContext {

    private final CustomEnchantment enchantment;
    private final int level;
    private final LivingEntity wearer;
    private final LivingEntity target;
    private final ItemStack item;
    private final Event event;

    private double damageBonus;
    private double damageMultiplier = 1.0D;
    private double receivedDamageBonus;
    private double receivedDamageMultiplier = 1.0D;
    private double knockbackMultiplier = 1.0D;
    private double swimSpeedMultiplier = 1.0D;
    private double elytraSpeedMultiplier = 1.0D;
    private double flySpeedMultiplier = 1.0D;

    public CustomEnchantmentEffectContext(CustomEnchantment enchantment, int level, LivingEntity wearer, LivingEntity target, ItemStack item, Event event) {
        this.enchantment = enchantment;
        this.level = Math.max(1, level);
        this.wearer = wearer;
        this.target = target;
        this.item = item;
        this.event = event;
    }

    public CustomEnchantment getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }

    public LivingEntity getWearer() {
        return wearer;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public ItemStack getItem() {
        return item;
    }

    public Event getEvent() {
        return event;
    }

    public double getDamageBonus() {
        return damageBonus;
    }

    public void addDamageBonus(double value) {
        this.damageBonus += value;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = Math.max(0.0D, damageMultiplier);
    }

    public double getReceivedDamageBonus() {
        return receivedDamageBonus;
    }

    public void addReceivedDamageBonus(double value) {
        this.receivedDamageBonus += value;
    }

    public double getReceivedDamageMultiplier() {
        return receivedDamageMultiplier;
    }

    public void setReceivedDamageMultiplier(double receivedDamageMultiplier) {
        this.receivedDamageMultiplier = Math.max(0.0D, receivedDamageMultiplier);
    }

    public double getKnockbackMultiplier() {
        return knockbackMultiplier;
    }

    public void setKnockbackMultiplier(double knockbackMultiplier) {
        this.knockbackMultiplier = Math.max(0.0D, knockbackMultiplier);
    }

    public double getSwimSpeedMultiplier() {
        return swimSpeedMultiplier;
    }

    public void setSwimSpeedMultiplier(double swimSpeedMultiplier) {
        this.swimSpeedMultiplier = Math.max(0.0D, swimSpeedMultiplier);
    }

    public double getElytraSpeedMultiplier() {
        return elytraSpeedMultiplier;
    }

    public void setElytraSpeedMultiplier(double elytraSpeedMultiplier) {
        this.elytraSpeedMultiplier = Math.max(0.0D, elytraSpeedMultiplier);
    }

    public double getFlySpeedMultiplier() {
        return flySpeedMultiplier;
    }

    public void setFlySpeedMultiplier(double flySpeedMultiplier) {
        this.flySpeedMultiplier = Math.max(0.0D, flySpeedMultiplier);
    }

    public void spawnParticle(String particleName, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (wearer == null || particleName == null) {
            return;
        }

        Location location = wearer.getLocation();
        World world = location == null ? null : location.getWorld();
        if (world == null) {
            return;
        }

        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            Object particle = null;
            Object[] constants = particleClass.getEnumConstants();
            if (constants != null) {
                String lookup = particleName.trim().toUpperCase(Locale.ROOT);
                for (Object constant : constants) {
                    if (constant != null && lookup.equals(constant.toString())) {
                        particle = constant;
                        break;
                    }
                }
            }
            if (particle == null) {
                throw new IllegalStateException("Particle not available");
            }
            Method method = world.getClass().getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class, double.class);
            method.invoke(world, particle, location, Integer.valueOf(Math.max(1, count)), offsetX, offsetY, offsetZ, extra);
            return;
        } catch (Throwable ignored) {
        }

        try {
            Effect effect = Effect.valueOf(particleName.trim().toUpperCase(Locale.ROOT));
            world.playEffect(location, effect, 0);
        } catch (Throwable ignored) {
        }
    }

    public void playSound(String soundName, float volume, float pitch) {
        if (wearer == null || soundName == null) {
            return;
        }

        Location location = wearer.getLocation();
        World world = location == null ? null : location.getWorld();
        if (world == null) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase(Locale.ROOT));
            world.playSound(location, sound, volume, pitch);
        } catch (Throwable ignored) {
        }
    }

    public void addPotionEffect(String potionName, int duration, int amplifier, boolean ambient, boolean particles) {
        if (wearer == null || potionName == null) {
            return;
        }

        PotionEffectType type = PotionEffectType.getByName(potionName.trim().toUpperCase(Locale.ROOT));
        if (type == null) {
            return;
        }

        wearer.addPotionEffect(new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier), ambient, particles));
    }

    public void addSpeedSupportEffects() {
        addPotionEffect("SPEED", 40 + (level * 20), Math.max(0, level - 1), true, true);
        addPotionEffect("DOLPHINS_GRACE", 40 + (level * 20), Math.max(0, level - 1), true, true);
    }

    public void boostVelocity(double multiplier) {
        if (!(wearer instanceof Player)) {
            return;
        }

        Player player = (Player) wearer;
        try {
            player.setVelocity(player.getVelocity().multiply(multiplier));
        } catch (Throwable ignored) {
        }
    }

    public void setPlayerFlySpeed(float baseSpeed) {
        if (!(wearer instanceof Player)) {
            return;
        }

        Player player = (Player) wearer;
        try {
            float speed = Math.max(0.0F, Math.min(1.0F, baseSpeed * (float) flySpeedMultiplier));
            player.setFlySpeed(speed);
        } catch (Throwable ignored) {
        }
    }

    public void applyMovementModifiers() {
        if (!(wearer instanceof Player)) {
            return;
        }

        Player player = (Player) wearer;

        if (swimSpeedMultiplier > 1.0D) {
            addSpeedSupportEffects();
        }

        if (elytraSpeedMultiplier > 1.0D && isGliding(player)) {
            boostVelocity(elytraSpeedMultiplier);
        }

        if (flySpeedMultiplier > 1.0D && player.isFlying()) {
            setPlayerFlySpeed(0.1F);
        }
    }

    private boolean isGliding(Player player) {
        try {
            Method method = player.getClass().getMethod("isGliding");
            Object value = method.invoke(player);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
