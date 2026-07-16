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
    private final Method globalExecuteMethod;
    private final Method getEntitySchedulerMethod;
    private final Method entityExecuteMethod;

    public ServerDispatcher(Plugin plugin) {
        this.plugin = plugin;

        Method schedulerGetter = null;
        Method globalExecute = null;
        Method entitySchedulerGetter = null;
        Method entityExecute = null;

        try {
            schedulerGetter = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            ClassLoader apiClassLoader = Bukkit.class.getClassLoader();
            Class<?> globalSchedulerType = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", false, apiClassLoader);
            globalExecute = globalSchedulerType.getMethod("execute", Plugin.class, Runnable.class);
        } catch (Exception ignored) {
            schedulerGetter = null;
            globalExecute = null;
        }

        try {
            ClassLoader apiClassLoader = Bukkit.class.getClassLoader();
            Class<?> entityType = Class.forName("org.bukkit.entity.Entity", false, apiClassLoader);
            Class<?> entitySchedulerType = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.EntityScheduler", false, apiClassLoader);
            entitySchedulerGetter = entityType.getMethod("getScheduler");
            entityExecute = entitySchedulerType.getMethod("execute", Plugin.class, Runnable.class,
                    Runnable.class, long.class);
        } catch (Exception ignored) {
            entitySchedulerGetter = null;
            entityExecute = null;
        }

        this.getGlobalRegionSchedulerMethod = schedulerGetter;
        this.globalExecuteMethod = globalExecute;
        this.getEntitySchedulerMethod = entitySchedulerGetter;
        this.entityExecuteMethod = entityExecute;
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
        if (getGlobalRegionSchedulerMethod != null && globalExecuteMethod != null) {
            try {
                Object scheduler = getGlobalRegionSchedulerMethod.invoke(Bukkit.getServer());
                globalExecuteMethod.invoke(scheduler, plugin, runnable);
                return;
            } catch (Exception ignored) {
                plugin.getLogger().warning("Folia global scheduler rejected a task; execution was cancelled.");
                return;
            }
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runForPlayer(final Player player, final Runnable runnable) {
        if (getEntitySchedulerMethod != null && entityExecuteMethod != null) {
            try {
                Object scheduler = getEntitySchedulerMethod.invoke(player);
                Object scheduled = entityExecuteMethod.invoke(scheduler, plugin, runnable, null, Long.valueOf(1L));
                if (Boolean.FALSE.equals(scheduled)) {
                    plugin.getLogger().warning("Folia player scheduler retired before notification; task was cancelled.");
                }
                return;
            } catch (Exception ignored) {
                plugin.getLogger().warning("Folia player scheduler rejected a task; notification was cancelled.");
                return;
            }
        }

        if (getGlobalRegionSchedulerMethod != null) {
            plugin.getLogger().warning("Folia entity scheduler is unavailable; notification was cancelled.");
            return;
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
