package online.carteris.hot_reload_resource_packs;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class HotReloader extends Thread {
    static Logger logger;
    static Minecraft minecraft_client;
    static Path resource_packs_path;
    static long recent_ping;
    public static long lastResourcifyCloseTime = 0;
    private static final long RESOURCIFY_COOLDOWN_MS = 5000;

    public void run() {
        logger.info("Watching for changes in {}", resource_packs_path);
        var default_fs = FileSystems.getDefault();

        // Track the latest reload time to avoid double events on (open,
        // modify), for systems that trigger these as unique separate
        // ENTRY_MODIFY
        recent_ping = System.currentTimeMillis();

        try (var watch_service = default_fs.newWatchService()) {
            // watch the resourcepacks folder for changes
            registerPath(watch_service, resource_packs_path);

            // recursively visit the preexisting directories and watch them for changes too
            Files.walkFileTree(resource_packs_path, new SimpleFileVisitor<>() {
               @Override
               public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                   registerPath(watch_service, dir);
                   return FileVisitResult.CONTINUE;
               }
            });

            // watch for file events until the thread is interrupted (until the game closes)
            while (!Thread.currentThread().isInterrupted()) {
                watchFileEvents(watch_service);
            }
        } catch (UnsupportedOperationException e) {
            logger.error("The filesystem doesn't support watching for file changes");
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error("IO error while trying to engage filesystem watching: {}", e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    void watchFileEvents(WatchService watch_service) {
        try {
            // a "key" represents a registered directory
            var watch_key = watch_service.take();
            // whether or not we will reload packs with this file event
            var reload = false;

            // handle file events that occur in one of our registered directories
            for (WatchEvent<?> event : watch_key.pollEvents()) {
                handleFileEvent(watch_service, watch_key, event);
                reload = true;
            }

            // let the watch service know that we're done handling events for this directory
            watch_key.reset();

            // Test if we just got here, is recent_ping within a seconds of the previous.
            if ((System.currentTimeMillis() - recent_ping) < 1500) {
                reload = false;
            }
            recent_ping = System.currentTimeMillis();

            if (reload) {
                boolean resourcifyRecentlyOpen =
                        (System.currentTimeMillis() - lastResourcifyCloseTime) < RESOURCIFY_COOLDOWN_MS;

                if (!isResourcifyScreen(minecraft_client.screen) && !resourcifyRecentlyOpen) {
                    minecraft_client.reloadResourcePacks();
                }
            }

        } catch (InterruptedException e) {
            logger.error("Got interrupted while trying to wait for file changes");
            throw new RuntimeException(e);
        }
    }

    private static boolean isResourcifyScreen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.equals("dev.dediamondpro.resourcify.gui.projectpage.ProjectScreen")
                || name.equals("dev.dediamondpro.resourcify.gui.browsepage.BrowseScreen")
                || name.equals("dev.dediamondpro.resourcify.gui.update.UpdateGui");
    }

    void handleFileEvent(WatchService watch_service, WatchKey watch_key, WatchEvent<?> event) {
        // this looks weird, but we're essentially joining the watched path (.minecraft/resourcepacks/xyz) with the ...
        // ... name of the file that had an event occur. Trying to get the absolute path of the file on its own ...
        // ... using event.context().toAbsolutePath() will give us the wrong path for some reason.
        Path context = (Path) event.context();
        Path baseDir = (Path) watch_key.watchable();
        Path path = baseDir.resolve(context).toAbsolutePath();

        // register any new directories to be watched
        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && path.toFile().isDirectory()) {
            registerPath(watch_service, path);
        }
    }

    void registerPath(WatchService watch_service, Path path) {
        try {
            // Ensure resourcepacks path exists by creating it if it doesn't. 
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Created missing resourcepacks directory: {}", path);
            }

            path.register(
                    watch_service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );
        } catch (IOException e) {
            logger.error("IO error while watching path ({}): {}", path, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    public HotReloader(Minecraft minecraft_client, Logger logger, Path resource_packs_path) {
        super();

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (isResourcifyScreen(client.screen)) {
                lastResourcifyCloseTime = System.currentTimeMillis();
            }
        });

        HotReloader.minecraft_client = minecraft_client;
        HotReloader.resource_packs_path = resource_packs_path;
        HotReloader.logger = logger;
    }
}
