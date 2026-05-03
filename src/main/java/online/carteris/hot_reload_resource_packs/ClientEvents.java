package online.carteris.hot_reload_resource_packs;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static online.carteris.hot_reload_resource_packs.HotReloader.isResourcifyScreen;

@Mod.EventBusSubscriber(modid = HotReloadResourcePacks.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (isResourcifyScreen(event.getCurrentScreen())) {
            HotReloader.lastResourcifyCloseTime = System.currentTimeMillis();
        }
    }
}