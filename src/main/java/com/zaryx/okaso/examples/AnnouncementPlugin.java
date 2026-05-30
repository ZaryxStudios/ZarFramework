package com.zaryx.okaso.examples;

import com.zaryx.okaso.api.OkasoConfig;
import com.zaryx.okaso.bukkit.adapter.PluginOkasoAdapter;
import com.zaryx.okaso.core.event.EventBus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementPlugin extends JavaPlugin implements CommandExecutor {

    private PluginOkasoAdapter okaso;
    private EventBus eventBus;
    private List<String> announcements;

    @Override
    public void onEnable() {
        try {

            OkasoConfig config = new OkasoConfig()
                .setCacheTTL(1800000)
                    .setCacheMaxSize(500)
                    .setThreadPoolSize(2)
                    .setDebugMode(false);

            okaso = new PluginOkasoAdapter(this)
                    .withConfig(config)
                    .initialize();

            if (!okaso.isRunning()) {
                getLogger().severe("✗ Okaso failed to initialize");
                setEnabled(false);
                return;
            }

            eventBus = okaso.getEventBus();
            announcements = new ArrayList<>();

            getCommand("announce").setExecutor(this);

            startAnnouncementTask();

            getLogger().info("✓ AnnouncementPlugin initialized");

        } catch (Exception e) {
            getLogger().severe("✗ Error: " + e.getMessage());
            e.printStackTrace();
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (okaso != null) {
            okaso.shutdown();
            getLogger().info("✓ AnnouncementPlugin disabled");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("announce.use")) {
            sender.sendMessage("§cYou do not have permission");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("announce")) {
            if (args.length == 0) {
                showStats(sender);
                return true;
            }

            String message = String.join(" ", args);
            announcements.add(message);

            AnnouncementEvent event = new AnnouncementEvent(message);
            eventBus.publish(event);

            sender.sendMessage("§a✓ Announcement added");
            return true;
        }

        return false;
    }

    private void startAnnouncementTask() {
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!announcements.isEmpty()) {
                String announcement = announcements.get(0);
                getServer().broadcastMessage("§b[ANNOUNCEMENT] " + announcement);
                announcements.remove(0);
            }
        }, 0, 1200);
    }

    private void showStats(CommandSender sender) {
        if (okaso.getOkaso() == null) {
            sender.sendMessage("§cFramework not available");
            return;
        }

        String stats = okaso.getStatsString();
        sender.sendMessage("§b--- Okaso Statistics ---");
        sender.sendMessage("§e" + stats);
        sender.sendMessage("§b--- ---");
    }

    public static class AnnouncementEvent extends EventBus.FrameworkEvent {
        private final String message;

        public AnnouncementEvent(String message) {
            super("AnnouncementPlugin");
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String getEventName() {
            return "AnnouncementEvent";
        }
    }
}
