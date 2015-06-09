package buildcraft;

import java.io.File;
import java.util.HashSet;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import buildcraft.api.core.BCLog;
import buildcraft.compat.CompatModuleAMT;
import buildcraft.compat.CompatModuleAgriCraft;
import buildcraft.compat.CompatModuleBase;
import buildcraft.compat.CompatModuleBinnie;
import buildcraft.compat.CompatModuleBundledRedstone;
import buildcraft.compat.CompatModuleCarpentersBlocks;
import buildcraft.compat.CompatModuleEnderIO;
import buildcraft.compat.CompatModuleFMP;
import buildcraft.compat.CompatModuleForestry;
import buildcraft.compat.CompatModuleImmibisMicroblocks;
import buildcraft.compat.CompatModuleIronChest;
import buildcraft.compat.CompatModuleMFR;
import buildcraft.compat.CompatModuleMineTweaker3;
import buildcraft.compat.CompatModuleNEI;
import buildcraft.compat.CompatModuleRailcraft;
import buildcraft.compat.CompatModuleRedLogic;
import buildcraft.compat.CompatModuleWAILA;
import buildcraft.compat.CompatModuleWitchery;
import buildcraft.compat.forestry.pipes.network.PacketGenomeFilterChange;
import buildcraft.compat.forestry.pipes.network.PacketRequestFilterSet;
import buildcraft.compat.forestry.pipes.network.PacketTypeFilterChange;
import buildcraft.core.DefaultProps;
import buildcraft.core.lib.network.ChannelHandler;
import buildcraft.gui.CompatGuiHandler;
import buildcraft.network.PacketHandlerCompat;
import buildcraft.texture.TextureManager;

@Mod(name = "BuildCraft Compat", version = "@VERSION@", useMetadata = false, modid = "BuildCraft|Compat", acceptedMinecraftVersions = "[1.7.10,1.8)", dependencies = "required-after:Forge@[10.13.0.1179,);required-after:BuildCraft|Core;after:Forestry;after:BuildCraft|Transport;after:BuildCraft|Builders")
public class BuildCraftCompat extends BuildCraftMod {

    @Mod.Instance("BuildCraft|Compat")
    public static BuildCraftCompat instance;

    private static Configuration config;
    private static final HashSet<CompatModuleBase> modules;
    private static final HashSet<String> moduleNames;
    private static ChannelHandler compatChannelHandler;

    private void offerModule(final CompatModuleBase module) {
        if (module.canLoad() && BuildCraftCompat.config.getBoolean(module.name(), "modules", true, module.comment())) {
            BuildCraftCompat.modules.add(module);
            BuildCraftCompat.moduleNames.add(module.name());
        }
    }

    public static boolean isLoaded(String module) {
        return moduleNames.contains(module);
    }

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent evt) {
        (BuildCraftCompat.config = new Configuration(new File(new File(evt.getSuggestedConfigurationFile().getParentFile(), "buildcraft"), "compat.cfg"))).load();
        this.offerModule(new CompatModuleWitchery());
        this.offerModule(new CompatModuleAMT());
        this.offerModule(new CompatModuleFMP());
        this.offerModule(new CompatModuleForestry());
        this.offerModule(new CompatModuleRailcraft());
        this.offerModule(new CompatModuleRedLogic());
        this.offerModule(new CompatModuleImmibisMicroblocks());
        this.offerModule(new CompatModuleMFR());
        this.offerModule(new CompatModuleMineTweaker3());
        this.offerModule(new CompatModuleNEI());
        this.offerModule(new CompatModuleCarpentersBlocks());
        this.offerModule(new CompatModuleIronChest());
        this.offerModule(new CompatModuleWAILA());
        this.offerModule(new CompatModuleBundledRedstone());
        this.offerModule(new CompatModuleAgriCraft());
        this.offerModule(new CompatModuleBinnie());
        this.offerModule(new CompatModuleEnderIO());
        BuildCraftCompat.config.save();

        for (final CompatModuleBase m : BuildCraftCompat.modules) {
            m.preInit();
        }
    }
    
    @Mod.EventHandler
    public void init(final FMLInitializationEvent evt) {

        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new CompatGuiHandler());

        compatChannelHandler = new ChannelHandler();
        MinecraftForge.EVENT_BUS.register(this);

        compatChannelHandler.registerPacketType(PacketGenomeFilterChange.class);
        compatChannelHandler.registerPacketType(PacketTypeFilterChange.class);
        compatChannelHandler.registerPacketType(PacketRequestFilterSet.class);

        channels = NetworkRegistry.INSTANCE.newChannel
                (DefaultProps.NET_CHANNEL_NAME + "-COMPAT", compatChannelHandler, new PacketHandlerCompat());

        for (final CompatModuleBase m : BuildCraftCompat.modules) {
            BCLog.logger.info("Loading compat module " + m.name());
            m.init();
        }
    }
    
    @Mod.EventHandler
    public void postInit(final FMLPostInitializationEvent evt) {
        for (final CompatModuleBase m : BuildCraftCompat.modules) {
            m.postInit();
        }
    }

    @Mod.EventHandler
    public void missingMapping(FMLMissingMappingsEvent event) {
        CompatModuleForestry.missingMapping(event);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void handleTextureRemap(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() == 1) {
            TextureManager.getInstance().initIcons(event.map);
        }
    }

    public static boolean hasModule(final String module) {
        return BuildCraftCompat.moduleNames.contains(module);
    }
    
    static {
        modules = new HashSet<CompatModuleBase>();
        moduleNames = new HashSet<String>();
    }
}
