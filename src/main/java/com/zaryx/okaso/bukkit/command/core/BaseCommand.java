package com.zaryx.okaso.bukkit.command.core;

import com.zaryx.okaso.bukkit.command.annotation.Aliases;
import com.zaryx.okaso.bukkit.command.annotation.Description;
import com.zaryx.okaso.bukkit.command.annotation.Info;
import com.zaryx.okaso.bukkit.command.extra.CommandArgument;
import com.zaryx.okaso.bukkit.command.extra.CommandContext;
import com.zaryx.okaso.bukkit.utility.Task;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseCommand extends Command {

    private static final Logger LOGGER = Logger.getLogger(BaseCommand.class.getName());

    protected String permission;
    protected boolean playerOnly;
    protected boolean consoleOnly;

    private volatile boolean enabled;
    private final List<BaseCommand> subCommands;
    private final List<CommandArgument<?>> commandArguments;

    protected BaseCommand() {
        super(resolveNameFromAnnotation());
        this.enabled = true;
        this.subCommands = new ArrayList<>();
        this.commandArguments = Collections.unmodifiableList(sortArguments(this.arguments()));
        applyAnnotations();
    }

    protected BaseCommand(String name) {
        super(name);
        this.enabled = true;
        this.subCommands = new ArrayList<>();
        this.commandArguments = Collections.unmodifiableList(sortArguments(this.arguments()));
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public boolean isPlayerOnly() { return playerOnly; }
    public void setPlayerOnly(boolean playerOnly) { this.playerOnly = playerOnly; }

    public boolean isConsoleOnly() { return consoleOnly; }
    public void setConsoleOnly(boolean consoleOnly) { this.consoleOnly = consoleOnly; }

    public List<BaseCommand> getSubCommands() { return Collections.unmodifiableList(subCommands); }
    public List<CommandArgument<?>> getCommandArguments() { return commandArguments; }

    @Override
    public final boolean execute(CommandSender sender, String label, String[] args) {
        if (!canExecute(sender)) return true;

        BaseCommand sub = findSubCommandFor(args);
        if (sub != null) {
            String[] subArgs = (args != null && args.length > 1)
                    ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
            String subLabel = (args != null && args.length > 0) ? args[0] : label;
            return sub.execute(sender, subLabel, subArgs);
        }

        try {
            CommandContext ctx = buildContext(sender, args);
            Task.sync(() -> {
                try {
                    execute(ctx);
                } catch (Exception e) {
                    sender.sendMessage("§cAn internal error occurred while executing the command.");
                    LOGGER.log(Level.WARNING, "Error executing command: " + getName(), e);
                }
            });
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c" + e.getMessage());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args == null || args.length == 0) {
            return getSubCommandNames("");
        }

        BaseCommand sub = findSubCommandFor(args);
        if (sub != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return sub.tabComplete(sender, alias, subArgs);
        }

        if (args.length == 1) {
            return getSubCommandNames(args[0]);
        }

        int idx = args.length - 1;
        if (idx < 0 || idx >= commandArguments.size()) {
            return Collections.emptyList();
        }

        CommandArgument<?> arg = commandArguments.get(idx);
        String input = args[idx] != null ? args[idx] : "";
        try {
            return arg.tabComplete(sender, input);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Tab completion error for argument '" + arg.getName() + "'", e);
            return Collections.emptyList();
        }
    }

    public abstract void execute(CommandContext ctx);

    protected CommandContext buildContext(CommandSender sender, String[] inputArgs) {
        String[] args = (inputArgs != null) ? inputArgs : new String[0];

        if (args.length > commandArguments.size()) {
            throw new IllegalArgumentException("Too many arguments.");
        }

        CommandContext ctx = new CommandContext(sender);
        for (int i = 0; i < commandArguments.size(); i++) {
            CommandArgument<?> arg = commandArguments.get(i);

            if (!arg.shouldParse(ctx)) continue;

            if (i >= args.length) {
                if (arg.isRequired(ctx)) {
                    throw new IllegalArgumentException("Missing required argument: " + arg.getName());
                }
                ctx.put(arg.getName(), arg.getDefaultValue());
                continue;
            }

            String input = args[i];
            if (!arg.validate(input)) {
                throw new IllegalArgumentException("Invalid argument: '" + arg.getName() + "'");
            }
            ctx.put(arg.getName(), arg.parse(input));
        }
        return ctx;
    }

    protected List<CommandArgument<?>> arguments() {
        return Collections.emptyList();
    }

    public void addSubCommand(BaseCommand sub) {
        if (sub != null) subCommands.add(sub);
    }

    public BaseCommand findSubCommand(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String key = input.trim().toLowerCase(Locale.ROOT);
        for (BaseCommand sub : subCommands) {
            if (sub == null) continue;
            if (sub.getName() != null && sub.getName().toLowerCase(Locale.ROOT).equals(key)) return sub;
            for (String alias : sub.getAliases()) {
                if (alias != null && alias.toLowerCase(Locale.ROOT).equals(key)) return sub;
            }
        }
        return null;
    }

    public List<String> getSubCommandNames(String prefix) {
        String pfx = (prefix != null) ? prefix.trim().toLowerCase(Locale.ROOT) : "";
        List<String> names = new ArrayList<>();
        for (BaseCommand sub : subCommands) {
            if (sub == null || sub.getName() == null) continue;
            String n = sub.getName();
            if (pfx.isEmpty() || n.toLowerCase(Locale.ROOT).startsWith(pfx)) names.add(n);
            for (String alias : sub.getAliases()) {
                if (alias != null && (pfx.isEmpty() || alias.toLowerCase(Locale.ROOT).startsWith(pfx))) names.add(alias);
            }
        }
        return names;
    }

    private boolean canExecute(CommandSender sender) {
        if (!enabled) {
            sender.sendMessage("§cThis command is currently disabled.");
            return false;
        }
        if (playerOnly && !(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return false;
        }
        if (consoleOnly && sender instanceof Player) {
            sender.sendMessage("§cThis command can only be used from the console.");
            return false;
        }
        if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return false;
        }
        return true;
    }

    private BaseCommand findSubCommandFor(String[] args) {
        if (args == null || args.length == 0) return null;
        return findSubCommand(args[0]);
    }

    private void applyAnnotations() {
        Class<?> clz = getClass();
        if (clz.isAnnotationPresent(Info.class)) {
            Info info = clz.getAnnotation(Info.class);
            this.permission = info.permission();
            this.playerOnly = info.playerOnly();
            this.consoleOnly = info.consoleOnly();
        }
        if (clz.isAnnotationPresent(Aliases.class)) {
            setAliases(Arrays.asList(clz.getAnnotation(Aliases.class).value()));
        }
        if (clz.isAnnotationPresent(Description.class)) {
            this.description = clz.getAnnotation(Description.class).value();
            setDescription(this.description);
        }
    }

    private static List<CommandArgument<?>> sortArguments(List<CommandArgument<?>> original) {
        List<CommandArgument<?>> sorted = new ArrayList<>(original);
        sorted.sort((a, b) -> {
            int p = Integer.compare(a.getPriority(), b.getPriority());
            if (p != 0) return p;
            if (a.isOptional() != b.isOptional()) return a.isOptional() ? 1 : -1;
            return 0;
        });
        return sorted;
    }

    private static String resolveNameFromAnnotation() {
        for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
            try {
                Class<?> clz = Class.forName(el.getClassName());
                if (BaseCommand.class.isAssignableFrom(clz) && clz.isAnnotationPresent(Info.class)) {
                    return clz.getAnnotation(Info.class).name();
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.FINE, "Could not resolve class from stack trace: " + el.getClassName(), e);
            }
        }
        throw new IllegalStateException("Could not resolve command name. Use @Info or the classic constructor.");
    }
}
