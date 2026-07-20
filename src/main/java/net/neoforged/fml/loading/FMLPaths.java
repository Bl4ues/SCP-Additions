package net.neoforged.fml.loading;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
public enum FMLPaths {
    CONFIGDIR;
    public Path get() { return FabricLoader.getInstance().getConfigDir(); }
}
