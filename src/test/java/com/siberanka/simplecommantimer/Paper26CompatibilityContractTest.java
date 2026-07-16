package com.siberanka.simplecommantimer;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class Paper26CompatibilityContractTest {
    @Test
    void paper26SchedulerSignaturesMatchReflectionBridge() throws Exception {
        Class<?> globalScheduler = optionalClass(
                "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
        assumeTrue(globalScheduler != null, "Legacy API build does not expose Paper region schedulers");

        Class<?> entityScheduler = Class.forName(
                "io.papermc.paper.threadedregions.scheduler.EntityScheduler");
        Class<?> entity = Class.forName("org.bukkit.entity.Entity");

        Method globalExecute = globalScheduler.getMethod("execute", Plugin.class, Runnable.class);
        Method entityGetter = entity.getMethod("getScheduler");
        Method entityExecute = entityScheduler.getMethod("execute", Plugin.class, Runnable.class,
                Runnable.class, long.class);

        assertNotNull(globalExecute);
        assertEquals(entityScheduler, entityGetter.getReturnType());
        assertEquals(boolean.class, entityExecute.getReturnType());
    }

    private Class<?> optionalClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
}
