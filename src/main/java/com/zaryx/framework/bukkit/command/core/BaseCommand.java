package com.zaryx.framework.bukkit.command.core;

import com.zaryx.framework.bukkit.command.annotation.Aliases;
import com.zaryx.framework.bukkit.command.annotation.Description;
import com.zaryx.framework.bukkit.command.annotation.Info;
import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import com.zaryx.framework.bukkit.command.extra.CommandContext;
import com.zaryx.framework.bukkit.utility.Task;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Getter @Setter
public abstract class BaseCommand extends Command {

    protected String permission;
    protected boolean playerOnly;
    protected boolean consoleOnly;

    private boolean enabled;

    private final List<BaseCommand> subCommands;
    private final List<CommandArgument<?>> commandArguments;
    
    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    protected BaseCommand() {
        super(resolveNameFromAnnotation());
        this.applyAnnotations();

        this.enabled = true;
        this.subCommands = new ArrayList<>();

        List<CommandArgument<?>> rawCommandArguments = this.arguments();
        this.commandArguments = Collections.unmodifiableList(this.sortArguments(rawCommandArguments));
    }

    protected BaseCommand(String name) {
        super(name);

        this.enabled = true;
        this.subCommands = new ArrayList<>();

        List<CommandArgument<?>> rawCommandArguments = this.arguments();
        this.commandArguments = Collections.unmodifiableList(this.sortArguments(rawCommandArguments));
    }

    @Override
    public final boolean execute(CommandSender sender, String label, String[] args) {
        if (!this.enabled) {
            sender.sendMessage("This command is disabled.");
            return true;
        }

        if (this.playerOnly && !(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        if (this.consoleOnly && sender instanceof Player) {
            sender.sendMessage("This command can only be used from console.");
            return true;
        }

        if (this.permission != null && !this.permission.isEmpty()) {
            if (!sender.hasPermission(this.permission)) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
        }

        if (args != null && args.length > 0) {
            BaseCommand subCommand = findSubCommand(args[0]);
            if (subCommand != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.execute(sender, args[0], subArgs);
            }
        }

        try {
            CommandContext commandContext = this.buildContext(sender, args);
            Task.sync(() -> this.execute(commandContext));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(e.getMessage());
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args == null || args.length == 0) {
            return getSubCommandNames("");
        }

        if (args.length == 1) {
            BaseCommand subCommand = findSubCommand(args[0]);
            if (subCommand != null) {
                return subCommand.tabComplete(sender, alias, new String[0]);
            }

            return getSubCommandNames(args[0]);
        }

        BaseCommand subCommand = findSubCommand(args[0]);
        if (subCommand != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.tabComplete(sender, alias, subArgs);
        }

        int index = args.length - 1;

        if (index < 0 || index >= this.commandArguments.size()) {
            return Collections.emptyList();
        }

        CommandArgument<?> commandArgument = this.commandArguments.get(index);
        String input = args[index];

        return commandArgument.tabComplete(sender, input);
    }

    protected CommandContext buildContext(CommandSender sender, String[] inputArgs) {
        if (inputArgs.length > this.commandArguments.size()) {
            throw new IllegalArgumentException("Too many arguments.");
        }

        CommandContext commandContext = new CommandContext(sender);
        for (int i = 0; i < this.commandArguments.size(); i++) {
            CommandArgument<?> commandArgument = this.commandArguments.get(i);

            if (!commandArgument.shouldParse(commandContext)) continue;

            boolean hasInput = i < inputArgs.length;
            if (!hasInput) {
                if (!commandArgument.isRequired(commandContext)) {
                    commandContext.put(commandArgument.getName(), commandArgument.getDefaultValue());
                    continue;
                }

                throw new IllegalArgumentException("Missing required argument: " + commandArgument.getName());
            }

            String input = inputArgs[i];

            if (!commandArgument.validate(input)) {
                throw new IllegalArgumentException("Invalid argument: '" + commandArgument.getName() + "'");
            }

            commandContext.put(commandArgument.getName(), commandArgument.parse(input));
        }

        return commandContext;
    }

    protected List<CommandArgument<?>> arguments() {
        return Collections.emptyList();
    }

    public void addSubCommand(BaseCommand subCommand) {
        if (subCommand != null) {
            this.subCommands.add(subCommand);
        }
    }

    public abstract void execute(CommandContext commandContext);

    private void applyAnnotations() {
        Class<?> clazz = getClass();

        if (clazz.isAnnotationPresent(Info.class)) {
            Info info = clazz.getAnnotation(Info.class);
            this.permission = info.permission();
            this.playerOnly = info.playerOnly();
            this.consoleOnly = info.consoleOnly();
        }

        if (clazz.isAnnotationPresent(Aliases.class)) {
            this.setAliases(Arrays.asList(
                    clazz.getAnnotation(Aliases.class).value()
            ));
        }

        if (clazz.isAnnotationPresent(Description.class)) {
            this.description = clazz.getAnnotation(Description.class).value();
            this.setDescription(description);
        }
    }

    private List<CommandArgument<?>> sortArguments(List<CommandArgument<?>> original) {
        List<CommandArgument<?>> sorted = new ArrayList<>(original);
        sorted.sort((a, b) -> {
            int priority = Integer.compare(a.getPriority(), b.getPriority());
            if (priority != 0) {
                return priority;
            }

            if (a.isOptional() != b.isOptional()) {
                return a.isOptional() ? 1 : -1;
            }

            return 0;
        });

        return sorted;
    }

    private static String resolveNameFromAnnotation() {

        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        try {
            for (StackTraceElement element : stack) {
                Class<?> clazz = Class.forName(element.getClassName());
                if (BaseCommand.class.isAssignableFrom(clazz)
                        && clazz.isAnnotationPresent(Info.class)) {
                    return clazz.getAnnotation(Info.class).name();
                }
            }
        } catch (ClassNotFoundException ignored) {}

        throw new IllegalStateException("Could not resolve command name. Use @Info or the classic constructor.");
    }

    private BaseCommand findSubCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        for (BaseCommand subCommand : this.subCommands) {
            if (subCommand == null) {
                continue;
            }

            if (subCommand.getName() != null && subCommand.getName().toLowerCase(Locale.ROOT).equals(normalized)) {
                return subCommand;
            }

            for (String alias : subCommand.getAliases()) {
                if (alias != null && alias.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return subCommand;
                }
            }
        }

        return null;
    }

    private List<String> getSubCommandNames(String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();

        for (BaseCommand subCommand : this.subCommands) {
            if (subCommand == null || subCommand.getName() == null) {
                continue;
            }

            String subName = subCommand.getName();
            if (normalizedPrefix.isEmpty() || subName.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                names.add(subName);
            }

            for (String alias : subCommand.getAliases()) {
                if (alias != null && (normalizedPrefix.isEmpty() || alias.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))) {
                    names.add(alias);
                }
            }
        }

        return names;
    }
}
