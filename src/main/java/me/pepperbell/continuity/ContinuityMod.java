package me.pepperbell.continuity;

import com.dhj.actinium.api.render.terrain.BlockQuadTransformerHolder;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.ContinuityCtmTransformer;
import me.pepperbell.continuity.client.resource.BuiltinResourcePackInstaller;
import me.pepperbell.continuity.client.resource.ContinuityTextureEvents;
import me.pepperbell.continuity.client.resource.ContinuityModelEvents;
import me.pepperbell.continuity.proxy.IProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, guiFactory = "me.pepperbell.continuity.client.config.ContinuityGuiFactory")
public class ContinuityMod {
	public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

	@SidedProxy(modId = Reference.MOD_ID, clientSide = "me.pepperbell.continuity.proxy.ClientProxy", serverSide = "me.pepperbell.continuity.proxy.CommonProxy")
	public static IProxy proxy;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ContinuityClient.registerLoaders();
		BlockQuadTransformerHolder.register(new ContinuityCtmTransformer());
		LOGGER.debug("Registered Continuity block quad transformer");
		MinecraftForge.EVENT_BUS.register(new ContinuityTextureEvents());
		MinecraftForge.EVENT_BUS.register(new ContinuityModelEvents());
		if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
			BuiltinResourcePackInstaller.install();
		}
		LOGGER.info("Initializing {} for Minecraft 1.12.2", Reference.MOD_NAME);
	}
}
