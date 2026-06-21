/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.addons.farm;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FarmAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHoe = settings.createGroup("锄地");
    private final SettingGroup sgPlant = settings.createGroup("种植");
    private final SettingGroup sgWeed = settings.createGroup("除草");
    private final SettingGroup sgDebug = settings.createGroup("调试");

    // ==================== General Settings ====================

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("工作半径")
        .description("以玩家为中心的水平工作半径")
        .defaultValue(4)
        .min(1)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("操作延迟")
        .description("每次操作的延迟（毫秒），越小越快")
        .defaultValue(80)
        .min(20)
        .sliderMax(300)
        .build()
    );

    private final Setting<Boolean> loopMode = sgGeneral.add(new BoolSetting.Builder()
        .name("循环模式")
        .description("持续循环执行（关闭则执行一次后自动停止）")
        .defaultValue(true)
        .build()
    );

    // ==================== Hoe Settings ====================

    private final Setting<Boolean> hoeEnabled = sgHoe.add(new BoolSetting.Builder()
        .name("启用锄地")
        .description("自动锄地")
        .defaultValue(true)
        .build()
    );

    // ==================== Plant Settings ====================

    private final Setting<Boolean> plantEnabled = sgPlant.add(new BoolSetting.Builder()
        .name("启用种植")
        .description("自动种植")
        .defaultValue(true)
        .build()
    );

    private final Setting<CropType> cropType = sgPlant.add(new EnumSetting.Builder<CropType>()
        .name("作物类型")
        .description("要种植的作物类型")
        .defaultValue(CropType.Wheat)
        .visible(plantEnabled::get)
        .build()
    );

    // ==================== Weed Settings ====================

    private final Setting<Boolean> weedEnabled = sgWeed.add(new BoolSetting.Builder()
        .name("启用除草")
        .description("自动清除杂草（花、高草丛等）")
        .defaultValue(true)
        .build()
    );

    // ==================== Debug Settings ====================

    private final Setting<Boolean> chatFeedback = sgDebug.add(new BoolSetting.Builder()
        .name("聊天反馈")
        .description("在聊天栏显示操作反馈")
        .defaultValue(false)
        .build()
    );

    // ==================== 运行状态 ====================

    private enum Phase {
        IDLE, WEEDING, HOEING, PLANTING, DONE
    }

    private Phase phase = Phase.IDLE;
    private List<BlockPos> weedTargets = new ArrayList<>();
    private List<BlockPos> hoeTargets = new ArrayList<>();
    private List<BlockPos> plantTargets = new ArrayList<>();
    private int currentIndex = 0;
    private long lastActionTime = 0;
    private boolean isProcessing = false;

    public FarmAura() {
        super(Categories.Combat, "自动农场", "范围锄地、种植、除草");
    }

    @Override
    public void onActivate() {
        resetState();
        if (chatFeedback.get()) info("自动农场已启动");
    }

    @Override
    public void onDeactivate() {
        resetState();
        if (chatFeedback.get()) info("自动农场已关闭");
    }

    private void resetState() {
        phase = Phase.IDLE;
        weedTargets.clear();
        hoeTargets.clear();
        plantTargets.clear();
        currentIndex = 0;
        lastActionTime = 0;
        isProcessing = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (isProcessing) return;

        if (phase == Phase.IDLE) {
            scanTargets();
            if (weedTargets.isEmpty() && hoeTargets.isEmpty() && plantTargets.isEmpty()) {
                if (chatFeedback.get()) info("没有找到目标方块");
                if (!loopMode.get()) toggle();
                return;
            }
            if (!weedTargets.isEmpty()) {
                phase = Phase.WEEDING;
                if (chatFeedback.get()) info("开始除草: " + weedTargets.size() + " 个");
            } else if (!hoeTargets.isEmpty()) {
                phase = Phase.HOEING;
                if (chatFeedback.get()) info("开始锄地: " + hoeTargets.size() + " 个");
            } else if (!plantTargets.isEmpty()) {
                phase = Phase.PLANTING;
                if (chatFeedback.get()) info("开始种植: " + plantTargets.size() + " 个");
            }
            currentIndex = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastActionTime < delay.get()) return;

        isProcessing = true;
        boolean moreWork = executeCurrentPhase();
        isProcessing = false;

        if (!moreWork) {
            advancePhase();
        }
    }

    // ==================== 扫描目标 ====================

    private void scanTargets() {
        weedTargets.clear();
        hoeTargets.clear();
        plantTargets.clear();

        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        // 获取玩家视线高度（眼睛位置）
        double eyeY = mc.player.getEyeY();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                // 只扫描表面方块（从玩家脚下 -1 到 +2 的高度范围）
                // 这样就不会扫描到地下深处的方块
                for (int y = -1; y <= 2; y++) {
                    BlockPos pos = center.add(x, y, z);

                    // 检查水平距离（只计算 XZ 平面的距离，忽略 Y）
                    double dx = pos.getX() - center.getX() + 0.5;
                    double dz = pos.getZ() - center.getZ() + 0.5;
                    double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                    if (horizontalDist > r) continue;

                    // 检查玩家是否能够到（交互距离）
                    if (!canReach(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    Block block = state.getBlock();

                    // 1. 杂草（花、高草丛等）
                    if (weedEnabled.get() && isWeedBlock(block)) {
                        weedTargets.add(pos);
                    }
                    // 2. 可耕地（表面方块）
                    else if (hoeEnabled.get() && isHoable(block) && !(block instanceof FarmlandBlock)) {
                        // 检查上方是否是空气（确保是表面）
                        BlockPos abovePos = pos.up();
                        BlockState aboveState = mc.world.getBlockState(abovePos);
                        boolean isSurface = aboveState.isAir() || aboveState.isReplaceable();
                        if (isSurface) {
                            hoeTargets.add(pos);
                        }
                    }
                    // 3. 耕地（需要种植）
                    else if (plantEnabled.get() && block instanceof FarmlandBlock) {
                        BlockPos abovePos = pos.up();
                        BlockState aboveState = mc.world.getBlockState(abovePos);
                        if (aboveState.isAir() || aboveState.isReplaceable()) {
                            // 检查是否能到达上方位置（种植位置）
                            if (canReach(abovePos)) {
                                plantTargets.add(abovePos);
                            }
                        }
                    }
                }
            }
        }

        // 按距离排序（从近到远）
        weedTargets.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(center)));
        hoeTargets.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(center)));
        plantTargets.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(center)));
    }

    // ==================== 判断是否能到达 ====================

    private boolean canReach(BlockPos pos) {
        if (mc.player == null) return false;

        // 获取玩家眼睛位置
        Vec3d eyePos = mc.player.getEyePos();
        // 获取方块中心位置
        Vec3d blockCenter = pos.toCenterPos();

        // 计算距离
        double distance = eyePos.distanceTo(blockCenter);

        // Minecraft 默认交互距离是 4.5 格，我们稍微放宽一点到 5 格
        // 但考虑到方块本身的大小，实际可交互距离会稍大
        return distance <= 5.0;
    }

    // ==================== 执行当前阶段 ====================

    private boolean executeCurrentPhase() {
        List<BlockPos> currentTargets = getCurrentTargets();
        if (currentTargets == null || currentIndex >= currentTargets.size()) {
            return false;
        }

        BlockPos pos = currentTargets.get(currentIndex);

        // 执行前再次检查是否能到达
        if (!canReach(pos)) {
            currentIndex++;
            return currentIndex < currentTargets.size();
        }

        boolean success = false;

        switch (phase) {
            case WEEDING -> success = performWeed(pos);
            case HOEING -> success = performHoe(pos);
            case PLANTING -> success = performPlant(pos, pos.down());
            default -> {}
        }

        if (success) {
            currentIndex++;
            lastActionTime = System.currentTimeMillis();
        } else {
            currentIndex++;
        }

        return currentIndex < currentTargets.size();
    }

    private List<BlockPos> getCurrentTargets() {
        return switch (phase) {
            case WEEDING -> weedTargets;
            case HOEING -> hoeTargets;
            case PLANTING -> plantTargets;
            default -> null;
        };
    }

    // ==================== 阶段切换 ====================

    private void advancePhase() {
        if (phase == Phase.WEEDING) {
            if (!hoeTargets.isEmpty()) {
                phase = Phase.HOEING;
                currentIndex = 0;
                if (chatFeedback.get()) info("开始锄地: " + hoeTargets.size() + " 个");
                return;
            } else if (!plantTargets.isEmpty()) {
                phase = Phase.PLANTING;
                currentIndex = 0;
                if (chatFeedback.get()) info("开始种植: " + plantTargets.size() + " 个");
                return;
            }
        } else if (phase == Phase.HOEING) {
            if (!plantTargets.isEmpty()) {
                phase = Phase.PLANTING;
                currentIndex = 0;
                if (chatFeedback.get()) info("开始种植: " + plantTargets.size() + " 个");
                return;
            }
        }

        phase = Phase.DONE;
        if (chatFeedback.get()) info("所有操作完成！");

        if (loopMode.get()) {
            phase = Phase.IDLE;
            scanTargets();
            if (chatFeedback.get()) info("循环扫描中...");
        } else {
            toggle();
        }
    }

    // ==================== 执行操作 ====================

    private boolean performHoe(BlockPos pos) {
        ItemStack mainHand = mc.player.getMainHandStack();
        if (!(mainHand.getItem() instanceof HoeItem)) {
            if (chatFeedback.get()) warning("主手需要锄头！");
            return false;
        }

        BlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();

        if (!isHoable(block) || block instanceof FarmlandBlock) {
            return true;
        }

        mc.player.swingHand(Hand.MAIN_HAND);
        Vec3d hitVec = pos.toCenterPos();
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        return true;
    }

    private boolean performPlant(BlockPos plantPos, BlockPos farmlandPos) {
        ItemStack offHand = mc.player.getOffHandStack();
        Item targetSeed = getSeedItem(cropType.get());

        if (offHand.isEmpty() || offHand.getItem() != targetSeed) {
            if (chatFeedback.get()) warning("副手需要对应的种子！");
            return false;
        }

        if (!(mc.world.getBlockState(farmlandPos).getBlock() instanceof FarmlandBlock)) {
            return true;
        }
        BlockState aboveState = mc.world.getBlockState(plantPos);
        if (!aboveState.isAir() && !aboveState.isReplaceable()) {
            return true;
        }

        mc.player.swingHand(Hand.OFF_HAND);
        Vec3d hitVec = new Vec3d(farmlandPos.getX() + 0.5, farmlandPos.getY() + 0.5, farmlandPos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, farmlandPos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hitResult);
        return true;
    }

    private boolean performWeed(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!isWeedBlock(state.getBlock())) {
            return true;
        }

        mc.player.swingHand(Hand.MAIN_HAND);
        mc.interactionManager.attackBlock(pos, Direction.UP);
        return true;
    }

    // ==================== Helper Methods ====================

    private Item getSeedItem(CropType type) {
        return switch (type) {
            case Wheat -> Items.WHEAT_SEEDS;
            case Carrot -> Items.CARROT;
            case Potato -> Items.POTATO;
            case Beetroot -> Items.BEETROOT_SEEDS;
            case Melon -> Items.MELON_SEEDS;
            case Pumpkin -> Items.PUMPKIN_SEEDS;
            case NetherWart -> Items.NETHER_WART;
        };
    }

    private boolean isHoable(Block block) {
        return block == Blocks.DIRT
            || block == Blocks.COARSE_DIRT
            || block == Blocks.DIRT_PATH
            || block == Blocks.PODZOL
            || block == Blocks.MYCELIUM
            || block == Blocks.ROOTED_DIRT
            || block == Blocks.GRASS_BLOCK;
    }

    private boolean isWeedBlock(Block block) {
        return block instanceof TallPlantBlock
            || block instanceof FlowerBlock
            || block instanceof FernBlock
            || block == Blocks.DEAD_BUSH
            || block == Blocks.GRASS
            || block == Blocks.TALL_GRASS
            || block == Blocks.FERN
            || block == Blocks.LARGE_FERN
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
        String status = switch (phase) {
            case IDLE -> "空闲";
            case WEEDING -> "除杂草 " + (currentIndex + 1) + "/" + weedTargets.size();
            case HOEING -> "锄地 " + (currentIndex + 1) + "/" + hoeTargets.size();
            case PLANTING -> "种植 " + (currentIndex + 1) + "/" + plantTargets.size();
            case DONE -> "完成";
        };
        return "范围: " + range.get() + " | " + status;
    }
}
