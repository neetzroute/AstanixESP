package dev.astanix.esp.command;

import dev.astanix.esp.AstanixESPPlugin;
import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.freecam.PlayerProfile;
import dev.astanix.esp.freecam.ViolationManager;
import dev.astanix.esp.freecam.WallOcclusionService;
import dev.astanix.esp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command executor and tab completer for /afc (Astanix FreeCam).
 * Silent operation mode: no alert broadcasts, handles bypass and status inspection.
 */
public class FreeCamCommand implements CommandExecutor, TabCompleter {
    private final AstanixESPPlugin plugin;
    private final PluginConfig config;
    private final ViolationManager violationManager;

    public FreeCamCommand(AstanixESPPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.violationManager = plugin.getViolationManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "bypass" -> handleBypass(sender);
            case "info", "status" -> handleInfo(sender, args);
            default -> sender.sendMessage(TextUtil.parse(config.getPrefix() + "<red>Неизвестная подкоманда! Используйте <yellow>/" + label + " help</yellow></red>"));
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━━━━━ [ Astanix-FC ] ━━━━━━━━━━━━━</bold></gold>"));
        sender.sendMessage(TextUtil.parse("<yellow>/" + label + " bypass</yellow> <gray>- Включить/выключить режим обхода скрытия для себя</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/" + label + " info [игрок]</yellow> <gray>- Просмотр текущего статуса скрытия контейнеров</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/" + label + " reload</yellow> <gray>- Перезагрузить конфигурацию config.yml</gray>"));
        sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gold>"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("astanix.freecam.admin")) {
            sender.sendMessage(TextUtil.parse(config.getPrefix() + config.getMsgNoPermission()));
            return;
        }

        plugin.reloadConfig();
        config.load(plugin.getConfig());
        sender.sendMessage(TextUtil.parse(config.getPrefix() + config.getMsgReloaded()));
    }

    private void handleBypass(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse(config.getPrefix() + "<red>Эта команда доступна только игрокам!</red>"));
            return;
        }

        if (!player.hasPermission("astanix.freecam.bypass") && !player.hasPermission("astanix.freecam.admin") && !player.isOp()) {
            player.sendMessage(TextUtil.parse(config.getPrefix() + config.getMsgNoPermission()));
            return;
        }

        PlayerProfile profile = violationManager.getProfile(player);
        boolean newBypass = !profile.isBypass();
        profile.setBypass(newBypass);

        if (newBypass) {
            player.sendMessage(TextUtil.parse(config.getPrefix() + config.getMsgBypassEnabled()));
        } else {
            player.sendMessage(TextUtil.parse(config.getPrefix() + config.getMsgBypassDisabled()));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("astanix.freecam.admin") && !sender.isOp()) {
            sender.sendMessage(TextUtil.parse(config.getPrefix() + config.getMsgNoPermission()));
            return;
        }

        WallOcclusionService wallOcclusion = plugin.getWallOcclusionService();

        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(TextUtil.parse(config.getPrefix() + config.getMsgPlayerNotFound()));
                return;
            }

            PlayerProfile profile = violationManager.getProfile(target);
            int hiddenCount = (wallOcclusion != null) ? wallOcclusion.getHiddenCount(target) : 0;

            sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━ [ Статус игрока: " + target.getName() + " ] ━━━━━━━━</bold></gold>"));
            sender.sendMessage(TextUtil.parse("<gray>Скрыто контейнеров сквозь стены: <yellow>" + hiddenCount + "</yellow></gray>"));
            sender.sendMessage(TextUtil.parse("<gray>Игровой режим: <yellow>" + target.getGameMode().name() + "</yellow></gray>"));
            sender.sendMessage(TextUtil.parse("<gray>Bypass режим: " + (profile.isBypass() ? "<green>Включен</green>" : "<red>Выключен (проверки активны)</red>") + "</gray>"));
            sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gold>"));
        } else {
            sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━ [ Astanix-FC - Статус ] ━━━━━━━━━</bold></gold>"));
            sender.sendMessage(TextUtil.parse("<gray>Reach проверка: " + (config.isFreeCamReachEnabled() ? "<green>Включена</green>" : "<red>Выключена</red>") + " (Выживание: <yellow>" + config.getFreeCamMaxReach(org.bukkit.GameMode.SURVIVAL) + "б</yellow>)</gray>"));
            sender.sendMessage(TextUtil.parse("<gray>Raytrace проверка: " + (config.isFreeCamRaytraceEnabled() ? "<green>Включена</green>" : "<red>Выключена</red>") + "</gray>"));
            sender.sendMessage(TextUtil.parse("<gray>Скрытие сундуков сквозь стены: <green>Включено</green></gray>"));
            sender.sendMessage(TextUtil.parse("<gray>Защита инвентарей (Open): <green>Включена</green></gray>"));
            sender.sendMessage(TextUtil.parse("<gray>Режим работы: <aqua>Бесшумный (без алертов в чат)</aqua></gray>"));
            sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gold>"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("astanix.freecam.admin") && !sender.hasPermission("astanix.freecam.bypass") && !sender.isOp()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("info", "bypass", "reload", "help"));
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
