package net.neoforged.fml;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
public final class ModList {
    private static final ModList INSTANCE = new ModList();
    public static ModList get() { return INSTANCE; }
    public boolean isLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
    public Optional<ModContainer> getModContainerById(String id) {
        return isLoaded(id) ? Optional.of(new ModContainer()) : Optional.empty();
    }
    public static final class ModContainer {
        public <T> void registerExtensionPoint(Class<T> type, T extension) { }
    }
}
