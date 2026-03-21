package online.carteris.hot_reload_resource_packs;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@Mod(HotReloadResourcePacks.MOD_ID)
public class HotReloadResourcePacks {
    public static final String MOD_ID = "hot_reload_resource_packs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    static HotReloader hot_reloader;
    static Minecraft client;

    public HotReloadResourcePacks() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            client = Minecraft.getInstance();

            Path resourcePacksPath = getResourcePacksPath();

            hot_reloader = new HotReloader(client, LOGGER, resourcePacksPath);
            hot_reloader.start();
        }
    }

    private static Path getResourcePacksPath() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        return gameDir.resolve("resourcepacks");
    }
}