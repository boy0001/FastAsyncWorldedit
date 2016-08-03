package com.boydti.fawe.config;

import com.boydti.fawe.configuration.ConfigurationSection;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.sk89q.minecraft.util.commands.Command;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Commands {

    private static YamlConfiguration cmdConfig;
    private static File cmdFile;

    public static void load(File file) {
        cmdFile = file;
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            cmdConfig = YamlConfiguration.loadConfiguration(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Command translate(final Command command) {
        if (cmdConfig == null) {
            return command;
        }

        String id = command.aliases()[0];
        ConfigurationSection commands = cmdConfig.getConfigurationSection(id);
        boolean set = false;
        if (commands == null) {
            set = (commands = cmdConfig.createSection(id)) != null;
        }

        HashMap<String, Object> options = new HashMap<>();
        options.put("aliases", new ArrayList<String>(Arrays.asList(command.aliases())));
        options.put("usage", command.usage());
        options.put("desc", command.desc());
        options.put("help", command.help());

        for (Map.Entry<String, Object> entry : options.entrySet()) {
            String key = entry.getKey();
            if (!commands.contains(key)) {
                commands.set(key, entry.getValue());
                set = true;
            }
        }
        if (set) {
            try {
                cmdConfig.save(cmdFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final String[] aliases = commands.getStringList("aliases").toArray(new String[0]);
        final String usage = commands.getString("usage");
        final String desc = commands.getString("desc");
        final String help = commands.getString("help");

        return new Command() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return command.annotationType();
            }

            @Override
            public String[] aliases() {
                return aliases;
            }

            @Override
            public String usage() {
                return usage;
            }

            @Override
            public String desc() {
                return desc;
            }

            @Override
            public int min() {
                return command.min();
            }

            @Override
            public int max() {
                return command.max();
            }

            @Override
            public String flags() {
                return command.flags();
            }

            @Override
            public String help() {
                return help;
            }

            @Override
            public boolean anyFlags() {
                return command.anyFlags();
            }
        };
    }
}
