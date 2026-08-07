package me.pepperbell.continuity.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class ContinuityConfigScreen extends GuiScreen {
	private final GuiScreen parent;
	private final ContinuityConfig config;

	public ContinuityConfigScreen(GuiScreen parent, ContinuityConfig config) {
		this.parent = parent;
		this.config = config;
	}

	@Override
	public void initGui() {
		buttonList.clear();
		buttonList.add(new GuiButton(0, width / 2 - 100, height / 2 - 30, 200, 20, optionText("connected_textures", config.connectedTextures.get())));
		buttonList.add(new GuiButton(1, width / 2 - 100, height / 2 - 5, 200, 20, optionText("emissive_textures", config.emissiveTextures.get())));
		buttonList.add(new GuiButton(2, width / 2 - 100, height / 2 + 25, 200, 20, I18n.format("gui.done")));
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) {
			config.connectedTextures.set(!config.connectedTextures.get());
			config.save();
			button.displayString = optionText("connected_textures", config.connectedTextures.get());
			reloadRenderers();
		} else if (button.id == 1) {
			config.emissiveTextures.set(!config.emissiveTextures.get());
			config.save();
			button.displayString = optionText("emissive_textures", config.emissiveTextures.get());
			reloadRenderers();
		} else if (button.id == 2) {
			mc.displayGuiScreen(parent);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRenderer, I18n.format("options.continuity.title"), width / 2, 30, 0xFFFFFF);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private String optionText(String key, boolean value) {
		return I18n.format("options.continuity." + key) + ": " + I18n.format(value ? "options.on" : "options.off");
	}

	private void reloadRenderers() {
		Minecraft.getMinecraft().renderGlobal.loadRenderers();
	}
}
