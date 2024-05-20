package tsu.mc.mipor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.server.FMLServerHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO: Is there a way to propagate values from mod.properties to java class annotations and build.gradle.
@Mod(modid = "mipor", name = "Mobs Ignore Player On Respawn", version = "1.1")
public class Mipor {

    private static final Logger log = LogManager.getLogger();

    // Store a list of mobs to wake after the protection is over.
    private List<EntityMob> disabledMobs;

    private static final int PROTECTION_DURATION = 60 * 20; // 30 seconds in ticks

    public Mipor() {
        MinecraftForge.EVENT_BUS.register(this);
        log.info("[Tsu] Loaded Mipor v1.1!");

        // Initialize disabledMobs to prevent NPE.
        disabledMobs = new ArrayList<>();
    }

    private boolean isPlayer(Object o) {
        return o instanceof EntityPlayer;
    }

    private boolean isRecentlyRespawnedPlayer(Object target) {
        return (isPlayer(target) && isRecentlyRespawned((EntityPlayer) target));
    }

    @SubscribeEvent
    public void onLivingSetAttackTarget(LivingSetAttackTargetEvent event) {
        EntityLivingBase target = event.getTarget();
        EntityLivingBase attacker = event.getEntityLiving();
        if (isRecentlyRespawnedPlayer(target) && attacker instanceof EntityMob) {
            EntityMob attackerAsEntityMob = (EntityMob) attacker;
            ((EntityMob) attacker).setNoAI(true);
            disabledMobs.add(attackerAsEntityMob);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent respawnEvent) {
        EntityPlayer newPlayer = respawnEvent.player;
        // Set a flag on the new player indicating they just respawned
        newPlayer.getEntityData().setBoolean("justRespawned", true);
        //newPlayer.sendMessage(new TextComponentString("§k########################"));
        //newPlayer.sendMessage(new TextComponentString("§k$a#You have the protection!§k#"));
        //newPlayer.sendMessage(new TextComponentString("§k########################"));
        sendMultiColoredMessage("&9You &ahave &bthe &dProtection&f!", newPlayer);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && isRecentlyRespawned(event.player)) {
            EntityPlayer player = event.player;
            if (player.ticksExisted % 20 == 0) {
                String formattedDuration = "";
                int tickDelta = (PROTECTION_DURATION - player.ticksExisted);
                float prepPROTECTION_DURATION = (float) PROTECTION_DURATION;
                float prepTICKS_EXISTED = (float) player.ticksExisted;
                float percentage = ((prepTICKS_EXISTED * 100.0f) / prepPROTECTION_DURATION);
                NumberFormat numberFormat = NumberFormat.getNumberInstance();
                numberFormat.setMinimumFractionDigits(2);
                numberFormat.setMaximumFractionDigits(2);
                String formattedFloat = numberFormat.format(percentage);
                int tickDeltaInSeconds = Math.round(tickDelta / 20.0f);
                if (percentage >= 80.0f) {
                    formattedDuration = TextFormatting.DARK_RED + String.valueOf(tickDeltaInSeconds);
                } else if (percentage >= 60.0f) {
                    formattedDuration = TextFormatting.RED + String.valueOf(tickDeltaInSeconds);
                } else if (percentage >= 40.0f) {
                    formattedDuration = TextFormatting.YELLOW + String.valueOf(tickDeltaInSeconds);
                } else if (percentage >= 20.0f) {
                    formattedDuration = TextFormatting.GREEN + String.valueOf(tickDeltaInSeconds);
                } else {
                    formattedDuration = TextFormatting.AQUA + String.valueOf(tickDeltaInSeconds);
                }

                // DEBUG
                /*
                player.sendMessage(new TextComponentString(
                        String.valueOf(PROTECTION_DURATION)
                        +"/"+String.valueOf(player.ticksExisted)
                        +"/"+String.valueOf(tickDelta)
                        +"/"+String.valueOf(prepPROTECTION_DURATION)
                        +"/"+String.valueOf(prepTICKS_EXISTED)
                        +"/"+String.valueOf(formattedFloat)
                ));
                player.sendMessage(new TextComponentString("["+percentage+"]Protection: " + formattedDuration));
                */

                player.sendMessage(new TextComponentString("Protection: " + formattedDuration));
            }
            if (player.ticksExisted >= PROTECTION_DURATION) {
                player.getEntityData().setBoolean("justRespawned", false);
                //player.sendMessage(new TextComponentString("§k###################"));
                //player.sendMessage(new TextComponentString("§k#§cProtection Ended!§k#"));
                //player.sendMessage(new TextComponentString("§k###################"));
                sendMultiColoredMessage("&cProtection &4Ended&f!", player);
                for (EntityMob disabledMob : disabledMobs) {
                    disabledMob.setNoAI(false);
                }
            }
        }
    }

    private boolean isRecentlyRespawned(EntityPlayer player) {
        return player.getEntityData().getBoolean("justRespawned");
    }

    // Method to send a chat message with multiple colors
    public void sendMultiColoredMessage(String message, EntityPlayer player) {
        TextComponentString textComponent = new TextComponentString("");

        // Split the message into color segments
        String[] segments = message.split("&");

        // Iterate over the segments and apply colors
        for (String segment : segments) {
            if (segment.length() > 0) {
                TextFormatting color = getColor(segment.charAt(0));
                String content = segment.substring(1);

                // Create a text component for each segment and apply color
                TextComponentString segmentComponent = new TextComponentString(content);
                segmentComponent.getStyle().setColor(color);

                // Add the colored segment to the main text component
                textComponent.appendSibling(segmentComponent);
            }
        }

        // Send the multicolored message to the player
        player.sendMessage(textComponent);
    }

    // Helper method to get TextFormatting from color code character
    private TextFormatting getColor(char code) {
        // You can define your own mapping of color codes to TextFormatting here
        switch (code) {
            case '0': return TextFormatting.BLACK;
            case '1': return TextFormatting.DARK_BLUE;
            case '2': return TextFormatting.DARK_GREEN;
            case '3': return TextFormatting.DARK_AQUA;
            case '4': return TextFormatting.DARK_RED;
            case '5': return TextFormatting.DARK_PURPLE;
            case '6': return TextFormatting.GOLD;
            case '7': return TextFormatting.GRAY;
            case '8': return TextFormatting.DARK_GRAY;
            case '9': return TextFormatting.BLUE;
            case 'a': return TextFormatting.GREEN;
            case 'b': return TextFormatting.AQUA;
            case 'c': return TextFormatting.RED;
            case 'd': return TextFormatting.LIGHT_PURPLE;
            case 'e': return TextFormatting.YELLOW;
            case 'f': return TextFormatting.WHITE;
            default: return TextFormatting.RESET;
        }
    }
}