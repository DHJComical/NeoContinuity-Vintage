package me.pepperbell.continuity.client.config;

// import com.terraformersmc.modmenu.api.ConfigScreenFactory;
// import com.terraformersmc.modmenu.api.ModMenuApi;

import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ModMenuApiImpl /* implements ModMenuApi */ {
	//@Override
	public /* ConfigScreenFactory<?>*/ static IConfigScreenFactory getModConfigScreenFactory() {
		// return parent -> new ContinuityConfigScreen(parent, ContinuityConfig.INSTANCE);
		return (_, parent) -> new ContinuityConfigScreen(parent, ContinuityConfig.INSTANCE);
	}
}
