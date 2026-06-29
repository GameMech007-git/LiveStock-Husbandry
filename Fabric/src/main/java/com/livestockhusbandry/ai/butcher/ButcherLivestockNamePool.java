package com.livestockhusbandry.ai.butcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ButcherLivestockNamePool {

    private static final Identifier NAME_FILE =
            Identifier.fromNamespaceAndPath(
                    "livestockhusbandry",
                    "livestock_names/names.json"
            );

    private static final List<String> NAMES = new ArrayList<>();

    private static boolean loaded = false;

    private ButcherLivestockNamePool() {
    }

    public static String randomName(ServerLevel level, LivingEntity animal) {
        loadIfNeeded(level);

        if (NAMES.isEmpty()) {
            return "Livestock ";
        }

        return NAMES.get(level.getRandom().nextInt(NAMES.size()));
    }

    private static void loadIfNeeded(ServerLevel level) {
        if (loaded) {
            return;
        }

        loaded = true;
        NAMES.clear();

        ResourceManager resourceManager = level.getServer().getResourceManager();

        Optional<Resource> resource = resourceManager.getResource(NAME_FILE);

        if (resource.isEmpty()) {
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(
                resource.get().open(),
                StandardCharsets.UTF_8
        )) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("names");

            if (array == null) {
                return;
            }

            for (JsonElement element : array) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }

                String name = element.getAsString().trim();

                if (!name.isEmpty()) {
                    NAMES.add(name);
                }
            }
        } catch (Exception ignored) {
        }
    }
}