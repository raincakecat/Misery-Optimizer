package dev.tung.miseryoptimizer;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu entrypoint; the O key continues to work without Mod Menu installed. */
public final class MiseryOptimizerModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MiseryOptimizerConfigScreen(parent, MiseryOptimizerClient.config());
    }
}
