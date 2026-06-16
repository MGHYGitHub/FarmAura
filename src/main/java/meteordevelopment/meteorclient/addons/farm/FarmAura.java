/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.addons.farm;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class FarmAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHoe = settings.createGroup("Hoe");
    private final SettingGroup sgPlant = settings.createGroup("Plant");
    private final SettingGroup sgWeed = settings.createGroup("Weed");

    // ==================== General Settings ====================

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Working radius (centered on player).")
        .defaultValue(5)
        .min(1)
        .sliderMax(15)
        .build()
    );

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Instant range operation (bypasses delay).")
        .defaultValue(true)
        .build()
    );

    // ==================== Hoe Settings ====================

    private final Setting<Boolean> hoeEnabled = sgHoe.add(new BoolSetting.Builder()
        .name("hoe-enabled")
        .description("Range hoe all tillable blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hoeRequireTool = sgHoe.add(new BoolSetting.Builder()
        .name("hoe-require-tool")
        .description("Require a hoe in hotbar.")
        .defaultValue(true)
        .visible(hoeEnabled::get)
        .build()
    );

    // ==================== Plant Settings ====================

    private final Setting<Boolean> plantEnabled = sgPlant.add(new BoolSetting.Builder()
        .name("plant-enabled")
        .description("Range plant on all farmland.")
        .defaultValue(true)
        .build()
    );

    private final Setting<CropType> cropType = sgPlant.add(new EnumSetting.Builder<CropType>()
        .name("crop-type")
        .description("Type of crop to plant.")
        .defaultValue(CropType.Wheat)
        .visible(plantEnabled::get)
        .build()
    );

    private final Setting<Boolean> plantRequireSeed = sgPlant.add(new BoolSetting.Builder()
        .name("plant-require-seed")
        .description("Require seeds in hotbar.")
        .defaultValue(true)
        .visible(plantEnabled::get)
        .build()
    );

    // ==================== Weed Settings ====================

    private final Setting<Boolean> weedEnabled = sgWeed.add(new BoolSetting.Builder()
        .name("weed-enabled")
        .description("Range remove all weeds (grass, flowers, etc.).")
        .defaultValue(true)
        .build()
    );

    private int timer;
    private boolean hasHoed;
    private boolean hasPlanted;
    private boolean hasWeeded;

    public FarmAura() {
        super(Categories.Combat, "FarmAura", "Range hoe, planting and weeding.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        hasHoed = false;
        hasPlanted = false;
        hasWeeded = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        timer++;
        if (!instant.get() && timer < 2) return;
        timer = 0;

        if (weedEnabled.get() && !hasWeeded) {
            performRangeWeed();
            hasWeeded = true;
        }

        if (hoeEnabled.get() && !hasHoed) {
            performRangeHoe();
            hasHoed = true;
        }

        if (plantEnabled.get() && !hasPlanted) {
            performRangePlant();
            hasPlanted = true;
        }

        if (hasHoed && hasPlanted && hasWeeded) {
            toggle();
        }
    }

    // ==================== Range Hoe ====================

    private void performRangeHoe() {
        if (hoeRequireTool.get() && !hasHoe()) {
            warning("No hoe in hotbar!");
            return;
        }

        int hoeSlot = -1;
        if (hoeRequireTool.get()) {
            hoeSlot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof HoeItem).slot();
            if (hoeSlot == -1) return;
        }

        List<BlockPos> targets = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (mc.player.getBlockPos().getSquaredDistance(pos) <= r * r) {
                        BlockState state = mc.world.getBlockState(pos);
                        if (isHoable(state.getBlock()) && !(state.getBlock() instanceof FarmlandBlock)) {
                            targets.add(pos);
                        }
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            info("No tillable blocks found.");
            return;
        }

        info("Hoing " + targets.size() + " blocks...");

        int oldSlot = mc.player.getInventory().selectedSlot;
        if (hoeRequireTool.get() && hoeSlot != -1 && hoeSlot != oldSlot) {
            mc.player.getInventory().selectedSlot = hoeSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hoeSlot));
        }

        // 修复：使用正确的构造函数（3个参数）
        for (BlockPos pos : targets) {
            Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, true);
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
        }

        if (oldSlot != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = oldSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
        }

        info("Hoed " + targets.size() + " blocks.");
    }

    // ==================== Range Plant ====================

    private void performRangePlant() {
        int seedSlot = InvUtils.findInHotbar(itemStack ->
            isSeedItem(itemStack.getItem(), cropType.get())
        ).slot();

        if (plantRequireSeed.get() && seedSlot == -1) {
            warning("No seeds in hotbar!");
            return;
        }

        List<BlockPos> targets = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (mc.player.getBlockPos().getSquaredDistance(pos) <= r * r) {
                        BlockState state = mc.world.getBlockState(pos);
                        if (state.getBlock() instanceof FarmlandBlock) {
                            BlockPos abovePos = pos.up();
                            BlockState aboveState = mc.world.getBlockState(abovePos);
                            if (aboveState.isAir() || aboveState.isReplaceable()) {
                                targets.add(abovePos);
                            }
                        }
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            info("No plantable spots found.");
            return;
        }

        info("Planting " + targets.size() + " spots...");

        int oldSlot = mc.player.getInventory().selectedSlot;
        if (seedSlot != -1 && seedSlot != oldSlot) {
            mc.player.getInventory().selectedSlot = seedSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(seedSlot));
        }

        // 修复：使用正确的构造函数（3个参数）
        for (BlockPos pos : targets) {
            Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, true);
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
        }

        if (oldSlot != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = oldSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
        }

        info("Planted " + targets.size() + " crops.");
    }

    // ==================== Range Weed ====================

    private void performRangeWeed() {
        List<BlockPos> targets = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (mc.player.getBlockPos().getSquaredDistance(pos) <= r * r) {
                        BlockState state = mc.world.getBlockState(pos);
                        if (isWeedBlock(state.getBlock())) {
                            targets.add(pos);
                        }
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            info("No weeds found.");
            return;
        }

        info("Removing " + targets.size() + " weeds...");

        for (BlockPos pos : targets) {
            mc.interactionManager.attackBlock(pos, Direction.UP);
        }

        info("Removed " + targets.size() + " weeds.");
    }

    // ==================== Helper Methods ====================

    private boolean hasHoe() {
        return InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof HoeItem).found();
    }

    private boolean isSeedItem(Item item, CropType type) {
        return switch (type) {
            case Wheat -> item == Items.WHEAT_SEEDS;
            case Carrot -> item == Items.CARROT;
            case Potato -> item == Items.POTATO;
            case Beetroot -> item == Items.BEETROOT_SEEDS;
            case Melon -> item == Items.MELON_SEEDS;
            case Pumpkin -> item == Items.PUMPKIN_SEEDS;
            case NetherWart -> item == Items.NETHER_WART;
        };
    }

    private boolean isHoable(Block block) {
        return block instanceof GrassBlock
            || block == Blocks.DIRT
            || block == Blocks.COARSE_DIRT
            || block == Blocks.DIRT_PATH
            || block == Blocks.PODZOL
            || block == Blocks.MYCELIUM
            || block == Blocks.ROOTED_DIRT;
    }

    private boolean isWeedBlock(Block block) {
        return block instanceof GrassBlock
            || block instanceof TallPlantBlock
            || block instanceof FlowerBlock
            || block instanceof FernBlock
            || block == Blocks.DEAD_BUSH
            || block == Blocks.SWEET_BERRY_BUSH
            || block == Blocks.VINE
            || block == Blocks.LILY_PAD
            || block == Blocks.SEAGRASS
            || block == Blocks.TALL_SEAGRASS
            || block == Blocks.KELP
            || block == Blocks.KELP_PLANT;
    }

    // ==================== Enums ====================

    public enum CropType {
        Wheat, Carrot, Potato, Beetroot, Melon, Pumpkin, NetherWart
    }

    @Override
    public String getInfoString() {
        return "Range: " + range.get();
    }
}
