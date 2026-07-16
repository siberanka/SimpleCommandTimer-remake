package com.siberanka.simplecommantimer;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

public final class ServerDispatcher {
    private final Plugin plugin;
    private final Method getGlobalRegionSchedulerMethod;
    private final Method executeMethod;

    public ServerDispatcher(Plugin plugin) {
        this.plugin = plugin;

        Method schedulerGetter = null;
        Method globalExecute = null;

        try {
            schedulerGetter = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            Object scheduler = schedulerGetter.invoke(Bukkit.getServer());
            if (scheduler != null) {
                globalExecute = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
            }
        } catch (Exception ignored) {
            schedulerGetter = null;
            globalExecute = null;
        }

        this.getGlobalRegionSchedulerMethod = schedulerGetter;
        this.executeMethod = globalExecute;
    }

    public void dispatchCommands(final List<String> commands) {
        runGlobal(new Runnable() {
            @Override
            public void run() {
                ConsoleCommandSender console = Bukkit.getConsoleSender();
                for (String command : commands) {
                    if (command == null || command.trim().isEmpty()) {
                        continue;
                    }
                    Bukkit.dispatchCommand(console, command.trim());
                }
            }
        });
    }

    public void runGlobal(Runnable runnable) {
        if (getGlobalRegionSchedulerMethod != null && executeMethod != null) {
            try {
                Object scheduler = getGlobalRegionSchedulerMethod.invoke(Bukkit.getServer());
                executeMethod.invoke(scheduler, plugin, runnable);
                return;
            } catch (Exception ignored) {
                plugin.getLogger().warning("Folia global scheduler rejected a task; execution was cancelled.");
                return;
            }
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runForPlayer(final Player player, final Runnable runnable) {
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(player);
            Method execute = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class,
                    Runnable.class, long.class);
            execute.invoke(scheduler, plugin, runnable, null, Long.valueOf(1L));
            return;
        } catch (Exception ignored) {
            if (getGlobalRegionSchedulerMethod != null) {
                plugin.getLogger().warning("Folia player scheduler rejected a task; notification was cancelled.");
                return;
            }
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
