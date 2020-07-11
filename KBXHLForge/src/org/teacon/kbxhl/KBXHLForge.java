package org.teacon.kbxhl;

import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

import java.util.regex.Pattern;

@Mod("kbxhl")
@Mod.EventBusSubscriber(modid = "kbxhl")
public final class KBXHLForge {

    private static final Pattern CHAT_PATTERN_1 = Pattern.compile("\u6d77\u87ba[\uff01\u0021]\u005c\u005c\u0073\u002a");
    private static final Pattern CHAT_PATTERN_2 = Pattern.compile("\u571f\u7403[\uff01\u0021]\u005c\u005c\u0073\u002a");

    static String version = "";

    private KBXHLScoreManager scoreManager;

    public KBXHLForge() {
        ModLoadingContext context = ModLoadingContext.get();
        version = context.getActiveContainer().getModInfo().getVersion().toString();
        context.registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (serverVer, isDedi) -> true));
        MinecraftForge.EVENT_BUS.addListener(this.scoreManager::onTick);
    }

    @SubscribeEvent
    public static void onServerStart(FMLServerStartingEvent event) {
        new KBXHLCommand(event.getCommandDispatcher());
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        final String previous = event.getMessage();
        if (CHAT_PATTERN_1.matcher(event.getMessage()).matches()) {
            event.setComponent(new TranslationTextComponent("chat.type.text", "<<<<<<< " + previous));
            event.getPlayer().sendMessage(new TranslationTextComponent("chat.type.text", ">>>>>>> " + previous.replace("\u6d77\u87ba", "\u571f\u7403")), ChatType.CHAT);
        } else if (CHAT_PATTERN_2.matcher(event.getMessage()).matches()) {
            event.setComponent(new TranslationTextComponent("chat.type.text", "<<<<<<< " + previous));
            event.getPlayer().sendMessage(new TranslationTextComponent("chat.type.text", ">>>>>>> " + previous.replace("\u571f\u7403", "\u6d77\u87ba")), ChatType.CHAT);
        }
    }

}
