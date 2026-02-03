package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.OrientationMode;
import me.pepperbell.continuity.client.processor.Symmetry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;

public class RepeatCtmProperties extends BaseCtmProperties {
	protected int width;
	protected int height;
	protected Symmetry symmetry = Symmetry.NONE;
	protected OrientationMode orientationMode = OrientationMode.NONE;

	public RepeatCtmProperties(Properties properties, Identifier resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
	}

	@Override
	public void init() {
		super.init();
		parseWidth();
		parseHeight();
		parseSymmetry();
		parseOrient();
	}

	protected void parseWidth() {
		String widthStr = properties.getProperty("width");
		if (widthStr == null) {
			ContinuityClient.LOGGER.error("No 'width' value provided in file '" + resourceId + "' in pack '" + packId + "'");
			valid = false;
			return;
		}

		try {
			int width = Integer.parseInt(widthStr.trim());
			if (width > 0) {
				this.width = width;
				return;
			}
		} catch (NumberFormatException e) {
			//
		}
		ContinuityClient.LOGGER.error("Invalid 'width' value '" + widthStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
		valid = false;
	}

	protected void parseHeight() {
		String heightStr = properties.getProperty("height");
		if (heightStr == null) {
			ContinuityClient.LOGGER.error("No 'height' value provided in file '" + resourceId + "' in pack '" + packId + "'");
			valid = false;
			return;
		}

		try {
			int height = Integer.parseInt(heightStr.trim());
			if (height > 0) {
				this.height = height;
				return;
			}
		} catch (NumberFormatException e) {
			//
		}
		ContinuityClient.LOGGER.error("Invalid 'height' value '" + heightStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
		valid = false;
	}

	protected void parseSymmetry() {
		Symmetry symmetry = PropertiesParsingHelper.parseSymmetry(properties, "symmetry", resourceId, packId);
		if (symmetry != null) {
			this.symmetry = symmetry;
		}
	}

	protected void parseOrient() {
		OrientationMode orientationMode = PropertiesParsingHelper.parseOrientationMode(properties, "orient", resourceId, packId);
		if (orientationMode != null) {
			this.orientationMode = orientationMode;
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Symmetry getSymmetry() {
		return symmetry;
	}

	public OrientationMode getOrientationMode() {
		return orientationMode;
	}

	public static class Validator<T extends RepeatCtmProperties> implements TileAmountValidator<T> {
		@Override
		public boolean validateTileAmount(int amount, T properties) {
			int targetAmount = properties.getWidth() * properties.getHeight();
			if (amount == targetAmount) {
				return true;
			}
			ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires exactly " + targetAmount + " tiles but " + amount + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
			return false;
		}
	}
}
