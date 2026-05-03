package online.carteris.hot_reload_resource_packs;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import static online.carteris.hot_reload_resource_packs.HotReloader.isResourcifyScreen;

@EventBusSubscriber(modid = HotReloadResourcePacks.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (isResourcifyScreen(event.getCurrentScreen())) {
            HotReloader.lastResourcifyCloseTime = System.currentTimeMillis();
        }
    }
}