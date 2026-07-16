package com.siberanka.simplecommantimer;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceConfigurationTest {
    @Test
    void bundledYamlFilesLoadAndContainReleaseSettings() {
        YamlConfiguration plugin = load("plugin.yml");
        YamlConfiguration config = load("config.yml");

        assertEquals("1.2.0", plugin.getString("version"));
        assertTrue(plugin.contains("permissions.sctimer.admin"));
        assertEquals("siberanka/SimpleCommandTimer-remake",
                config.getString("Update_Check.repository"));
        assertNotNull(config.getString("Lang.Update_available"));
        assertNotNull(config.getString("Lang.Trigger_rate_limited"));
    }

    @Test
    void integrityCheckRepairsMalformedSecuritySettings() {
        YamlConfiguration config = load("config.yml");
        config.set("Update_Check.enabled", "not-a-boolean");
        config.set("Update_Check.check-interval-hours", "many");
        config.set("Lang.Update_available", Boolean.TRUE);

        assertTrue(ConfigIntegrityService.ensure(config));
        assertTrue(config.isBoolean("Update_Check.enabled"));
        assertTrue(config.isInt("Update_Check.check-interval-hours"));
        assertTrue(config.isString("Lang.Update_available"));
    }

    private YamlConfiguration load(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull(stream);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
