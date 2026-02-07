package com.simplecommandtimer;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
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
        runOnMain(new Runnable() {
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

    private void runOnMain(Runnable runnable) {
        if (getGlobalRegionSchedulerMethod != null && executeMethod != null) {
            try {
                Object scheduler = getGlobalRegionSchedulerMethod.invoke(Bukkit.getServer());
                executeMethod.invoke(scheduler, plugin, runnable);
                return;
            } catch (Exception ignored) {
            }
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
