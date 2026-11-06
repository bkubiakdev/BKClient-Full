package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.authlib.GameProfile;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.LogoutGhostEntity;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.Utils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.*;

@ModuleInfo(name = "LogoutSpot", category = ModuleCategory.VISUALS)
public class LogoutSpot extends BaseModule {
    private final Map<UUID, PlayerEntity> playerMap = new HashMap<>();
    private final Set<UUID> lastPlayerList = new HashSet<>();
    private final List<LogoutPos> spots = new ArrayList<>();

    public final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.world == null || mc.player == null || mc.getNetworkHandler() == null) {
            spots.clear();
            playerMap.clear();
            lastPlayerList.clear();
            return;
        }

        Collection<PlayerListEntry> currentEntries = mc.getNetworkHandler().getPlayerList();
        Set<UUID> currentUuids = new HashSet<>();
        Set<String> currentNames = new HashSet<>();

        for (PlayerListEntry entry : currentEntries) {
            UUID uuid = entry.getProfile().getId();
            String name = entry.getProfile().getName();
            
            currentUuids.add(uuid);
            if (name != null) currentNames.add(name.toLowerCase());

            // Track entity for data capture - must be in world to get position/health
            PlayerEntity worldPlayer = mc.world.getPlayerByUuid(uuid);
            if (worldPlayer != null && worldPlayer != mc.player && !(worldPlayer instanceof LogoutGhostEntity)) {
                playerMap.put(uuid, worldPlayer);
            }
        }

        // Detect disconnects (was in list, now not)
        for (UUID uuid : lastPlayerList) {
            if (!currentUuids.contains(uuid)) {
                if (playerMap.containsKey(uuid)) {
                    PlayerEntity player = playerMap.get(uuid);

                    PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
                    Identifier skin = entry != null ? entry.getSkinTextures().texture()
                            : DefaultSkinHelper.getSkinTextures(uuid).texture();

                    LogoutGhostEntity ghost = new LogoutGhostEntity(mc.world,
                            new GameProfile(uuid, player.getName().getString()));
                    ghost.copyFrom(player);
                    ghost.copyPositionAndRotation(player);
                    ghost.setInvisible(false);
                    ghost.setNoGravity(true);

                    // Copy equipment
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack stack = player.getEquippedStack(slot);
                        if (!stack.isEmpty()) {
                            ghost.equipStack(slot, stack.copy());
                        }
                    }

                    ghost.spawn();

                    spots.add(new LogoutPos(
                            uuid,
                            player.getName().getString(),
                            player.getPos(),
                            player.getHealth() + player.getAbsorptionAmount(),
                            System.currentTimeMillis(),
                            player,
                            skin,
                            ghost));
                    playerMap.remove(uuid);
                }
            }
        }

        // Detect reconnects to remove markers
        spots.removeIf(spot -> {
            boolean reconnected = currentUuids.contains(spot.getUuid()) 
                || (spot.getName() != null && currentNames.contains(spot.getName().toLowerCase()));
            
            if (!reconnected) {
                // Check world players as backup (sometimes tab list is laggy or different)
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player instanceof LogoutGhostEntity) continue;
                    if (player.getUuid().equals(spot.getUuid()) 
                        || (spot.getName() != null && player.getName().getString().equalsIgnoreCase(spot.getName()))) {
                        reconnected = true;
                        break;
                    }
                }
            }

            if (reconnected) {
                if (spot.getGhost() != null) {
                    spot.getGhost().remove();
                }
                return true;
            }
            return false;
        });

        lastPlayerList.clear();
        lastPlayerList.addAll(currentUuids);
    };

    public final EventListener<WorldChangeEvent> onWorldChange = event -> {
        this.clearAll();
    };

    public final EventListener<Render3DEvent> onRender3D = event -> {
    };

    public final EventListener<PreHudRenderEvent> onRender2D = event -> {
        if (spots.isEmpty())
            return;

        for (LogoutPos spot : spots) {
            Vec3d pos = spot.getPos().add(0, 2.0, 0);
            Vec2f screenPos = Utils.worldToScreen(pos);
            if (screenPos == null)
                continue;

            long timeSec = (System.currentTimeMillis() - spot.getTimestamp()) / 1000;
            String text = String.format("%s [%.1f] (%ds)", spot.getName(), spot.getHealth(), timeSec);
            Text textObj = Text.literal(text);

            float textWidth = Fonts.MEDIUM.getFont(11f).width(textObj);
            float fontHeight = Fonts.MEDIUM.getFont(11f).height();

            float headSize = fontHeight + 4;
            float totalWidth = textWidth + headSize + 8;

            float x = screenPos.x - totalWidth / 2f;
            float y = screenPos.y;

            event.getContext().drawRect(x - 4, y - 4, totalWidth + 8, fontHeight + 8, new ColorRGBA(0, 0, 0, 100));

            DrawUtility.drawPlayerHeadWithRoundedShader(event.getContext().getMatrices(), spot.getSkinTexture(), x,
                    y - 2, headSize, BorderRadius.all(2f), Colors.WHITE);
            DrawUtility.drawPlayerHatLayerWithRoundedShader(event.getContext().getMatrices(), spot.getSkinTexture(), x,
                    y - 2, headSize, BorderRadius.all(2f), Colors.WHITE);

            event.getContext().drawText(Fonts.MEDIUM.getFont(11f), textObj, (int) (x + headSize + 4), (int) y);
        }
    };

    private void clearAll() {
        for (LogoutPos spot : spots) {
            if (spot.getGhost() != null) {
                try {
                    spot.getGhost().remove();
                } catch (Exception ignored) {
                }
            }
        }
        spots.clear();
        playerMap.clear();
        lastPlayerList.clear();
    }

    @Override
    public void onDisable() {
        this.clearAll();
        super.onDisable();
    }
}
