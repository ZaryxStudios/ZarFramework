package com.zaryx.framework.bukkit.utility;

import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.Thread.UncaughtExceptionHandler;


public final class Task {

    private static JavaPlugin plugin;

    private Task() {}

    public static void setPlugin(JavaPlugin plugin) {
        Task.plugin = plugin;
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static ThreadFactory newThreadFactory(String name) {
        return new ThreadFactoryBuilder().setNameFormat(name).build();
    }

    public static ThreadFactory newThreadFactory(String name, UncaughtExceptionHandler handler) {
        return new ThreadFactoryBuilder().setNameFormat(name).setUncaughtExceptionHandler(handler).build();
    }

    public static void sync(Callable callable, JavaPlugin plugin) {
        Bukkit.getScheduler().runTask(plugin, callable::call);
    }

    public static void sync(Callable callable) {
        if (plugin == null) {
            callable.call();
            return;
        }
        sync(callable, plugin);
    }

    public static BukkitTask syncLater(Callable callable, JavaPlugin plugin, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, callable::call, delay);
    }

    public static BukkitTask syncLater(Callable callable, long delay) {
        if (plugin == null) {
            callable.call();
            return null;
        }
        return syncLater(callable, plugin, delay);
    }

    public static BukkitTask syncTimer(Callable callable, JavaPlugin plugin, long delay, long value) {
        return Bukkit.getScheduler().runTaskTimer(plugin, callable::call, delay, value);
    }

    public static BukkitTask syncTimer(Callable callable, long delay, long value) {
        if (plugin == null) {
            callable.call();
            return null;
        }
        return syncTimer(callable, plugin, delay, value);
    }

    public static void async(Callable callable, JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, callable::call);
    }

    public static void async(Callable callable) {
        if (plugin == null) {
            callable.call();
            return;
        }
        async(callable, plugin);
    }

    public static BukkitTask asyncLater(Callable callable, JavaPlugin plugin, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, callable::call, delay);
    }

    public static BukkitTask asyncTimer(Callable callable, JavaPlugin plugin, long delay, long value) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, callable::call, delay, value);
    }

    public static void asyncDelayed(Callable callable, JavaPlugin plugin, long delay) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, callable::call, delay);
    }

    public interface Callable {
        void call();
    }
}
