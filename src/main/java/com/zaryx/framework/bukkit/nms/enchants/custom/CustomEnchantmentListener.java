package com.zaryx.framework.bukkit.nms.enchants.custom;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public class CustomEnchantmentListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Player sourcePlayer = null;

        if (damager instanceof Player) {
            sourcePlayer = (Player) damager;
        }

        if (sourcePlayer != null) {
            ItemStack item = getMainHand(sourcePlayer);
            applyAttackEffects(sourcePlayer, item, event);
        }

        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            applyDefendEffects(target, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack chestItem = player.getInventory().getChestplate();
        Map<CustomEnchantment, Integer> enchantments = CustomEnchantmentHelper.getEnchantments(chestItem);
        if (enchantments.isEmpty()) {
            return;
        }

        for (Map.Entry<CustomEnchantment, Integer> entry : enchantments.entrySet()) {
            CustomEnchantment enchantment = entry.getKey();
            if (enchantment == null) {
                continue;
            }

            CustomEnchantmentEffectContext context = new CustomEnchantmentEffectContext(enchantment, entry.getValue(), player, null, chestItem, event);
            enchantment.fireHooks(context);
            context.applyMovementModifiers();
        }
    }

    private void applyAttackEffects(Player player, ItemStack item, EntityDamageByEntityEvent event) {
        Map<CustomEnchantment, Integer> enchantments = CustomEnchantmentHelper.getEnchantments(item);
        if (enchantments.isEmpty()) {
            return;
        }

        double damage = event.getDamage();
        for (Map.Entry<CustomEnchantment, Integer> entry : enchantments.entrySet()) {
            CustomEnchantment enchantment = entry.getKey();
            if (enchantment == null) {
                continue;
            }

            CustomEnchantmentEffectContext context = new CustomEnchantmentEffectContext(enchantment, entry.getValue(), player, event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null, item, event);
            enchantment.fireHooks(context);
            damage = (damage + context.getDamageBonus()) * context.getDamageMultiplier();
        }

        event.setDamage(Math.max(0.0D, damage));
    }

    private void applyDefendEffects(LivingEntity target, EntityDamageByEntityEvent event) {
        if (!(target instanceof Player)) {
            return;
        }

        Player player = (Player) target;
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armor = inventory.getArmorContents();
        if (armor == null || armor.length == 0) {
            return;
        }

        double damage = event.getDamage();
        for (ItemStack piece : armor) {
            Map<CustomEnchantment, Integer> enchantments = CustomEnchantmentHelper.getEnchantments(piece);
            if (enchantments.isEmpty()) {
                continue;
            }

            for (Map.Entry<CustomEnchantment, Integer> entry : enchantments.entrySet()) {
                CustomEnchantment enchantment = entry.getKey();
                if (enchantment == null) {
                    continue;
                }

                CustomEnchantmentEffectContext context = new CustomEnchantmentEffectContext(enchantment, entry.getValue(), player, event.getDamager() instanceof LivingEntity ? (LivingEntity) event.getDamager() : null, piece, event);
                enchantment.fireHooks(context);
                damage = (damage + context.getReceivedDamageBonus()) * context.getReceivedDamageMultiplier();
            }
        }

        event.setDamage(Math.max(0.0D, damage));
    }

    private ItemStack getMainHand(Player player) {
        try {
            java.lang.reflect.Method method = player.getInventory().getClass().getMethod("getItemInMainHand");
            Object value = method.invoke(player.getInventory());
            if (value instanceof ItemStack) {
                return (ItemStack) value;
            }
        } catch (Throwable ignored) {
        }

        return player.getItemInHand();
    }
}
