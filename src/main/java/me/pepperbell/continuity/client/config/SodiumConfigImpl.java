package me.pepperbell.continuity.client.config;

import me.pepperbell.continuity.client.ContinuityClient;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;

public class SodiumConfigImpl implements ConfigEntryPoint {
	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		ContinuityConfig config = ContinuityConfig.INSTANCE;

		builder.registerOwnModOptions()
				.setNonTintedIcon(ContinuityClient.asId(/* "icon.png" */ "neo_continuity_icon.png"))
				.addPage(builder.createOptionPage()
						.setName(Component.translatable(ContinuityConfigScreen.getTranslationKey("title")))
						.addOption(asSodiumOption(builder, config, config.connectedTextures)
								.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD))
						.addOption(asSodiumOption(builder, config, config.emissiveTextures)
								.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)));
	}

	private static BooleanOptionBuilder asSodiumOption(ConfigBuilder builder, ContinuityConfig config, Option.BooleanOption option) {
		String translationKey = ContinuityConfigScreen.getTranslationKey(option.getKey());
		Component component = Component.translatable(translationKey);
		Component tooltipComponent = Component.translatable(ContinuityConfigScreen.getTooltipKey(translationKey));

		return builder.createBooleanOption(ContinuityClient.asId(option.getKey()))
				.setName(component)
				.setTooltip(tooltipComponent)
				.setDefaultValue(option.getDefault())
				.setBinding(new OptionBindingImpl<>(option))
				.setStorageHandler(config::save);
	}

	private static class OptionBindingImpl<T> implements OptionBinding<T> {
		private final Option<T> option;

		public OptionBindingImpl(Option<T> option) {
			this.option = option;
		}

		@Override
		public void save(T value) {
			option.set(value);
		}

		@Override
		public T load() {
			return option.get();
		}
	}
}
