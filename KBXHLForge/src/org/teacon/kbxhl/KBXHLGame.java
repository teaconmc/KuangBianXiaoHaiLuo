package org.teacon.kbxhl;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.ShulkerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.play.server.SChangeBlockPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public final class KBXHLGame {

    private static final Field CHANGE_BLOCK_PACKET_POS;
    private static final Field CHANGE_BLOCK_PACKET_STATE;

    static {
        CHANGE_BLOCK_PACKET_POS = ObfuscationReflectionHelper.findField(SChangeBlockPacket.class, "field_179828_a");
        CHANGE_BLOCK_PACKET_STATE = ObfuscationReflectionHelper.findField(SChangeBlockPacket.class, "field_197686_b");
    }

    public enum ShulkerType { // yneos, who the heck is going to extend this enum?!
        N("普通海螺", DyeColor.PURPLE, 1, 108),
        R("稀有海螺", DyeColor.PINK, 5, 102),
        SR("超级稀有海螺", DyeColor.ORANGE, 25, 90),
        SSR("特级稀有海螺", DyeColor.YELLOW, 125, 54);

        static final ShulkerType TYPES[] = ShulkerType.values();

        public static ShulkerType byIndex(int ordinal) {
            return ordinal >= 0 && ordinal <= TYPES.length ? TYPES[ordinal] : N;
        }

        public final String baseName;
        public final DyeColor color;
        public final int score;
        public final int lifeSpawn;

        ShulkerType(String baseName, DyeColor color, int score, int lifeSpawn) {
            this.baseName = baseName;
            this.color = color;
            this.score = score;
            this.lifeSpawn = lifeSpawn;
        }

        public ShulkerEntity create(ServerPlayerEntity creator, BlockPos targetPos, Direction expectedFacing) {
            ShulkerEntity shulker = new ShulkerEntity(EntityType.SHULKER, creator.world);
            /*
             * All shulker-exclusive DataParameters have protected access so we cannot
             * directly use them.
             * We use the approach used by vanilla `/data merge' command, i.e. manipulate
             * them by writing/reading its NBT instead.
             */
            final CompoundNBT entityData = new CompoundNBT();
            shulker.writeAdditional(entityData);
            entityData.putInt("Color", this.color.getId());
            entityData.putByte("AttachFace", (byte) expectedFacing.getIndex());
            shulker.readAdditional(entityData);
            // Write data related to the mini-game.
            final CompoundNBT gameData = shulker.getPersistentData().getCompound("KBXHL");
            gameData.put("Creator", NBTUtil.writeUniqueId(creator.getUniqueID()));
            gameData.putInt("Type", this.ordinal());
            gameData.putInt("RemainTime", this.lifeSpawn);
            // Other misc stuff
            shulker.setNoAI(true);
            shulker.setNoGravity(true);
            shulker.getLootTableResourceLocation();
            shulker.setCustomName(new StringTextComponent(this.baseName).applyTextStyle(TextFormatting.GOLD));
            shulker.setPosition(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            return shulker;
        }
    }

    public static final class ShulkerSpawner {
        final ServerPlayerEntity player;
        final List<Vec3i> candidateOffsets;
        private final Random rng = new Random();
        private int counter = 0;
        ShulkerSpawner(final ServerPlayerEntity player, final List<Vec3i> offsets) {
            this.player = player;
            this.candidateOffsets = offsets;
        }
        public void tick() {
            if (++this.counter >= 20) {
                this.counter = 0;
                Collections.shuffle(candidateOffsets, this.rng);
                final int result = this.rng.nextInt(23);
                final ShulkerType type;
                if (result < 16) {
                    type = ShulkerType.N;
                } else if (result < 20) {
                    type = ShulkerType.R;
                } else if (result < 22) {
                    type = ShulkerType.SR;
                } else {
                    type = ShulkerType.SSR;
                }
                player.getServerWorld().addEntity(type.create(player, player.getPosition().add(this.candidateOffsets.get(0)), Direction.NORTH));
            }
        }
    }

    /**
     * A set of UUIDs of players who are playing the KBXHL mini-game.
     */
    final Set<UUID> currentPlayers = new HashSet<>();

    private final List<Vec3i> positionForAir;
    private final List<Vec3i> positionForEndBricks;
    private final List<Vec3i> positionForPurpurBlock;
    private final List<Vec3i> positionForPurpleGlass;

    private BlockState air = Blocks.AIR.getDefaultState();
    private BlockState endBricks = Blocks.END_STONE_BRICKS.getDefaultState();
    private BlockState purpurBlock = Blocks.PURPUR_BLOCK.getDefaultState();
    private BlockState purpleGlass = Blocks.PURPLE_STAINED_GLASS.getDefaultState();

    KBXHLGame() {
        final ArrayList<Vec3i> airPos = new ArrayList<>();
        final ArrayList<Vec3i> endBrickPos = new ArrayList<>();
        final ArrayList<Vec3i> purpurPos = new ArrayList<>();
        final ArrayList<Vec3i> purpleGlassPos = new ArrayList<>();
        for (int i = -4; i <= 4; ++i) {
            for (int j = -4; j <= 4; ++j) {
                int maxSquared = Math.max(i * i, j * j);
                int minSquared = Math.min(i * i, j * j);
                if (minSquared < 4 * 4) {
                    endBrickPos.add(new Vec3i(i, -1, j));
                    purpleGlassPos.add(new Vec3i(i, 3, j));
                    if (maxSquared > 2 * 2) {
                        if (minSquared < 2 * 2 && maxSquared < 4 * 4) {
                            purpurPos.add(new Vec3i(i, 0, j));
                            purpurPos.add(new Vec3i(i, 1, j));
                            purpurPos.add(new Vec3i(i, 2, j));
                        } else {
                            endBrickPos.add(new Vec3i(i, 0, j));
                            endBrickPos.add(new Vec3i(i, 1, j));
                            endBrickPos.add(new Vec3i(i, 2, j));
                        }
                    } else {
                        airPos.add(new Vec3i(i, 0, j));
                        airPos.add(new Vec3i(i, 1, j));
                        airPos.add(new Vec3i(i, 2, j));
                    }
                }
            }
        }
        this.positionForAir = Collections.unmodifiableList(airPos);
        this.positionForEndBricks = Collections.unmodifiableList(endBrickPos);
        this.positionForPurpurBlock = Collections.unmodifiableList(purpurPos);
        this.positionForPurpleGlass = Collections.unmodifiableList(purpleGlassPos);
    }

    /*
     * Apparently, Sponge implements Viewer.sendBlockChange using a SPacketBlockChange
     * (now known as SChangeBlockPacket) with some black magic.
     * We do the same here to create a fake block state change.
     * TODO This does look like a use case for access transformer?
     */
    static void setFakeStateFor(ServerPlayerEntity player, BlockPos targetPos, BlockState theState) {
        try {
            final SChangeBlockPacket packet = new SChangeBlockPacket();
            CHANGE_BLOCK_PACKET_POS.set(packet, targetPos);
            CHANGE_BLOCK_PACKET_STATE.set(packet, theState);
            player.connection.sendPacket(packet);
        } catch (Exception e) {
            // TODO Log error
        }
    }

    /*
     * Sponge implements Viewer.resetBlockChange using a plain SPacketBlockChange
     * (now known as SChangeBlockPacket).
     * We do the same here to reset those fake block states.
     */
    static void resetFakeStateFor(ServerPlayerEntity player, BlockPos targetPos) {
        player.connection.sendPacket(new SChangeBlockPacket(player.world, targetPos));
    }

    public void fixStruct(ServerPlayerEntity player, Vec3i offset) {
        final BlockPos base = player.getPosition();
        if (this.positionForAir.contains(offset)) {
            setFakeStateFor(player, base.add(offset), this.air);
        }
        if (this.positionForEndBricks.contains(offset)) {
            setFakeStateFor(player, base.add(offset), this.endBricks);
        }
        if (this.positionForPurpurBlock.contains(offset)) {
            setFakeStateFor(player, base.add(offset), this.purpurBlock);
        }
        if (this.positionForPurpleGlass.contains(offset)) {
            setFakeStateFor(player, base.add(offset), this.purpleGlass);
        }
    }

    public void constructArenaFor(ServerPlayerEntity player) {
        final BlockPos base = player.getPosition();
        for (Vec3i offset : this.positionForAir) {
            setFakeStateFor(player, base.add(offset), this.air);
        }
        for (Vec3i offset : this.positionForEndBricks) {
            setFakeStateFor(player, base.add(offset), this.endBricks);
        }
        for (Vec3i offset : this.positionForPurpurBlock) {
            setFakeStateFor(player, base.add(offset), this.purpurBlock);
        }
        for (Vec3i offset : this.positionForPurpleGlass) {
            setFakeStateFor(player, base.add(offset), this.purpleGlass);
        }
        player.setLocationAndAngles(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0F, 0F);
    }

    public void destructArenaFor(ServerPlayerEntity player) {
        final BlockPos base = player.getPosition();
        for (Vec3i offset : this.positionForAir) {
            resetFakeStateFor(player, base.add(offset));
        }
        for (Vec3i offset : this.positionForEndBricks) {
            resetFakeStateFor(player, base.add(offset));
        }
        for (Vec3i offset : this.positionForPurpurBlock) {
            resetFakeStateFor(player, base.add(offset));
        }
        for (Vec3i offset : this.positionForPurpleGlass) {
            resetFakeStateFor(player, base.add(offset));
        }

        player.getServerWorld().getEntities(EntityType.SHULKER, e -> e.getDistanceSq(base.getX(), base.getY(), base.getZ()) <= 25
                && NBTUtil.readUniqueId(e.getPersistentData().getCompound("KBXHL").getCompound("Creator")).equals(player.getUniqueID())).forEach(Entity::remove);
    }

    @SubscribeEvent
    public void onGameStart(KBXHLEvent.Start event) {
        final ServerPlayerEntity player = event.getPlayer();
        final UUID uuid = player.getUniqueID();
        if (!currentPlayers.contains(uuid)) {
            currentPlayers.add(uuid);
            PlayerInventory inv = player.inventory;
            KBXHLForge.theMod.config.savedInv.put(uuid, inv.write(new ListNBT()));
            inv.mainInventory.clear();
            inv.armorInventory.clear();
            inv.offHandInventory.clear();
            final ItemStack disabled = new ItemStack(Items.BARRIER);
            IntStream.rangeClosed(0, 8).forEach(index -> inv.setInventorySlotContents(index, disabled));
            inv.setInventorySlotContents(4, new ItemStack(Items.STONE_AXE)
                    .setDisplayName(new StringTextComponent("点击左键狂扁小海螺")
                            .applyTextStyle(TextFormatting.BOLD)
                            .applyTextStyle(TextFormatting.GOLD)));
            inv.currentItem = 4; // In the middle of hotbar.
        }
    }

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        final LivingEntity target = event.getEntityLiving();
        if (target.getType() == EntityType.SHULKER) {
            if (target.getPersistentData().contains("KBXHL", Constants.NBT.TAG_COMPOUND)) {
                final Entity source = event.getSource().getTrueSource();
                if (source instanceof ServerPlayerEntity) {
                    event.setCanceled(true);
                    ShulkerType type = ShulkerType.byIndex(target.getPersistentData().getCompound("KBXHL").getInt("Type"));
                    KBXHLForge.theMod.scoreManager.add((ServerPlayerEntity) source, type.score);
                    target.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public void onGameTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.side == LogicalSide.SERVER) {
            ((ServerWorld) event.world).getEntities(EntityType.SHULKER, e -> e.getPersistentData().contains("KBXHL", Constants.NBT.TAG_COMPOUND))
                    .forEach(shulker -> {
                        final CompoundNBT gameData = shulker.getPersistentData().getCompound("KBXHL");
                        int remainTime = gameData.getInt("RemainTime");
                        if (--remainTime <= 0) {
                            shulker.remove();
                        } else {
                            gameData.putInt("RemainTime", remainTime);
                        }
                    });
        }
    }

    @SubscribeEvent
    public void onGameStop(KBXHLEvent.Stop event) {
        final ServerPlayerEntity player = event.getPlayer();
        final UUID uuid = player.getUniqueID();
        if (currentPlayers.contains(player.getUniqueID())) {
            PlayerInventory inv = player.inventory;
            inv.read(KBXHLForge.theMod.config.savedInv.remove(player.getUniqueID()));
            currentPlayers.remove(uuid);
        }
    }
}
