package online.carteris.hot_reload_resource_packs;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

public class HotReloadResourcePacks implements ModInitializer {
    public static final String MOD_ID = "hot_reload_resource_packs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    static HotReloader hot_reloader;
    static MinecraftClient client;

    @Override
    public void onInitialize() {
        client = MinecraftClient.getInstance();

        var resource_packs_path = getResourcePacksPath();

        hot_reloader = new HotReloader(client, LOGGER, resource_packs_path);
        hot_reloader.start();
    }

    private static @NotNull Path getResourcePacksPath() {
        var mod_container = FabricLoader.getInstance().getModContainer(MOD_ID);

        if (mod_container.isEmpty()) {
            throw new RuntimeException("This mod doesn't exist according to fabric. Something went very wrong");
        }

        // should be <minecraft location>/mods/hot_reload_resource_packs-x.y.z.jar
        var mod_location = mod_container.get().getOrigin().getPaths().get(0);

        // go upwards from our jarfile to mods folder, up again to minecraft folder, then down to resourcepacks.
        return mod_location.getParent().getParent().resolve("resourcepacks");
    }
}