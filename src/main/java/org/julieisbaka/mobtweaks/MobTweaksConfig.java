package org.julieisbaka.mobtweaks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class MobTweaksConfig {
    private static final String FILE_NAME = "mob-tweaks.properties";
    private static final Map<MobTweak, Boolean> ENABLED = new EnumMap<>(MobTweak.class);

    static {
        for (MobTweak tweak : MobTweak.values()) {
            ENABLED.put(tweak, true);
        }
    }

    private MobTweaksConfig() {
    }

    public static boolean isEnabled(MobTweak tweak) {
        return ENABLED.get(tweak);
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties properties = new Properties();
        boolean needsSave = !Files.exists(path);
        try {
            if (!needsSave) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    properties.load(reader);
                }
            }

            for (MobTweak tweak : MobTweak.values()) {
                String value = properties.getProperty(tweak.key());
                if (value == null) {
                    value = "true";
                    properties.setProperty(tweak.key(), value);
                    needsSave = true;
                }
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new IllegalStateException(
                            "Invalid boolean for " + tweak.key() + " in " + path + ": " + value);
                }
                ENABLED.put(tweak, Boolean.parseBoolean(value));
            }

            if (needsSave) {
                Files.createDirectories(path.getParent());
                try (Writer writer = Files.newBufferedWriter(path)) {
                    properties.store(writer, "Set an individual tweak to false to disable it.");
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Mob Tweaks settings from " + path, exception);
        }
    }
}
