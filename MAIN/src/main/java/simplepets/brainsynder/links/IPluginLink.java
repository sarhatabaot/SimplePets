package simplepets.brainsynder.links;

import org.bukkit.plugin.Plugin;

public interface IPluginLink<T extends Plugin> {
    T getDependency();

    boolean isHooked();

    String getDependencyName();

    default boolean onHook() { return true; }

    default void onUnhook() {}
}