package dev.astanix.esp.command;

import dev.astanix.esp.AstanixESPPlugin;
import dev.astanix.esp.config.PluginConfig;
import dev.astanix.esp.manager.StorageManager;
import dev.astanix.esp.model.PlayerState;
import dev.astanix.esp.tracker.PlayerTrackerService;
import dev.astanix.esp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
 * Administrative command executor and tab completer for Astanix ESP (/esp, /astanixesp).
 */
public class EspCommand implements CommandExecutor, TabCompleter {
    private final AstanixESPPlugin plugin;
    private final PluginConfig config;
    private final StorageManager storageManager;
    private final PlayerTrackerService trackerService;

    public EspCommand(AstanixESPPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.storageManager = plugin.getStorageManager();
        this.trackerService = plugin.getTrackerService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("astanix.esp.admin")) {
            sender.sendMessage(TextUtil.parse("<red>У вас нет прав для выполнения этой команды!</red>"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "status", "info" -> handleStatus(sender);
            case "bypass" -> handleBypass(sender);
            case "debug" -> handleDebug(sender);
            default -> {
                sender.sendMessage(TextUtil.parse("<red>Неизвестная подкоманда! Используйте <yellow>/" + label + " help</yellow></red>"));
            }
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━━━━━ [ Astanix ESP ] ━━━━━━━━━━━━━</bold></gold>"));
        sender.sendMessage(TextUtil.parse("<yellow>/" + label + " status</yellow> <gray>- Статистика и мониторинг кэша</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/" + label + " reload</yellow> <gray>- Перезагрузить конфигурацию и обновить кэш</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/" + label + " bypass</yellow> <gray>- Переключить режим обхода маскировки</gray>"));
        sender.sendMessage(TextUtil.parse("<yellow>/" + label + " debug</yellow> <gray>- Включить/выключить логирование отладки</gray>"));
        sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gold>"));
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(TextUtil.parse("<yellow>Перезагрузка конфигурации Astanix ESP...</yellow>"));
        plugin.reloadConfig();
        config.load(plugin.getConfig());

        // Re-index all loaded chunks in all worlds
        storageManager.clearAll();
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                storageManager.loadChunk(chunk);
            }
        }

        // Refresh all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            trackerService.checkPlayer(player, true);
        }

        sender.sendMessage(TextUtil.parse("<green>Конфигурация успешно перезагружена! Проиндексировано " +
                storageManager.getTotalBlockCount() + " хранилищ в " + storageManager.getTotalChunkCount() + " чанках.</green>"));
    }

    private void handleStatus(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemMb = runtime.maxMemory() / (1024 * 1024);

        sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━ [ Astanix ESP - Статус ] ━━━━━━━━━</bold></gold>"));
        sender.sendMessage(TextUtil.parse("<gray>Дистанция скрытия: <yellow>" + config.getHideDistance() + " блоков</yellow></gray>"));
        sender.sendMessage(TextUtil.parse("<gray>Интервал проверки: <yellow>" + config.getCheckIntervalTicks() + " тиков</yellow></gray>"));
        sender.sendMessage(TextUtil.parse("<gray>Отслеживаемых чанков: <green>" + storageManager.getTotalChunkCount() + "</green></gray>"));
        sender.sendMessage(TextUtil.parse("<gray>Отслеживаемых блоков: <green>" + storageManager.getTotalBlockCount() + "</green></gray>"));
        sender.sendMessage(TextUtil.parse("<gray>Отслеживаемых сущностей: <green>" + storageManager.getTotalEntityCount() + "</green></gray>"));
        sender.sendMessage(TextUtil.parse("<gray>Игроков онлайн: <aqua>" + Bukkit.getOnlinePlayers().size() + "</aqua></gray>"));
        sender.sendMessage(TextUtil.parse("<gray>Типы блоков: <yellow>" + config.getTargetBlocks().size() + "</yellow>, Сущностей: <yellow>" + config.getTargetEntities().size() + "</yellow></gray>"));
        sender.sendMessage(TextUtil.parse("<gray>Использование памяти JVM: <yellow>" + usedMemMb + " MB / " + maxMemMb + " MB</yellow></gray>"));
        sender.sendMessage(TextUtil.parse("<gold><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gold>"));
    }

    private void handleBypass(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse("<red>Эта команда доступна только игрокам!</red>"));
            return;
        }

        if (!player.hasPermission("astanix.esp.bypass")) {
            player.sendMessage(TextUtil.parse("<red>У вас нет разрешения <yellow>astanix.esp.bypass</yellow>!</red>"));
            return;
        }

        PlayerState state = trackerService.getPlayerState(player);
        boolean newBypass = !state.isBypass();
        state.setBypass(newBypass);

        if (newBypass) {
            trackerService.restorePlayer(player);
            player.sendMessage(TextUtil.parse("<green>Режим обхода маскировки включен. Теперь вы видите все хранилища.</green>"));
        } else {
            trackerService.checkPlayer(player, true);
            player.sendMessage(TextUtil.parse("<yellow>Режим обхода маскировки выключен. Хранилища на дистанции снова маскируются.</yellow>"));
        }
    }

    private void handleDebug(CommandSender sender) {
        boolean newDebug = !config.isDebug();
        config.setDebug(newDebug);
        sender.sendMessage(TextUtil.parse("<gray>Режим отладки: " + (newDebug ? "<green>Включен</green>" : "<red>Выключен</red>") + "</gray>"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("astanix.esp.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("status", "reload", "bypass", "debug", "help"));
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(input));
            return completions;
        }

        return Collections.emptyList();
    }
}
