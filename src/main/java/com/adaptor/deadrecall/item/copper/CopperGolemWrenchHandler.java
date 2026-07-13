package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.advancement.ModCriteriaTriggers;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import com.adaptor.deadrecall.network.CopperGolemFuelSlotPayload;
import com.adaptor.deadrecall.network.CopperGolemGatheringSlotPayload;
import com.adaptor.deadrecall.network.CopperGolemGatheringTargetPayload;
import com.adaptor.deadrecall.network.CopperGolemVisualizationPayload;
import com.adaptor.deadrecall.network.CopperWrenchBindingsPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CopperGolemWrenchHandler {
    private static final String TAG_SELECTED_GOLEM = "deadrecall_selected_golem";

    private static final String TAG_BOUND_CONTAINER_DIM = "deadrecall_bound_container_dim";
    private static final String TAG_BOUND_CONTAINER_X = "deadrecall_bound_container_x";
    private static final String TAG_BOUND_CONTAINER_Y = "deadrecall_bound_container_y";
    private static final String TAG_BOUND_CONTAINER_Z = "deadrecall_bound_container_z";
    private static final String TAG_BOUND_CONTAINERS = "deadrecall_bound_containers";
    private static final String TAG_SOURCE_COPPER_CONTAINER_DIM = "deadrecall_source_copper_container_dim";
    private static final String TAG_SOURCE_COPPER_CONTAINER_X = "deadrecall_source_copper_container_x";
    private static final String TAG_SOURCE_COPPER_CONTAINER_Y = "deadrecall_source_copper_container_y";
    private static final String TAG_SOURCE_COPPER_CONTAINER_Z = "deadrecall_source_copper_container_z";
    private static final String TAG_GATHERING_AREA_DIM = "deadrecall_gathering_area_dim";
    private static final String TAG_GATHERING_CORNER_A_X = "deadrecall_gathering_corner_a_x";
    private static final String TAG_GATHERING_CORNER_A_Y = "deadrecall_gathering_corner_a_y";
    private static final String TAG_GATHERING_CORNER_A_Z = "deadrecall_gathering_corner_a_z";
    private static final String TAG_GATHERING_CORNER_B_X = "deadrecall_gathering_corner_b_x";
    private static final String TAG_GATHERING_CORNER_B_Y = "deadrecall_gathering_corner_b_y";
    private static final String TAG_GATHERING_CORNER_B_Z = "deadrecall_gathering_corner_b_z";
    private static final String TAG_GATHERING_MANUAL_TARGETS = "deadrecall_gathering_manual_targets";
    private static final String TAG_GATHERING_TARGET_X = "deadrecall_gathering_target_x";
    private static final String TAG_GATHERING_TARGET_Y = "deadrecall_gathering_target_y";
    private static final String TAG_GATHERING_TARGET_Z = "deadrecall_gathering_target_z";
    private static final String TAG_GATHERING_BREAK_TICKS = "deadrecall_gathering_break_ticks";
    private static final String TAG_GATHERING_BREAK_REQUIRED_TICKS = "deadrecall_gathering_break_required_ticks";
    private static final String TAG_GATHERING_BREAK_STATE = "deadrecall_gathering_break_state";
    private static final String TAG_GATHERING_SCAN_INDEX = "deadrecall_gathering_scan_index";
    private static final String TAG_GATHERING_NEAREST_SCAN_RADIUS = "deadrecall_gathering_nearest_scan_radius";
    private static final String TAG_GATHERING_NEAREST_SCAN_CURSOR = "deadrecall_gathering_nearest_scan_cursor";
    private static final String TAG_GATHERING_SKIPPED_TARGETS = "deadrecall_gathering_skipped_targets";
    private static final String TAG_GATHERING_MOVE_BEST_DISTANCE = "deadrecall_gathering_move_best_distance";
    private static final String TAG_GATHERING_MOVE_STUCK_TICKS = "deadrecall_gathering_move_stuck_ticks";
    private static final String TAG_GATHERING_LLM_WARMUP_INDEX = "deadrecall_gathering_llm_warmup_index";
    private static final String TAG_GATHERING_RETRY_TICK = "deadrecall_gathering_retry_tick";
    private static final String TAG_BINDING_DIM = "dimension";
    private static final String TAG_BINDING_X = "x";
    private static final String TAG_BINDING_Y = "y";
    private static final String TAG_BINDING_Z = "z";
    private static final String TAG_SOURCE_CONTAINER_DIM = "deadrecall_source_container_dim";
    private static final String TAG_SOURCE_CONTAINER_X = "deadrecall_source_container_x";
    private static final String TAG_SOURCE_CONTAINER_Y = "deadrecall_source_container_y";
    private static final String TAG_SOURCE_CONTAINER_Z = "deadrecall_source_container_z";
    private static final String TAG_SOURCE_SLOT = "deadrecall_source_slot";
    private static final String TAG_TRIED_DESTINATIONS = "deadrecall_tried_destinations";
    private static final String TAG_TRANSPORT_ENABLED = "deadrecall_transport_enabled";
    private static final String TAG_DATA_VERSION = "deadrecall_data_version";
    private static final String TAG_REVISION = "deadrecall_revision";
    private static final String TAG_MODE = "deadrecall_mode";
    private static final String TAG_ACTIVITY = "deadrecall_activity";
    private static final String TAG_SORTING_BLOCKED = "deadrecall_sorting_blocked";
    private static final String TAG_BLOCKED_SOURCE_CONTAINER_DIM = "deadrecall_blocked_source_container_dim";
    private static final String TAG_BLOCKED_SOURCE_CONTAINER_X = "deadrecall_blocked_source_container_x";
    private static final String TAG_BLOCKED_SOURCE_CONTAINER_Y = "deadrecall_blocked_source_container_y";
    private static final String TAG_BLOCKED_SOURCE_CONTAINER_Z = "deadrecall_blocked_source_container_z";
    private static final String TAG_BLOCKED_SOURCE_HASH = "deadrecall_blocked_source_hash";
    private static final String TAG_BLOCKED_BINDINGS_HASH = "deadrecall_blocked_bindings_hash";
    private static final String TAG_BLOCKED_TARGETS_HASH = "deadrecall_blocked_targets_hash";
    private static final String TAG_LLM_BINDINGS = "deadrecall_llm_bindings";
    private static final String TAG_LLM_API_URL = "deadrecall_llm_api_url";
    private static final String TAG_LLM_API_KEY = "deadrecall_llm_api_key";
    private static final String TAG_LLM_MODEL = "deadrecall_llm_model";
    private static final String TAG_GATHERING_LLM_ENABLED = "deadrecall_gathering_llm_enabled";
    private static final String TAG_GATHERING_LLM_PROMPT = "deadrecall_gathering_llm_prompt";
    private static final String TAG_GATHERING_LLM_PROMPT_REVISION = "deadrecall_gathering_llm_prompt_revision";
    private static final String TAG_GATHERING_LLM_ALLOWED_BLOCK_IDS = "deadrecall_gathering_llm_allowed_block_ids";
    private static final String TAG_GATHERING_LLM_DENIED_BLOCK_IDS = "deadrecall_gathering_llm_denied_block_ids";
    private static final String TAG_GATHERING_LLM_ALLOWED_TAGS = "deadrecall_gathering_llm_allowed_tags";
    private static final String TAG_GATHERING_LLM_DENIED_TAGS = "deadrecall_gathering_llm_denied_tags";
    private static final String TAG_LLM_ENABLED = "llm_enabled";
    private static final String TAG_LLM_PROMPT = "llm_prompt";
    private static final String TAG_LLM_ALLOWED_ITEM_IDS = "llm_allowed_item_ids";
    private static final String TAG_LLM_DENIED_ITEM_IDS = "llm_denied_item_ids";
    private static final String TAG_LLM_ALLOWED_TAGS = "llm_allowed_tags";
    private static final String TAG_LLM_DENIED_TAGS = "llm_denied_tags";
    private static final String TAG_FUEL_STACK = "deadrecall_fuel_stack";
    private static final String TAG_FUEL_TICKS = "deadrecall_fuel_ticks";
    private static final String TAG_GATHERING_TOOL_STACK = "deadrecall_gathering_tool_stack";
    private static final String TAG_GATHERING_STORAGE_STACK = "deadrecall_gathering_storage_stack";
    private static final int TRANSPORTED_ITEM_MAX_STACK_SIZE = 16;
    private static final int FUEL_TICKS_PER_TRANSPORT = 200;
    private static final float COPPER_GOLEM_REPAIR_AMOUNT = 4.0F;
    private static final int GATHERING_MANUAL_TARGET_LIMIT = 64;
    private static final int GATHERING_TARGET_CLICK_DEBOUNCE_TICKS = 8;
    private static final int GATHERING_SCAN_BUDGET_PER_TICK = 512;
    private static final int GATHERING_LLM_WARMUP_BUDGET_PER_TICK = 16;
    private static final int GATHERING_UNREACHABLE_SKIP_TICKS = 80;
    private static final int GATHERING_SKIPPED_TARGET_LIMIT = 128;
    private static final int GATHERING_UPWARD_REACH_HORIZONTAL = 1;
    private static final int GATHERING_UPWARD_REACH_HEIGHT = 2;
    private static final int GATHERING_DOWNWARD_REACH_HORIZONTAL = 1;
    private static final int GATHERING_DOWNWARD_REACH_DEPTH = 2;
    private static final int GATHERING_MAX_AXIS_LENGTH = 64;
    private static final long GATHERING_MAX_VOLUME = 262_144L;
    private static final long GATHERING_MOVE_DISTANCE_SCALE = 1000L;
    private static final long GATHERING_MOVE_PROGRESS_THRESHOLD = 250L;
    private static final int GATHERING_MIN_VISIBLE_BREAK_TICKS = 8;
    private static final float GATHERING_TOOL_EFFICIENCY_MULTIPLIER = 0.5F;
    private static final int LLM_CACHE_VALUE_LIMIT = 128;
    private static final int GATHERING_RETRY_TICKS = 100;
    private static final double GATHERING_BREAK_REACH_DISTANCE_SQR = 4.0D;
    private static final double GATHERING_DEPOSIT_REACH_DISTANCE_SQR = 6.25D;
    private static final double GATHERING_NAVIGATION_SPEED = 0.75D;
    private static final double UI_MANAGEMENT_DISTANCE_SQR = 64.0D * 64.0D;
    private static final int DATA_VERSION = 2;
    private static final int SORTING_BLOCKED_JUMP_INTERVAL_TICKS = 10;
    private static final int PRUNE_BINDINGS_INTERVAL_TICKS = 20;
    private static final Map<GatheringTargetClickKey, Long> RECENT_GATHERING_TARGET_CLICKS = new HashMap<>();
    private static int pruneBindingsTicker = 0;

    private CopperGolemWrenchHandler() {
    }

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, clickedPos, direction) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isSpectator() || !stack.is(ModItems.COPPER_WRENCH)) {
                return InteractionResult.PASS;
            }

            BlockEntity blockEntity = world.getBlockEntity(clickedPos);
            UUID selectedGolem = getSelectedGolem(stack);
            if (selectedGolem == null) {
                if (blockEntity instanceof Container && !world.isClientSide()) {
                    notify(player, Component.translatable("message.deadrecall.copper_wrench.left_click_select"));
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            ServerLevel serverLevel = (ServerLevel) world;
            Entity selectedEntity = serverLevel.getEntityInAnyDimension(selectedGolem);
            if (!(selectedEntity instanceof CopperGolem golem)) {
                clearSelection(stack);
                notify(player, Component.translatable("message.deadrecall.copper_wrench.golem_unavailable"));
                return InteractionResult.SUCCESS;
            }

            if (!golem.level().dimension().equals(world.dimension())) {
                clearSelection(stack);
                notify(player, Component.translatable("message.deadrecall.copper_wrench.target_other_dimension"));
                return InteractionResult.SUCCESS;
            }

            Binding binding = new Binding(world.dimension(), clickedPos.immutable());
            if (isCopperSourceContainer(world, clickedPos)) {
                boolean removed = removeSourceContainer(golem, binding);
                notify(player, Component.translatable(removed
                        ? "message.deadrecall.copper_wrench.source_unbind_success"
                        : "message.deadrecall.copper_wrench.source_unbind_missing"));
            } else if (getMode(golem) == CopperGolemMode.GATHERING) {
                if (blockEntity instanceof Container) {
                    notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_target_reject_container"));
                    return InteractionResult.SUCCESS;
                }

                Identifier blockId = BuiltInRegistries.BLOCK.getKey(world.getBlockState(clickedPos).getBlock());
                if (shouldIgnoreGatheringTargetClick(serverLevel, player, golem.getUUID(), clickedPos, blockId)) {
                    return InteractionResult.SUCCESS;
                }

                boolean added = toggleGatheringManualTarget(golem, blockId);
                notify(player, Component.translatable(added
                                ? "message.deadrecall.copper_wrench.gathering_target_added"
                                : "message.deadrecall.copper_wrench.gathering_target_removed",
                        blockId.toString()));
            } else if (blockEntity instanceof Container) {
                boolean removed = removeBinding(golem, binding);
                notify(player, Component.translatable(removed
                        ? "message.deadrecall.copper_wrench.unbind_success"
                        : "message.deadrecall.copper_wrench.unbind_missing"));
            } else {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!isCopperGolem(entity)) {
                return InteractionResult.PASS;
            }

            CopperGolem golem = (CopperGolem) entity;
            if (stack.is(Items.COPPER_INGOT)) {
                return repairCopperGolem(player, world, hand, golem);
            }

            if (stack.isEmpty() && isVirtualGatheringDisplayedItem(golem)) {
                return InteractionResult.SUCCESS;
            }

            if (!stack.is(ModItems.COPPER_WRENCH)) {
                return InteractionResult.PASS;
            }

            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            setSelectedGolem(stack, golem.getUUID());
            if (player instanceof ServerPlayer serverPlayer) {
                sendBindingListUi(serverPlayer, golem);
            }
            return InteractionResult.SUCCESS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.COPPER_WRENCH)) {
                return InteractionResult.PASS;
            }

            UUID selectedGolem = getSelectedGolem(stack);
            if (selectedGolem == null) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            BlockPos clickedPos = hitResult.getBlockPos();
            ServerLevel serverLevel = (ServerLevel) world;
            Entity selectedEntity = serverLevel.getEntityInAnyDimension(selectedGolem);
            if (!(selectedEntity instanceof CopperGolem golem)) {
                clearSelection(stack);
                notify(player, Component.translatable("message.deadrecall.copper_wrench.golem_unavailable"));
                return InteractionResult.SUCCESS;
            }

            if (!golem.level().dimension().equals(world.dimension())) {
                clearSelection(stack);
                notify(player, Component.translatable("message.deadrecall.copper_wrench.target_other_dimension"));
                return InteractionResult.SUCCESS;
            }

            Binding binding = new Binding(world.dimension(), clickedPos.immutable());
            if (isCopperSourceContainer(world, clickedPos)) {
                boolean changed = setSourceContainer(golem, binding);
                if (player instanceof ServerPlayer serverPlayer) {
                    ModCriteriaTriggers.FIRST_COPPER_GOLEM_BINDING.trigger(serverPlayer);
                }
                showParticlePath(serverLevel, golem, clickedPos);
                notify(player, Component.translatable(changed
                        ? "message.deadrecall.copper_wrench.source_bind_success"
                        : "message.deadrecall.copper_wrench.source_bind_duplicate"));
                return InteractionResult.SUCCESS;
            }

            BlockEntity blockEntity = world.getBlockEntity(clickedPos);
            CopperGolemMode mode = getMode(golem);
            if (mode == CopperGolemMode.GATHERING) {
                if (blockEntity instanceof Container) {
                    notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_container_binding_disabled"));
                    return InteractionResult.SUCCESS;
                }

                boolean areaChanged = setGatheringAreaCorner(golem, serverLevel, clickedPos, player.isShiftKeyDown());
                if (!areaChanged) {
                    notify(player, Component.translatable(
                            "message.deadrecall.copper_wrench.gathering_area_too_large",
                            GATHERING_MAX_AXIS_LENGTH,
                            GATHERING_MAX_VOLUME));
                    return InteractionResult.SUCCESS;
                }
                notify(player, Component.translatable(player.isShiftKeyDown()
                        ? "message.deadrecall.copper_wrench.gathering_corner_b_set"
                        : "message.deadrecall.copper_wrench.gathering_corner_a_set",
                        clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()));
                return InteractionResult.SUCCESS;
            }

            if (!(blockEntity instanceof Container)) {
                notify(player, Component.translatable("message.deadrecall.copper_wrench.need_container"));
                return InteractionResult.SUCCESS;
            }

            if (mode != CopperGolemMode.SORTING) {
                notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_container_binding_disabled"));
                return InteractionResult.SUCCESS;
            }

            boolean added = addBinding(golem, binding);
            if (added && player instanceof ServerPlayer serverPlayer) {
                ModCriteriaTriggers.FIRST_COPPER_GOLEM_BINDING.trigger(serverPlayer);
            }
            showParticlePath(serverLevel, golem, clickedPos);
            notify(player, Component.translatable(added
                    ? "message.deadrecall.copper_wrench.bind_success"
                    : "message.deadrecall.copper_wrench.bind_duplicate"));
            return InteractionResult.SUCCESS;
        });
    }

    private static InteractionResult repairCopperGolem(Player player, Level world, net.minecraft.world.InteractionHand hand, CopperGolem golem) {
        if (!golem.isAlive() || golem.getHealth() >= golem.getMaxHealth()) {
            return InteractionResult.PASS;
        }

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        float beforeHealth = golem.getHealth();
        golem.heal(COPPER_GOLEM_REPAIR_AMOUNT);
        float repaired = golem.getHealth() - beforeHealth;
        if (repaired <= 0.0F) {
            return InteractionResult.PASS;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.WAX_ON,
                    golem.getX(),
                    golem.getY() + golem.getBbHeight() * 0.65D,
                    golem.getZ(),
                    8,
                    0.25D,
                    0.35D,
                    0.25D,
                    0.02D
            );
        }
        notify(player, Component.translatable("message.deadrecall.copper_wrench.golem_repaired"));
        return InteractionResult.SUCCESS;
    }

    public static Optional<Binding> getBinding(CopperGolem golem) {
        List<Binding> bindings = getBindings(golem);
        return bindings.isEmpty() ? Optional.empty() : Optional.of(bindings.getFirst());
    }

    public static List<Binding> getBindings(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        return readBindings(tag);
    }

    public static boolean hasBinding(CopperGolem golem) {
        return !getBindings(golem).isEmpty();
    }

    public static Optional<Binding> getSourceContainer(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        return readBinding(tag, TAG_SOURCE_COPPER_CONTAINER_DIM, TAG_SOURCE_COPPER_CONTAINER_X, TAG_SOURCE_COPPER_CONTAINER_Y, TAG_SOURCE_COPPER_CONTAINER_Z);
    }

    public static boolean hasSourceContainer(CopperGolem golem) {
        return getSourceContainer(golem).isPresent();
    }

    public static boolean isSourceContainer(CopperGolem golem, Level level, BlockPos pos) {
        return getSourceContainer(golem)
                .filter(binding -> binding.dimension().equals(level.dimension()) && binding.containerPos().equals(pos))
                .isPresent();
    }

    public static boolean isTransportEnabled(CopperGolem golem) {
        return getEntityCustomDataTag(golem).getBooleanOr(TAG_TRANSPORT_ENABLED, true);
    }

    public static CopperGolemMode getMode(CopperGolem golem) {
        migrateGolemData(golem);
        return CopperGolemMode.fromId(getEntityCustomDataTag(golem).getStringOr(TAG_MODE, CopperGolemMode.SORTING.id()));
    }

    public static boolean isSortingMode(CopperGolem golem) {
        return getMode(golem) == CopperGolemMode.SORTING;
    }

    public static boolean isSortingBlocked(CopperGolem golem) {
        return getEntityCustomDataTag(golem).getBooleanOr(TAG_SORTING_BLOCKED, false);
    }

    public static boolean hasFuelAvailable(CopperGolem golem, ServerLevel level) {
        return getFuelTicks(golem) > 0 || isFuel(level, getFuelStack(golem));
    }

    private static Optional<CopperGolem> resolveUiGolem(ServerPlayer player, UUID golemId, int clientRevision) {
        Entity entity = player.level().getEntityInAnyDimension(golemId);
        if (!(entity instanceof CopperGolem golem)) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.golem_unavailable"));
            return Optional.empty();
        }

        migrateGolemData(golem);
        if (!isPlayerHoldingBoundWrench(player, golemId)) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.ui_invalid_wrench"));
            return Optional.empty();
        }

        if (!golem.level().dimension().equals(player.level().dimension())) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.target_other_dimension"));
            return Optional.empty();
        }

        if (player.distanceToSqr(golem) > UI_MANAGEMENT_DISTANCE_SQR) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.golem_too_far"));
            sendBindingListUi(player, golem);
            return Optional.empty();
        }

        int serverRevision = getRevision(golem);
        if (clientRevision != serverRevision) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.ui_stale"));
            sendBindingListUi(player, golem);
            return Optional.empty();
        }

        return Optional.of(golem);
    }

    private static boolean isPlayerHoldingBoundWrench(ServerPlayer player, UUID golemId) {
        return isBoundWrench(player.getMainHandItem(), golemId)
                || isBoundWrench(player.getOffhandItem(), golemId);
    }

    private static boolean isBoundWrench(ItemStack stack, UUID golemId) {
        return stack.is(ModItems.COPPER_WRENCH) && golemId.equals(getSelectedGolem(stack));
    }

    private static void migrateGolemData(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        boolean changed = false;
        if (tag.getIntOr(TAG_DATA_VERSION, 0) < DATA_VERSION) {
            tag.putInt(TAG_DATA_VERSION, DATA_VERSION);
            changed = true;
        }
        if (!tag.contains(TAG_MODE)) {
            tag.putString(TAG_MODE, CopperGolemMode.SORTING.id());
            changed = true;
        }
        if (!tag.contains(TAG_REVISION)) {
            tag.putInt(TAG_REVISION, 0);
            changed = true;
        }
        if (changed) {
            setEntityCustomDataTag(golem, tag);
        }
    }

    private static int getRevision(CopperGolem golem) {
        return getEntityCustomDataTag(golem).getIntOr(TAG_REVISION, 0);
    }

    private static void bumpRevision(CompoundTag tag) {
        tag.putInt(TAG_REVISION, tag.getIntOr(TAG_REVISION, 0) + 1);
        tag.putInt(TAG_DATA_VERSION, DATA_VERSION);
    }

    private static void bumpGolemRevision(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
    }

    public static void setTransportEnabledFromUi(ServerPlayer player, UUID golemId, boolean enabled, int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }
        CopperGolem golem = resolved.get();

        setTransportEnabled(golem, enabled);
        sendBindingListUi(player, golem);
    }

    public static void setModeFromUi(ServerPlayer player, UUID golemId, String modeId, int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }
        CopperGolem golem = resolved.get();

        CopperGolemMode requestedMode = CopperGolemMode.fromId(modeId);
        if (requestedMode == getMode(golem)) {
            sendBindingListUi(player, golem);
            return;
        }

        if (!canSwitchMode(player, golem, requestedMode)) {
            sendBindingListUi(player, golem);
            return;
        }

        setMode(golem, requestedMode);
        notify(player, Component.translatable(
                "message.deadrecall.copper_wrench.mode_switch_success",
                Component.translatable(modeTranslationKey(requestedMode))));
        sendBindingListUi(player, golem);
    }

    public static void setBindingLlmFromUi(ServerPlayer player, UUID golemId, String dimensionId, int x, int y, int z, boolean enabled, String prompt, int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }
        CopperGolem golem = resolved.get();

        Identifier dimension = Identifier.tryParse(dimensionId);
        if (dimension == null) {
            return;
        }

        Binding binding = new Binding(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimension), new BlockPos(x, y, z));
        if (!getBindings(golem).contains(binding)) {
            return;
        }

        setBindingLlmConfig(golem, binding, enabled, prompt);
        sendBindingListUi(player, golem);
    }

    public static void moveBindingLlmCacheFromUi(
            ServerPlayer player,
            UUID golemId,
            String dimensionId,
            int x,
            int y,
            int z,
            String value,
            boolean tagValue,
            boolean allowed,
            int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }
        CopperGolem golem = resolved.get();

        Identifier dimension = Identifier.tryParse(dimensionId);
        if (dimension == null || value == null || value.isBlank()) {
            return;
        }

        Binding binding = new Binding(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimension), new BlockPos(x, y, z));
        if (!getBindings(golem).contains(binding)) {
            return;
        }

        moveBindingLlmCache(golem, binding, value.trim(), tagValue, allowed);
        sendBindingListUi(player, golem);
    }

    public static void setGolemLlmConfigFromUi(ServerPlayer player, UUID golemId, String apiUrl, String apiKey, String model, int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }
        CopperGolem golem = resolved.get();

        setGolemLlmConfig(golem, apiUrl, apiKey, model);
        sendBindingListUi(player, golem);
    }

    public static void handleFuelSlotFromUi(ServerPlayer player, UUID golemId, CopperGolemFuelSlotPayload.Action action, int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }
        CopperGolem golem = resolved.get();

        if (action == CopperGolemFuelSlotPayload.Action.INSERT_MAIN_HAND) {
            insertFuelFromAvailableInventory(player, golem);
        } else if (action == CopperGolemFuelSlotPayload.Action.TAKE_ALL) {
            takeFuel(player, golem);
        }

        sendBindingListUi(player, golem);
    }

    public static void handleGatheringSlotFromUi(
            ServerPlayer player,
            UUID golemId,
            CopperGolemGatheringSlotPayload.Slot slot,
            CopperGolemGatheringSlotPayload.Action action,
            int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }
        CopperGolem golem = resolved.get();

        if (getMode(golem) != CopperGolemMode.GATHERING) {
            sendBindingListUi(player, golem);
            return;
        }

        if (isTransportEnabled(golem)) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_slot_stop_first"));
            sendBindingListUi(player, golem);
            return;
        }

        if (slot == CopperGolemGatheringSlotPayload.Slot.TOOL) {
            if (action == CopperGolemGatheringSlotPayload.Action.INSERT_MAIN_HAND) {
                insertGatheringToolFromAvailableInventory(player, golem);
            } else if (action == CopperGolemGatheringSlotPayload.Action.TAKE_ALL) {
                takeGatheringTool(player, golem);
            }
        } else if (slot == CopperGolemGatheringSlotPayload.Slot.STORAGE
                && action == CopperGolemGatheringSlotPayload.Action.TAKE_ALL) {
            takeGatheringStorage(player, golem);
        }

        sendBindingListUi(player, golem);
    }

    public static void handleGatheringTargetFromUi(
            ServerPlayer player,
            UUID golemId,
            String value,
            boolean tag,
            CopperGolemGatheringTargetPayload.TargetSet targetSet,
            CopperGolemGatheringTargetPayload.Action action,
            int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }
        CopperGolem golem = resolved.get();

        if (getMode(golem) != CopperGolemMode.GATHERING) {
            sendBindingListUi(player, golem);
            return;
        }

        if (action == CopperGolemGatheringTargetPayload.Action.REMOVE) {
            if (targetSet == CopperGolemGatheringTargetPayload.TargetSet.MANUAL) {
                Identifier identifier = Identifier.tryParse(value);
                if (identifier == null || tag || BuiltInRegistries.BLOCK.getOptional(identifier).isEmpty()) {
                    sendBindingListUi(player, golem);
                    return;
                }

                boolean removed = removeGatheringManualTarget(golem, identifier);
                notify(player, Component.translatable(removed
                        ? "message.deadrecall.copper_wrench.gathering_target_removed"
                        : "message.deadrecall.copper_wrench.gathering_target_remove_missing",
                        identifier.toString()));
            } else {
                boolean removed = removeGatheringLlmCache(golem, value, tag, targetSet == CopperGolemGatheringTargetPayload.TargetSet.ALLOWED);
                notify(player, Component.translatable(removed
                        ? "message.deadrecall.copper_wrench.gathering_target_removed"
                        : "message.deadrecall.copper_wrench.gathering_target_remove_missing",
                        value));
            }
        }

        sendBindingListUi(player, golem);
    }

    public static void setGatheringLlmFromUi(ServerPlayer player, UUID golemId, boolean enabled, String prompt, int revision) {
        Optional<CopperGolem> resolved = resolveUiGolem(player, golemId, revision);
        if (resolved.isEmpty()) {
            return;
        }

        CopperGolem golem = resolved.get();
        if (getMode(golem) != CopperGolemMode.GATHERING) {
            sendBindingListUi(player, golem);
            return;
        }

        setGatheringLlmConfig(golem, enabled, prompt);
        sendBindingListUi(player, golem);
    }

    public static void sendVisualization(ServerPlayer player, UUID golemId) {
        Entity entity = player.level().getEntityInAnyDimension(golemId);
        if (!(entity instanceof CopperGolem golem)
                || !isPlayerHoldingBoundWrench(player, golemId)
                || !golem.level().dimension().equals(player.level().dimension())) {
            ServerPlayNetworking.send(player, new CopperGolemVisualizationPayload(
                    golemId,
                    false,
                    player.level().dimension().identifier().toString(),
                    0.0D,
                    0.0D,
                    0.0D,
                    "",
                    "",
                    null,
                    null,
                    null,
                    List.of()
            ));
            return;
        }

        migrateGolemData(golem);
        MinecraftServer server = player.level().getServer();
        List<CopperGolemVisualizationPayload.PosEntry> destinations = new ArrayList<>();
        if (server != null) {
            for (Binding binding : getBindings(golem)) {
                destinations.add(createVisualizationPos(server, binding));
            }
        }

        Optional<BlockPos> gatheringTarget = getMode(golem) == CopperGolemMode.GATHERING ? getGatheringTarget(golem) : Optional.empty();
        ServerPlayNetworking.send(player, new CopperGolemVisualizationPayload(
                golem.getUUID(),
                true,
                golem.level().dimension().identifier().toString(),
                golem.getX(),
                golem.getY(),
                golem.getZ(),
                getMode(golem).id(),
                getActivity(golem, (ServerLevel) player.level()).id(),
                server == null ? null : getSourceContainer(golem)
                        .map(binding -> createVisualizationPos(server, binding))
                        .orElse(null),
                createVisualizationArea(golem),
                gatheringTarget
                        .map(pos -> new CopperGolemVisualizationPayload.PosEntry(
                                golem.level().dimension().identifier().toString(),
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                true))
                        .orElse(null),
                destinations
        ));
    }

    public static void dropGatheringInventory(CopperGolem golem) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return;
        }

        clearGatheringDisplayedItem(golem);
        ItemStack toolStack = getGatheringToolStack(golem);
        ItemStack storageStack = getGatheringStorageStack(golem);
        if (toolStack.isEmpty() && storageStack.isEmpty()) {
            return;
        }

        setGatheringToolStack(golem, ItemStack.EMPTY);
        setGatheringStorageStack(golem, ItemStack.EMPTY);
        dropStackAtGolem(level, golem, toolStack);
        dropStackAtGolem(level, golem, storageStack);
    }

    private static void dropStackAtGolem(ServerLevel level, CopperGolem golem, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEntity itemEntity = new ItemEntity(level, golem.getX(), golem.getY() + 0.25D, golem.getZ(), stack.copy());
        itemEntity.setDeltaMovement(
                (golem.getRandom().nextDouble() - 0.5D) * 0.12D,
                0.18D,
                (golem.getRandom().nextDouble() - 0.5D) * 0.12D);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    public static void recordLlmDecision(CopperGolem golem, Binding binding, String itemId, List<String> itemTags, boolean allowed, List<String> acceptedTags) {
        if (!getBindings(golem).contains(binding)) {
            return;
        }

        CompoundTag tag = getEntityCustomDataTag(golem);
        List<BindingLlmConfig> configs = new ArrayList<>(readBindingLlmConfigs(tag));
        BindingLlmConfig config = getBindingLlmConfig(configs, binding);

        List<String> allowedItemIds = new ArrayList<>(config.allowedItemIds());
        List<String> deniedItemIds = new ArrayList<>(config.deniedItemIds());
        List<String> allowedTags = new ArrayList<>(config.allowedTags());
        List<String> deniedTags = new ArrayList<>(config.deniedTags());

        if (allowed) {
            addUnique(allowedItemIds, itemId);
            deniedItemIds.remove(itemId);
            for (String tagId : acceptedTags) {
                addUnique(allowedTags, tagId);
                deniedTags.remove(tagId);
            }
        } else {
            addUnique(deniedItemIds, itemId);
            allowedItemIds.remove(itemId);
            for (String tagId : acceptedTags) {
                addUnique(deniedTags, tagId);
                allowedTags.remove(tagId);
            }
        }

        putBindingLlmConfig(configs, new BindingLlmConfig(binding, config.enabled(), config.prompt(), allowedItemIds, deniedItemIds, allowedTags, deniedTags));
        writeBindingLlmConfigs(tag, configs);
        removeSortingBlockedTags(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
    }

    public static void tickCopperGolemWrenchState(MinecraftServer server) {
        pruneBindingsTicker++;
        boolean shouldPruneBindings = pruneBindingsTicker >= PRUNE_BINDINGS_INTERVAL_TICKS;
        if (shouldPruneBindings) {
            pruneBindingsTicker = 0;
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CopperGolem golem) {
                    if (shouldPruneBindings) {
                        pruneUnavailableSourceContainer(golem, server);
                        pruneUnavailableBindings(golem, server);
                    }
                    CopperGolemMode mode = getMode(golem);
                    if (mode == CopperGolemMode.SORTING && isTransportEnabled(golem) && isSortingBlocked(golem)) {
                        tickSortingBlockedGolem(golem, level);
                    } else if (mode == CopperGolemMode.GATHERING) {
                        if (isTransportEnabled(golem)) {
                            tickGatheringGolem(golem, level);
                        } else {
                            clearGatheringDisplayedItem(golem);
                            tickGatheringLlmWarmup(golem, level);
                        }
                    }
                }
            }
        }
    }

    public static boolean isBindingInLevel(CopperGolem golem, Level level) {
        for (Binding binding : getBindings(golem)) {
            if (binding.dimension().equals(level.dimension())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBoundContainer(CopperGolem golem, Level level, BlockPos pos) {
        for (Binding binding : getBindings(golem)) {
            if (binding.dimension().equals(level.dimension()) && binding.containerPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<TransportItemTarget> findNextDestinationTarget(CopperGolem golem, ServerLevel level, ItemStack carried) {
        if (carried.isEmpty()) {
            return Optional.empty();
        }

        List<Binding> triedDestinations = getTriedDestinations(golem);
        Optional<Source> source = getRememberedSource(golem);
        for (Binding binding : getBindings(golem)) {
            if (!binding.dimension().equals(level.dimension())
                    || triedDestinations.contains(binding)
                    || source.filter(value -> value.dimension().equals(binding.dimension()) && value.containerPos().equals(binding.containerPos())).isPresent()) {
                continue;
            }

            TransportItemTarget target = tryCreateBoundTarget(level, binding.containerPos());
            if (target == null || !canSortInto(golem, level, binding, target.container(), carried)) {
                rememberTriedDestination(golem, binding);
                continue;
            }

            rememberTriedDestination(golem, binding);
            return Optional.of(target);
        }

        return Optional.empty();
    }

    public static ItemStack pickUpNextItem(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        if (!hasFuelAvailable(golem, level)) {
            return ItemStack.EMPTY;
        }

        if (hasItems(source) && !hasAnySortableItem(golem, level, source, sourcePos)) {
            markSortingBlocked(golem, level, sourcePos, source);
            return ItemStack.EMPTY;
        }

        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack picked = source.removeItem(slot, Math.min(stack.getCount(), TRANSPORTED_ITEM_MAX_STACK_SIZE));
            if (!picked.isEmpty()) {
                rememberSource(golem, level, sourcePos, slot);
                consumeFuelForTransport(golem, level);
            }
            return picked;
        }

        return ItemStack.EMPTY;
    }

    public static boolean returnCarriedItemToSource(CopperGolem golem, ServerLevel level) {
        ItemStack carried = golem.getMainHandItem();
        if (carried.isEmpty()) {
            clearRememberedSource(golem);
            return true;
        }

        Optional<Source> source = getRememberedSource(golem);
        if (source.isEmpty() || !source.get().dimension().equals(level.dimension())) {
            return false;
        }

        TransportItemTarget target = TransportItemTarget.tryCreatePossibleTarget(source.get().containerPos(), level);
        if (target == null) {
            return false;
        }

        ItemStack remaining = moveCarriedStackToSourceBack(target.container(), carried, source.get().slot());
        target.container().setChanged();
        golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, remaining);
        if (remaining.isEmpty()) {
            clearRememberedSource(golem);
            return true;
        }

        return false;
    }

    public static Optional<ItemStack> putCarriedItemIntoDestination(CopperGolem golem, ServerLevel level, BlockPos targetPos, Container container) {
        ItemStack carried = golem.getMainHandItem();
        if (carried.isEmpty()) {
            return Optional.empty();
        }

        Optional<Binding> binding = getBindings(golem).stream()
                .filter(value -> value.dimension().equals(level.dimension()) && value.containerPos().equals(targetPos))
                .findFirst();
        if (binding.isEmpty()) {
            return Optional.empty();
        }

        BindingLlmConfig config = getBindingLlmConfig(golem, binding.get());
        Optional<Boolean> cachedDecision = getCachedLlmDecision(
                config,
                CopperGolemLlmService.itemId(carried),
                CopperGolemLlmService.itemTags(carried));
        if (cachedDecision.isPresent() && !cachedDecision.get()) {
            return Optional.of(carried);
        }

        ItemStack remaining = carried.copy();
        NestedBackpackTarget nestedTarget = findNestedBackpackTarget(golem, binding.get(), container, carried);
        if (nestedTarget != null) {
            remaining = insertIntoBackpack(nestedTarget.backpackStack(), remaining);
            container.setItem(nestedTarget.containerSlot(), nestedTarget.backpackStack());
        }

        if (!remaining.isEmpty()) {
            remaining = insertIntoDestinationContainer(golem, binding.get(), container, remaining);
        }

        if (remaining.getCount() < carried.getCount()) {
            container.setChanged();
        }
        return Optional.of(remaining);
    }

    private static ItemStack insertIntoDestinationContainer(CopperGolem golem, Binding binding, Container container, ItemStack carried) {
        if (carried.isEmpty()) {
            return ItemStack.EMPTY;
        }

        BindingLlmConfig config = getBindingLlmConfig(golem, binding);
        Optional<Boolean> cachedDecision = getCachedLlmDecision(
                config,
                CopperGolemLlmService.itemId(carried),
                CopperGolemLlmService.itemTags(carried));
        if (cachedDecision.isPresent() && !cachedDecision.get()) {
            return carried;
        }

        boolean hasMatchingItem = hasMatchingDestinationItem(container, carried);
        boolean allowEmptySlots = hasMatchingItem || cachedDecision.orElse(false);
        if (!hasMatchingItem && !allowEmptySlots) {
            return carried;
        }

        ItemStack remaining = carried.copy();
        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            mergeIntoSlot(container, slot, remaining);
        }

        if (allowEmptySlots) {
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                placeIntoEmptySlot(container, slot, remaining);
            }
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    private static boolean hasMatchingDestinationItem(Container container, ItemStack carried) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, carried) && container.canPlaceItem(slot, carried)) {
                return true;
            }
        }
        return false;
    }

    public static void clearRememberedSource(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        tag.remove(TAG_SOURCE_CONTAINER_DIM);
        tag.remove(TAG_SOURCE_CONTAINER_X);
        tag.remove(TAG_SOURCE_CONTAINER_Y);
        tag.remove(TAG_SOURCE_CONTAINER_Z);
        tag.remove(TAG_SOURCE_SLOT);
        tag.remove(TAG_TRIED_DESTINATIONS);
        setEntityCustomDataTag(golem, tag);
    }

    private static boolean addBinding(CopperGolem golem, Binding binding) {
        List<Binding> bindings = new ArrayList<>(getBindings(golem));
        if (bindings.contains(binding)) {
            return false;
        }

        bindings.add(binding);
        setBindings(golem, bindings);
        return true;
    }

    private static boolean removeBinding(CopperGolem golem, Binding binding) {
        List<Binding> bindings = new ArrayList<>(getBindings(golem));
        if (!bindings.remove(binding)) {
            return false;
        }

        setBindings(golem, bindings);
        return true;
    }

    private static boolean setSourceContainer(CopperGolem golem, Binding binding) {
        Optional<Binding> current = getSourceContainer(golem);
        if (current.filter(binding::equals).isPresent()) {
            return false;
        }

        CompoundTag tag = getEntityCustomDataTag(golem);
        writeBinding(tag, binding, TAG_SOURCE_COPPER_CONTAINER_DIM, TAG_SOURCE_COPPER_CONTAINER_X, TAG_SOURCE_COPPER_CONTAINER_Y, TAG_SOURCE_COPPER_CONTAINER_Z);
        removeSortingBlockedTags(tag);
        resetGatheringSearch(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);

        List<Binding> bindings = new ArrayList<>(getBindings(golem));
        if (bindings.remove(binding)) {
            setBindings(golem, bindings);
        } else {
            resetTransportMemories(golem);
        }
        return true;
    }

    private static boolean removeSourceContainer(CopperGolem golem, Binding binding) {
        Optional<Binding> current = getSourceContainer(golem);
        if (current.isEmpty() || !current.get().equals(binding)) {
            return false;
        }

        clearSourceContainer(golem);
        return true;
    }

    private static void clearSourceContainer(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        tag.remove(TAG_SOURCE_COPPER_CONTAINER_DIM);
        tag.remove(TAG_SOURCE_COPPER_CONTAINER_X);
        tag.remove(TAG_SOURCE_COPPER_CONTAINER_Y);
        tag.remove(TAG_SOURCE_COPPER_CONTAINER_Z);
        removeSortingBlockedTags(tag);
        resetGatheringSearch(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        if (golem.level() instanceof ServerLevel level) {
            returnCarriedItemToSource(golem, level);
        }
        resetTransportMemories(golem);
    }

    private static boolean setGatheringAreaCorner(CopperGolem golem, ServerLevel level, BlockPos pos, boolean cornerB) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        String dimensionId = level.dimension().identifier().toString();
        String existingDimension = tag.getStringOr(TAG_GATHERING_AREA_DIM, "");
        if (!existingDimension.isBlank() && !existingDimension.equals(dimensionId)) {
            clearGatheringAreaTags(tag);
        }

        Optional<BlockPos> currentCornerA = readBlockPos(tag, TAG_GATHERING_CORNER_A_X, TAG_GATHERING_CORNER_A_Y, TAG_GATHERING_CORNER_A_Z);
        Optional<BlockPos> currentCornerB = readBlockPos(tag, TAG_GATHERING_CORNER_B_X, TAG_GATHERING_CORNER_B_Y, TAG_GATHERING_CORNER_B_Z);
        Optional<BlockPos> nextCornerA = cornerB ? currentCornerA : Optional.of(pos);
        Optional<BlockPos> nextCornerB = cornerB ? Optional.of(pos) : currentCornerB;
        if (nextCornerA.isPresent()
                && nextCornerB.isPresent()
                && !isGatheringAreaWithinLimits(nextCornerA.get(), nextCornerB.get())) {
            return false;
        }

        tag.putString(TAG_GATHERING_AREA_DIM, dimensionId);
        if (cornerB) {
            writeBlockPos(tag, pos, TAG_GATHERING_CORNER_B_X, TAG_GATHERING_CORNER_B_Y, TAG_GATHERING_CORNER_B_Z);
        } else {
            writeBlockPos(tag, pos, TAG_GATHERING_CORNER_A_X, TAG_GATHERING_CORNER_A_Y, TAG_GATHERING_CORNER_A_Z);
        }
        resetGatheringSearch(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
        return true;
    }

    private static boolean hasCompleteGatheringArea(CopperGolem golem) {
        return getGatheringArea(golem)
                .filter(area -> area.cornerA().isPresent() && area.cornerB().isPresent())
                .isPresent();
    }

    private static Optional<GatheringArea> getGatheringArea(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        Identifier dimensionId = Identifier.tryParse(tag.getStringOr(TAG_GATHERING_AREA_DIM, ""));
        if (dimensionId == null) {
            return Optional.empty();
        }

        Optional<BlockPos> cornerA = readBlockPos(tag, TAG_GATHERING_CORNER_A_X, TAG_GATHERING_CORNER_A_Y, TAG_GATHERING_CORNER_A_Z);
        Optional<BlockPos> cornerB = readBlockPos(tag, TAG_GATHERING_CORNER_B_X, TAG_GATHERING_CORNER_B_Y, TAG_GATHERING_CORNER_B_Z);
        if (cornerA.isEmpty() && cornerB.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new GatheringArea(
                net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimensionId),
                cornerA,
                cornerB
        ));
    }

    private static boolean toggleGatheringManualTarget(CopperGolem golem, Identifier blockId) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        List<String> targets = new ArrayList<>(readGatheringManualTargets(tag));
        String targetId = blockId.toString();
        boolean added;
        if (targets.remove(targetId)) {
            added = false;
        } else {
            if (targets.size() >= GATHERING_MANUAL_TARGET_LIMIT) {
                targets.remove(0);
            }
            targets.add(targetId);
            added = true;
        }

        writeStringList(tag, TAG_GATHERING_MANUAL_TARGETS, targets);
        resetGatheringSearch(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
        return added;
    }

    private static boolean removeGatheringManualTarget(CopperGolem golem, Identifier blockId) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        List<String> targets = new ArrayList<>(readGatheringManualTargets(tag));
        boolean removed = targets.remove(blockId.toString());
        if (!removed) {
            return false;
        }

        writeStringList(tag, TAG_GATHERING_MANUAL_TARGETS, targets);
        resetGatheringSearch(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
        return true;
    }

    private static boolean shouldIgnoreGatheringTargetClick(ServerLevel level, Player player, UUID golemId, BlockPos pos, Identifier blockId) {
        long gameTime = level.getGameTime();
        if (RECENT_GATHERING_TARGET_CLICKS.size() > 256) {
            RECENT_GATHERING_TARGET_CLICKS.entrySet().removeIf(entry ->
                    gameTime - entry.getValue() > GATHERING_TARGET_CLICK_DEBOUNCE_TICKS);
        }

        GatheringTargetClickKey key = new GatheringTargetClickKey(
                player.getUUID(),
                golemId,
                level.dimension(),
                pos.immutable(),
                blockId.toString()
        );
        Long previousClickTime = RECENT_GATHERING_TARGET_CLICKS.put(key, gameTime);
        return previousClickTime != null && gameTime - previousClickTime <= GATHERING_TARGET_CLICK_DEBOUNCE_TICKS;
    }

    private static List<String> getGatheringManualTargets(CopperGolem golem) {
        return readGatheringManualTargets(getEntityCustomDataTag(golem));
    }

    private static List<String> readGatheringManualTargets(CompoundTag tag) {
        return readStringList(tag, TAG_GATHERING_MANUAL_TARGETS).stream()
                .limit(GATHERING_MANUAL_TARGET_LIMIT)
                .toList();
    }

    private static void setBindings(CopperGolem golem, List<Binding> bindings) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        writeBindings(tag, bindings);
        pruneBindingLlmConfigs(tag, bindings);
        removeSortingBlockedTags(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        if (bindings.isEmpty() && golem.level() instanceof ServerLevel level) {
            returnCarriedItemToSource(golem, level);
        }
        resetTransportMemories(golem);
    }

    private static void setTransportEnabled(CopperGolem golem, boolean enabled) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        tag.putBoolean(TAG_TRANSPORT_ENABLED, enabled);
        removeSortingBlockedTags(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        if (!enabled && golem.level() instanceof ServerLevel level) {
            returnCarriedItemToSource(golem, level);
            clearGatheringDisplayedItem(golem);
        }
        resetTransportMemories(golem);
    }

    private static boolean canSwitchMode(ServerPlayer player, CopperGolem golem, CopperGolemMode requestedMode) {
        if (isTransportEnabled(golem)) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.mode_switch_stop_first"));
            return false;
        }

        CopperGolemMode currentMode = getMode(golem);
        if (currentMode == CopperGolemMode.SORTING && requestedMode == CopperGolemMode.GATHERING && !golem.getMainHandItem().isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.mode_switch_carrying"));
            return false;
        }

        if (currentMode == CopperGolemMode.SORTING && requestedMode == CopperGolemMode.GATHERING && getRememberedSource(golem).isPresent()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.mode_switch_pending_source"));
            return false;
        }

        if (currentMode == CopperGolemMode.GATHERING && requestedMode == CopperGolemMode.SORTING) {
            if (!getGatheringToolStack(golem).isEmpty()) {
                notify(player, Component.translatable("message.deadrecall.copper_wrench.mode_switch_gathering_tool"));
                return false;
            }
            if (!getGatheringStorageStack(golem).isEmpty()) {
                notify(player, Component.translatable("message.deadrecall.copper_wrench.mode_switch_gathering_storage"));
                return false;
            }
            if (getGatheringTarget(golem).isPresent()) {
                notify(player, Component.translatable("message.deadrecall.copper_wrench.mode_switch_gathering_work"));
                return false;
            }
        }

        return true;
    }

    private static void setMode(CopperGolem golem, CopperGolemMode mode) {
        if (mode == CopperGolemMode.SORTING) {
            golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        CompoundTag tag = getEntityCustomDataTag(golem);
        tag.putString(TAG_MODE, mode.id());
        tag.remove(TAG_ACTIVITY);
        tag.remove(TAG_TRIED_DESTINATIONS);
        resetGatheringSearch(tag);
        removeSortingBlockedTags(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
    }

    private static CopperGolemActivity getActivity(CopperGolem golem, ServerLevel level) {
        CopperGolemActivity activity = deriveActivity(golem, level);
        CompoundTag tag = getEntityCustomDataTag(golem);
        tag.putString(TAG_ACTIVITY, activity.id());
        setEntityCustomDataTag(golem, tag);
        return activity;
    }

    private static CopperGolemActivity deriveActivity(CopperGolem golem, ServerLevel level) {
        if (!isTransportEnabled(golem)) {
            return CopperGolemActivity.STOPPED;
        }

        if (getSourceContainer(golem).isEmpty()) {
            return CopperGolemActivity.BLOCKED_NO_HOME;
        }

        CopperGolemMode mode = getMode(golem);
        if (mode == CopperGolemMode.SORTING) {
            if (isSortingBlocked(golem)) {
                return CopperGolemActivity.BLOCKED_SORTING;
            }
            if (golem.getMainHandItem().isEmpty() && !hasFuelAvailable(golem, level)) {
                return CopperGolemActivity.BLOCKED_NO_FUEL;
            }
            return golem.getMainHandItem().isEmpty()
                    ? CopperGolemActivity.SEARCHING
                    : CopperGolemActivity.MOVING_TO_TARGET;
        }

        ItemStack storageStack = getGatheringStorageStack(golem);
        if (!storageStack.isEmpty()) {
            CopperGolemActivity stored = CopperGolemActivity.fromId(getEntityCustomDataTag(golem).getStringOr(TAG_ACTIVITY, ""));
            if (stored == CopperGolemActivity.BLOCKED_HOME_UNAVAILABLE
                    || stored == CopperGolemActivity.BLOCKED_HOME_FULL
                    || stored == CopperGolemActivity.RETURNING_HOME
                    || stored == CopperGolemActivity.DEPOSITING) {
                return stored;
            }
            if (isGatheringStorageFull(storageStack)) {
                return CopperGolemActivity.RETURNING_HOME;
            }
        }

        if (getGatheringToolStack(golem).isEmpty()) {
            return CopperGolemActivity.BLOCKED_NO_TOOL;
        }

        if (!hasFuelAvailable(golem, level)) {
            return CopperGolemActivity.BLOCKED_NO_FUEL;
        }

        Optional<GatheringAreaBounds> bounds = getGatheringAreaBounds(golem, level);
        if (bounds.isEmpty()) {
            return CopperGolemActivity.BLOCKED_NO_AREA;
        }

        if (!hasGatheringTargetRules(golem)) {
            return CopperGolemActivity.BLOCKED_NO_VALID_TARGET;
        }

        CompoundTag tag = getEntityCustomDataTag(golem);
        CopperGolemActivity stored = CopperGolemActivity.fromId(tag.getStringOr(TAG_ACTIVITY, ""));
        if (stored == CopperGolemActivity.BLOCKED_HOME_UNAVAILABLE
                || stored == CopperGolemActivity.BLOCKED_HOME_FULL
                || stored == CopperGolemActivity.BLOCKED_NO_VALID_TARGET) {
            return stored;
        }

        Optional<BlockPos> target = getGatheringTarget(golem);
        if (target.isPresent()) {
            return isGolemCloseToGatheringTarget(golem, target.get())
                    ? CopperGolemActivity.WORKING
                    : CopperGolemActivity.MOVING_TO_TARGET;
        }

        return CopperGolemActivity.SEARCHING;
    }

    private static void tickGatheringGolem(CopperGolem golem, ServerLevel level) {
        Optional<GatheringHome> home = resolveGatheringHome(golem, level);
        if (home.isEmpty()) {
            setActivity(golem, getSourceContainer(golem).isPresent()
                    ? CopperGolemActivity.BLOCKED_HOME_UNAVAILABLE
                    : CopperGolemActivity.BLOCKED_NO_HOME);
            stopGatheringNavigation(golem);
            clearGatheringDisplayedItem(golem);
            return;
        }

        ItemStack storageStack = getGatheringStorageStack(golem);
        if (!storageStack.isEmpty() && shouldContinueGatheringDeposit(golem, storageStack)) {
            showGatheringStorageInHand(golem, storageStack);
            tickGatheringDeposit(golem, home.get(), storageStack);
            return;
        }

        if (!hasFuelAvailable(golem, level)) {
            setActivity(golem, CopperGolemActivity.BLOCKED_NO_FUEL);
            stopGatheringNavigation(golem);
            clearGatheringDisplayedItem(golem);
            return;
        }

        if (getGatheringToolStack(golem).isEmpty()) {
            setActivity(golem, CopperGolemActivity.BLOCKED_NO_TOOL);
            stopGatheringNavigation(golem);
            clearGatheringDisplayedItem(golem);
            return;
        }

        Optional<GatheringAreaBounds> bounds = getGatheringAreaBounds(golem, level);
        if (bounds.isEmpty()) {
            setActivity(golem, CopperGolemActivity.BLOCKED_NO_AREA);
            stopGatheringNavigation(golem);
            clearGatheringDisplayedItem(golem);
            return;
        }

        List<String> manualTargets = getGatheringManualTargets(golem);
        if (!hasGatheringTargetRules(golem)) {
            setActivity(golem, CopperGolemActivity.BLOCKED_NO_VALID_TARGET);
            stopGatheringNavigation(golem);
            clearGatheringDisplayedItem(golem);
            return;
        }

        Optional<BlockPos> target = getGatheringTarget(golem);
        if (target.isPresent()
                && !isValidGatheringTarget(golem, level, bounds.get(), home.get(), manualTargets, target.get())) {
            level.destroyBlockProgress(golem.getId(), target.get(), -1);
            clearGatheringTarget(golem);
            target = Optional.empty();
        }

        if (target.isEmpty()) {
            target = scanNextGatheringTarget(golem, level, bounds.get(), home.get(), manualTargets);
            if (target.isEmpty()) {
                stopGatheringNavigation(golem);
                if (!storageStack.isEmpty() && isGatheringBlockedNoValidTarget(golem)) {
                    showGatheringStorageInHand(golem, storageStack);
                    tickGatheringDeposit(golem, home.get(), storageStack);
                } else if (!isGatheringBlockedNoValidTarget(golem)) {
                    clearGatheringDisplayedItem(golem);
                } else {
                    clearGatheringDisplayedItem(golem);
                }
                return;
            }
        }

        BlockPos targetPos = target.get();
        if (!isGolemCloseToGatheringTarget(golem, targetPos)) {
            setActivity(golem, CopperGolemActivity.MOVING_TO_TARGET);
            clearGatheringDisplayedItem(golem);
            moveToGatheringTargetOrSkip(golem, level, targetPos);
            return;
        }

        setActivity(golem, CopperGolemActivity.WORKING);
        showGatheringToolInHand(golem);
        if (tickGatheringBreak(golem, level, bounds.get(), home.get(), manualTargets, targetPos)) {
            clearGatheringDisplayedItem(golem);
            setActivity(golem, CopperGolemActivity.SEARCHING);
        }
    }

    private static void moveToGatheringTargetOrSkip(CopperGolem golem, ServerLevel level, BlockPos targetPos) {
        List<BlockPos> destinations = gatheringMiningDestinations(level, golem, targetPos);
        if (destinations.isEmpty()) {
            skipUnreachableGatheringTarget(golem, level, targetPos);
            return;
        }

        boolean navigationStarted = false;
        for (BlockPos destination : destinations) {
            navigationStarted = golem.getNavigation().moveTo(
                    destination.getX() + 0.5D,
                    destination.getY(),
                    destination.getZ() + 0.5D,
                    GATHERING_NAVIGATION_SPEED
            );
            if (navigationStarted) {
                break;
            }
        }
        if (shouldSkipGatheringTargetForMovement(golem, targetPos, navigationStarted)) {
            skipUnreachableGatheringTarget(golem, level, targetPos);
        }
    }

    private static List<BlockPos> gatheringMiningDestinations(ServerLevel level, CopperGolem golem, BlockPos targetPos) {
        List<BlockPos> destinations = new ArrayList<>();
        addGatheringMiningDestination(level, golem, destinations, targetPos.above());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            addGatheringMiningDestination(level, golem, destinations, targetPos.above().relative(direction));
        }
        addGatheringMiningDestination(level, golem, destinations, targetPos.below(2));
        addGatheringMiningDestination(level, golem, destinations, targetPos.below());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            addGatheringMiningDestination(level, golem, destinations, targetPos.relative(direction));
            addGatheringMiningDestination(level, golem, destinations, targetPos.below().relative(direction));
            addGatheringMiningDestination(level, golem, destinations, targetPos.below(2).relative(direction));
        }

        destinations.sort((a, b) -> {
            int priority = Integer.compare(
                    gatheringDestinationPriority(targetPos, a),
                    gatheringDestinationPriority(targetPos, b)
            );
            if (priority != 0) {
                return priority;
            }

            return Double.compare(
                    golem.distanceToSqr(Vec3.atCenterOf(a)),
                    golem.distanceToSqr(Vec3.atCenterOf(b))
            );
        });
        return destinations;
    }

    private static int gatheringDestinationPriority(BlockPos targetPos, BlockPos destination) {
        if (destination.getY() > targetPos.getY()) {
            return 0;
        }
        if (destination.getY() == targetPos.getY()) {
            return 1;
        }
        return 2;
    }

    private static void addGatheringMiningDestination(ServerLevel level, CopperGolem golem, List<BlockPos> destinations, BlockPos pos) {
        if (!destinations.contains(pos) && canStandAtGatheringDestination(level, golem, pos)) {
            destinations.add(pos.immutable());
        }
    }

    private static boolean canStandAtGatheringDestination(ServerLevel level, CopperGolem golem, BlockPos pos) {
        BlockPos floorPos = pos.below();
        if (!level.isLoaded(pos) || !level.isLoaded(floorPos)) {
            return false;
        }

        BlockState floorState = level.getBlockState(floorPos);
        if (!floorState.isFaceSturdy(level, floorPos, Direction.UP)) {
            return false;
        }

        double halfWidth = Math.max(0.1D, golem.getBbWidth()) / 2.0D;
        double height = Math.max(0.1D, golem.getBbHeight());
        AABB standingBox = new AABB(
                pos.getX() + 0.5D - halfWidth,
                pos.getY(),
                pos.getZ() + 0.5D - halfWidth,
                pos.getX() + 0.5D + halfWidth,
                pos.getY() + height,
                pos.getZ() + 0.5D + halfWidth
        ).deflate(1.0E-7D);
        return level.noCollision(golem, standingBox);
    }

    private static boolean shouldSkipGatheringTargetForMovement(CopperGolem golem, BlockPos targetPos, boolean navigationStarted) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        long distance = scaledGatheringTargetDistance(golem, targetPos);
        long bestDistance = tag.getLongOr(TAG_GATHERING_MOVE_BEST_DISTANCE, Long.MAX_VALUE);
        int stuckTicks = tag.getIntOr(TAG_GATHERING_MOVE_STUCK_TICKS, 0);

        if (bestDistance == Long.MAX_VALUE || distance + GATHERING_MOVE_PROGRESS_THRESHOLD < bestDistance) {
            bestDistance = distance;
            stuckTicks = 0;
        } else if (!navigationStarted || golem.getNavigation().isDone() || golem.getDeltaMovement().lengthSqr() < 1.0E-4D) {
            stuckTicks++;
        } else if (stuckTicks > 0) {
            stuckTicks--;
        }

        tag.putLong(TAG_GATHERING_MOVE_BEST_DISTANCE, bestDistance);
        tag.putInt(TAG_GATHERING_MOVE_STUCK_TICKS, stuckTicks);
        setEntityCustomDataTag(golem, tag);
        return stuckTicks >= GATHERING_UNREACHABLE_SKIP_TICKS;
    }

    private static long scaledGatheringTargetDistance(CopperGolem golem, BlockPos targetPos) {
        double distance = golem.distanceToSqr(Vec3.atCenterOf(targetPos));
        return Math.max(0L, Math.round(distance * GATHERING_MOVE_DISTANCE_SCALE));
    }

    private static void skipUnreachableGatheringTarget(CopperGolem golem, ServerLevel level, BlockPos targetPos) {
        level.destroyBlockProgress(golem.getId(), targetPos, -1);
        CompoundTag tag = getEntityCustomDataTag(golem);
        addSkippedGatheringTarget(tag, targetPos);
        clearGatheringTargetTags(tag);
        tag.putString(TAG_ACTIVITY, CopperGolemActivity.SEARCHING.id());
        setEntityCustomDataTag(golem, tag);
        stopGatheringNavigation(golem);
        clearGatheringDisplayedItem(golem);
    }

    private static boolean isSkippedGatheringTarget(CompoundTag tag, BlockPos pos) {
        return readStringList(tag, TAG_GATHERING_SKIPPED_TARGETS).contains(gatheringTargetKey(pos));
    }

    private static void addSkippedGatheringTarget(CompoundTag tag, BlockPos pos) {
        List<String> skippedTargets = new ArrayList<>(readStringList(tag, TAG_GATHERING_SKIPPED_TARGETS));
        String key = gatheringTargetKey(pos);
        if (!skippedTargets.contains(key)) {
            if (skippedTargets.size() >= GATHERING_SKIPPED_TARGET_LIMIT) {
                skippedTargets.remove(0);
            }
            skippedTargets.add(key);
        }
        writeStringList(tag, TAG_GATHERING_SKIPPED_TARGETS, skippedTargets);
    }

    private static String gatheringTargetKey(BlockPos pos) {
        return Long.toString(pos.asLong());
    }

    private static void tickGatheringDeposit(CopperGolem golem, GatheringHome home, ItemStack storageStack) {
        BlockPos homePos = home.binding().containerPos();
        if (!isGolemCloseToDepositTarget(golem, homePos)) {
            setActivity(golem, CopperGolemActivity.RETURNING_HOME);
            showGatheringStorageInHand(golem, storageStack);
            golem.getNavigation().moveTo(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D, GATHERING_NAVIGATION_SPEED);
            return;
        }

        setActivity(golem, CopperGolemActivity.DEPOSITING);
        showGatheringStorageInHand(golem, storageStack);
        if (!canInsertAll(home.container(), List.of(storageStack))) {
            setActivity(golem, CopperGolemActivity.BLOCKED_HOME_FULL);
            stopGatheringNavigation(golem);
            return;
        }

        if (insertAll(home.container(), List.of(storageStack))) {
            home.container().setChanged();
            setGatheringStorageStack(golem, ItemStack.EMPTY);
            clearGatheringDisplayedItem(golem);
            setActivity(golem, CopperGolemActivity.SEARCHING);
        } else {
            setActivity(golem, CopperGolemActivity.BLOCKED_HOME_FULL);
        }
    }

    private static Optional<GatheringHome> resolveGatheringHome(CopperGolem golem, ServerLevel level) {
        Optional<Binding> source = getSourceContainer(golem);
        if (source.isEmpty() || !source.get().dimension().equals(level.dimension())) {
            return Optional.empty();
        }

        return resolveCopperSourceContainer(level, source.get().containerPos())
                .map(container -> new GatheringHome(source.get(), container));
    }

    private static Optional<GatheringAreaBounds> getGatheringAreaBounds(CopperGolem golem, ServerLevel level) {
        Optional<GatheringArea> area = getGatheringArea(golem)
                .filter(value -> value.dimension().equals(level.dimension()))
                .filter(value -> value.cornerA().isPresent() && value.cornerB().isPresent());
        if (area.isEmpty()) {
            return Optional.empty();
        }

        BlockPos a = area.get().cornerA().get();
        BlockPos b = area.get().cornerB().get();
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());
        long sizeX = (long) maxX - minX + 1L;
        long sizeY = (long) maxY - minY + 1L;
        long sizeZ = (long) maxZ - minZ + 1L;
        long volume = sizeX * sizeY * sizeZ;
        if (!isGatheringAreaWithinLimits(sizeX, sizeY, sizeZ, volume)) {
            return Optional.empty();
        }

        return Optional.of(new GatheringAreaBounds(minX, minY, minZ, maxX, maxY, maxZ, volume));
    }

    private static boolean isGatheringAreaWithinLimits(BlockPos a, BlockPos b) {
        long sizeX = (long) Math.abs(a.getX() - b.getX()) + 1L;
        long sizeY = (long) Math.abs(a.getY() - b.getY()) + 1L;
        long sizeZ = (long) Math.abs(a.getZ() - b.getZ()) + 1L;
        return isGatheringAreaWithinLimits(sizeX, sizeY, sizeZ, sizeX * sizeY * sizeZ);
    }

    private static boolean isGatheringAreaWithinLimits(long sizeX, long sizeY, long sizeZ, long volume) {
        return sizeX <= GATHERING_MAX_AXIS_LENGTH
                && sizeY <= GATHERING_MAX_AXIS_LENGTH
                && sizeZ <= GATHERING_MAX_AXIS_LENGTH
                && volume <= GATHERING_MAX_VOLUME;
    }

    private static Optional<BlockPos> scanNextGatheringTarget(
            CopperGolem golem,
            ServerLevel level,
            GatheringAreaBounds bounds,
            GatheringHome home,
            List<String> manualTargets) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        long gameTime = level.getGameTime();
        CopperGolemActivity storedActivity = CopperGolemActivity.fromId(tag.getStringOr(TAG_ACTIVITY, ""));
        long retryTick = tag.getLongOr(TAG_GATHERING_RETRY_TICK, 0L);
        if (storedActivity == CopperGolemActivity.BLOCKED_NO_VALID_TARGET && gameTime < retryTick) {
            return Optional.empty();
        }

        long cursor = Math.max(0L, tag.getLongOr(TAG_GATHERING_SCAN_INDEX, 0L));
        if (cursor > bounds.volume()) {
            cursor = 0L;
        }
        int remainingBudget = (int) Math.min(GATHERING_SCAN_BUDGET_PER_TICK, bounds.volume());

        while (cursor < bounds.volume() && remainingBudget > 0) {
            BlockPos pos = bounds.topDownPositionAt(cursor);
            cursor++;
            remainingBudget--;

            if (isSkippedGatheringTarget(tag, pos)) {
                continue;
            }

            if (isValidGatheringTarget(golem, level, bounds, home, manualTargets, pos)) {
                CompoundTag updatedTag = getEntityCustomDataTag(golem);
                updatedTag.putLong(TAG_GATHERING_SCAN_INDEX, cursor);
                updatedTag.remove(TAG_GATHERING_NEAREST_SCAN_RADIUS);
                updatedTag.remove(TAG_GATHERING_NEAREST_SCAN_CURSOR);
                writeBlockPos(updatedTag, pos, TAG_GATHERING_TARGET_X, TAG_GATHERING_TARGET_Y, TAG_GATHERING_TARGET_Z);
                updatedTag.remove(TAG_GATHERING_RETRY_TICK);
                updatedTag.putString(TAG_ACTIVITY, CopperGolemActivity.MOVING_TO_TARGET.id());
                setEntityCustomDataTag(golem, updatedTag);
                return Optional.of(pos);
            }
        }

        CompoundTag updatedTag = getEntityCustomDataTag(golem);
        if (cursor >= bounds.volume()) {
            updatedTag.remove(TAG_GATHERING_SCAN_INDEX);
            updatedTag.remove(TAG_GATHERING_NEAREST_SCAN_RADIUS);
            updatedTag.remove(TAG_GATHERING_NEAREST_SCAN_CURSOR);
            updatedTag.remove(TAG_GATHERING_SKIPPED_TARGETS);
            updatedTag.putString(TAG_ACTIVITY, CopperGolemActivity.BLOCKED_NO_VALID_TARGET.id());
            updatedTag.putLong(TAG_GATHERING_RETRY_TICK, gameTime + GATHERING_RETRY_TICKS);
        } else {
            updatedTag.putLong(TAG_GATHERING_SCAN_INDEX, cursor);
            updatedTag.remove(TAG_GATHERING_NEAREST_SCAN_RADIUS);
            updatedTag.remove(TAG_GATHERING_NEAREST_SCAN_CURSOR);
            updatedTag.putString(TAG_ACTIVITY, CopperGolemActivity.SEARCHING.id());
        }
        setEntityCustomDataTag(golem, updatedTag);
        return Optional.empty();
    }

    private static void tickGatheringLlmWarmup(CopperGolem golem, ServerLevel level) {
        GatheringLlmConfig config = getGatheringLlmConfig(golem);
        GolemLlmConfig golemConfig = getGolemLlmConfig(golem);
        if (!config.isUsable(golemConfig)) {
            return;
        }

        Optional<GatheringAreaBounds> bounds = getGatheringAreaBounds(golem, level);
        if (bounds.isEmpty()) {
            return;
        }

        ItemStack toolStack = getGatheringToolStack(golem);
        if (toolStack.isEmpty()) {
            return;
        }

        List<String> manualTargets = getGatheringManualTargets(golem);
        CompoundTag tag = getEntityCustomDataTag(golem);
        long cursor = Math.floorMod(tag.getLongOr(TAG_GATHERING_LLM_WARMUP_INDEX, 0L), bounds.get().volume());
        long checked = 0L;
        int budget = (int) Math.min(GATHERING_LLM_WARMUP_BUDGET_PER_TICK, bounds.get().volume());
        for (int i = 0; i < budget; i++) {
            long index = (cursor + i) % bounds.get().volume();
            requestGatheringLlmWarmupForPos(
                    golem,
                    level,
                    bounds.get().positionAt(index),
                    toolStack,
                    manualTargets,
                    config,
                    golemConfig
            );
            checked++;
        }

        CompoundTag updatedTag = getEntityCustomDataTag(golem);
        updatedTag.putLong(TAG_GATHERING_LLM_WARMUP_INDEX, (cursor + checked) % bounds.get().volume());
        setEntityCustomDataTag(golem, updatedTag);
    }

    private static void requestGatheringLlmWarmupForPos(
            CopperGolem golem,
            ServerLevel level,
            BlockPos pos,
            ItemStack toolStack,
            List<String> manualTargets,
            GatheringLlmConfig config,
            GolemLlmConfig golemConfig) {
        if (!level.isLoaded(pos)
                || isSourceContainer(golem, level, pos)
                || isBoundContainer(golem, level, pos)) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()
                || state.liquid()
                || state.getDestroySpeed(level, pos) < 0.0F
                || isUnsafeGatheringBlock(state)) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof Container) {
            return;
        }

        Optional<List<ItemStack>> drops = getGatheringDrops(golem, level, pos, state, toolStack);
        if (drops.isEmpty()) {
            return;
        }

        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (manualTargets.contains(blockId)) {
            return;
        }

        List<String> blockTags = blockTags(state);
        if (getCachedGatheringLlmDecision(config, blockId, blockTags).isPresent()) {
            return;
        }

        CopperGolemLlmService.requestBlockClassification(
                level.getServer(),
                golem.getUUID(),
                blockId,
                state.getBlock().getName().getString(),
                blockTags,
                gatheringDropSummary(drops.get()),
                gatheringToolSummary(toolStack),
                config.prompt(),
                config.promptRevision(),
                golemConfig.apiUrl(),
                golemConfig.apiKey(),
                golemConfig.model()
        );
    }

    private static boolean tryGatherTarget(
            CopperGolem golem,
            ServerLevel level,
            GatheringAreaBounds bounds,
            GatheringHome home,
            List<String> manualTargets,
            BlockPos targetPos) {
        if (!isValidGatheringTarget(golem, level, bounds, home, manualTargets, targetPos)) {
            level.destroyBlockProgress(golem.getId(), targetPos, -1);
            clearGatheringTarget(golem);
            return false;
        }

        BlockState state = level.getBlockState(targetPos);
        ItemStack toolStack = getGatheringToolStack(golem);
        Optional<List<ItemStack>> drops = getGatheringDrops(golem, level, targetPos, state, toolStack);
        if (drops.isEmpty()) {
            clearGatheringTarget(golem);
            return false;
        }

        ItemStack storageStack = getGatheringStorageStack(golem);
        if (!canGatherDropsIntoStorage(storageStack, drops.get())) {
            clearGatheringTarget(golem);
            return false;
        }

        if (!level.destroyBlock(targetPos, false, golem)) {
            clearGatheringTarget(golem);
            return false;
        }

        if (!consumeFuelForTransport(golem, level)) {
            return false;
        }

        setGatheringStorageStack(golem, addDropsToGatheringStorage(storageStack, drops.get()), false);
        boolean toolBroken = damageGatheringToolAfterBreak(golem, level, toolStack);
        level.levelEvent(2001, targetPos, Block.getId(state));
        level.sendParticles(ParticleTypes.WAX_ON,
                targetPos.getX() + 0.5D,
                targetPos.getY() + 0.5D,
                targetPos.getZ() + 0.5D,
                6,
                0.2D,
                0.2D,
                0.2D,
                0.02D);
        golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        clearGatheringTarget(golem);
        if (toolBroken) {
            clearGatheringDisplayedItem(golem);
            setActivity(golem, CopperGolemActivity.BLOCKED_TOOL_BROKEN);
            return false;
        }
        return true;
    }

    private static boolean tickGatheringBreak(
            CopperGolem golem,
            ServerLevel level,
            GatheringAreaBounds bounds,
            GatheringHome home,
            List<String> manualTargets,
            BlockPos targetPos) {
        if (!isValidGatheringTarget(golem, level, bounds, home, manualTargets, targetPos)) {
            level.destroyBlockProgress(golem.getId(), targetPos, -1);
            clearGatheringTarget(golem);
            return false;
        }

        BlockState state = level.getBlockState(targetPos);
        ItemStack toolStack = getGatheringToolStack(golem);
        int requiredTicks = calculateGatheringBreakTicks(level, targetPos, state, toolStack);
        String stateKey = gatheringBreakStateKey(state);
        CompoundTag tag = getEntityCustomDataTag(golem);
        int progressTicks = tag.getIntOr(TAG_GATHERING_BREAK_TICKS, 0);
        int storedRequiredTicks = tag.getIntOr(TAG_GATHERING_BREAK_REQUIRED_TICKS, requiredTicks);
        String storedStateKey = tag.getStringOr(TAG_GATHERING_BREAK_STATE, "");
        if (storedRequiredTicks != requiredTicks || !stateKey.equals(storedStateKey)) {
            progressTicks = 0;
            storedRequiredTicks = requiredTicks;
        }

        progressTicks++;
        tag.putInt(TAG_GATHERING_BREAK_TICKS, progressTicks);
        tag.putInt(TAG_GATHERING_BREAK_REQUIRED_TICKS, storedRequiredTicks);
        tag.putString(TAG_GATHERING_BREAK_STATE, stateKey);
        setEntityCustomDataTag(golem, tag);

        int breakStage = Math.min(9, (int) ((progressTicks / (double) storedRequiredTicks) * 10.0D));
        level.destroyBlockProgress(golem.getId(), targetPos, breakStage);
        if (progressTicks % 5 == 0) {
            level.levelEvent(2001, targetPos, Block.getId(state));
            golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        }

        if (progressTicks < storedRequiredTicks) {
            return false;
        }

        level.destroyBlockProgress(golem.getId(), targetPos, -1);
        return tryGatherTarget(golem, level, bounds, home, manualTargets, targetPos);
    }

    private static int calculateGatheringBreakTicks(ServerLevel level, BlockPos pos, BlockState state, ItemStack toolStack) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= 0.0F) {
            return 1;
        }

        float toolSpeed = calculateGatheringToolSpeed(state, toolStack);
        boolean correctTool = !state.requiresCorrectToolForDrops() || toolStack.isCorrectToolForDrops(state);
        float divisor = correctTool ? 30.0F : 100.0F;
        int adjustedTicks = Math.max(1, (int) Math.ceil(hardness * divisor / toolSpeed));
        return Math.max(GATHERING_MIN_VISIBLE_BREAK_TICKS, adjustedTicks);
    }

    private static float calculateGatheringToolSpeed(BlockState state, ItemStack toolStack) {
        float toolSpeed = toolStack.isEmpty() ? 1.0F : Math.max(1.0F, toolStack.getDestroySpeed(state));
        if (!toolStack.isEmpty() && toolSpeed > 1.0F) {
            toolSpeed += (float) miningEfficiencyBonus(toolStack);
        }
        return Math.max(0.1F, toolSpeed * GATHERING_TOOL_EFFICIENCY_MULTIPLIER);
    }

    private static double miningEfficiencyBonus(ItemStack toolStack) {
        double[] bonus = {0.0D};
        EnchantmentHelper.forEachModifier(toolStack, EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.MINING_EFFICIENCY)
                    && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                bonus[0] += modifier.amount();
            }
        });
        return bonus[0];
    }

    private static String gatheringBreakStateKey(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static boolean isValidGatheringTarget(
            CopperGolem golem,
            ServerLevel level,
            GatheringAreaBounds bounds,
            GatheringHome home,
            List<String> manualTargets,
            BlockPos pos) {
        if (!bounds.contains(pos)
                || home.binding().containerPos().equals(pos)
                || isBoundContainer(golem, level, pos)
                || !level.isLoaded(pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()
                || state.liquid()
                || state.getDestroySpeed(level, pos) < 0.0F
                || isUnsafeGatheringBlock(state)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container) {
            return false;
        }

        ItemStack toolStack = getGatheringToolStack(golem);
        Optional<List<ItemStack>> drops = getGatheringDrops(golem, level, pos, state, toolStack);
        if (drops.isEmpty() || !canGatherDropsIntoStorage(getGatheringStorageStack(golem), drops.get())) {
            return false;
        }

        return isGatheringBlockAllowed(golem, level, state, toolStack, drops.get(), manualTargets);
    }

    private static boolean hasGatheringTargetRules(CopperGolem golem) {
        return !getGatheringManualTargets(golem).isEmpty()
                || getGatheringLlmConfig(golem).isUsable(getGolemLlmConfig(golem));
    }

    private static boolean isGatheringBlockAllowed(
            CopperGolem golem,
            ServerLevel level,
            BlockState state,
            ItemStack toolStack,
            List<ItemStack> drops,
            List<String> manualTargets) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (manualTargets.contains(blockId)) {
            return true;
        }

        GatheringLlmConfig config = getGatheringLlmConfig(golem);
        GolemLlmConfig golemConfig = getGolemLlmConfig(golem);
        if (!config.isUsable(golemConfig)) {
            return false;
        }

        List<String> blockTags = blockTags(state);
        Optional<Boolean> cachedDecision = getCachedGatheringLlmDecision(config, blockId, blockTags);
        if (cachedDecision.isPresent()) {
            return cachedDecision.get();
        }

        CopperGolemLlmService.requestBlockClassification(
                level.getServer(),
                golem.getUUID(),
                blockId,
                state.getBlock().getName().getString(),
                blockTags,
                gatheringDropSummary(drops),
                gatheringToolSummary(toolStack),
                config.prompt(),
                config.promptRevision(),
                golemConfig.apiUrl(),
                golemConfig.apiKey(),
                golemConfig.model()
        );
        return false;
    }

    private static Optional<Boolean> getCachedGatheringLlmDecision(GatheringLlmConfig config, String blockId, List<String> blockTags) {
        if (config.allowedBlockIds().contains(blockId)) {
            return Optional.of(true);
        }
        if (config.deniedBlockIds().contains(blockId)) {
            return Optional.of(false);
        }

        for (String tag : blockTags) {
            if (config.allowedTags().contains(tag)) {
                return Optional.of(true);
            }
            if (config.deniedTags().contains(tag)) {
                return Optional.of(false);
            }
        }
        return Optional.empty();
    }

    private static List<String> blockTags(BlockState state) {
        return state.getBlock().builtInRegistryHolder().tags()
                .map(tag -> tag.location().toString())
                .sorted()
                .toList();
    }

    private static List<String> gatheringDropSummary(List<ItemStack> drops) {
        List<String> summary = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                summary.add(CopperGolemLlmService.itemId(drop) + " x" + drop.getCount());
            }
        }
        return List.copyOf(summary);
    }

    private static String gatheringToolSummary(ItemStack toolStack) {
        if (toolStack.isEmpty()) {
            return "none";
        }
        String itemId = CopperGolemLlmService.itemId(toolStack);
        if (!toolStack.isDamageableItem()) {
            return itemId;
        }
        return itemId + " durability " + Math.max(0, toolStack.getMaxDamage() - toolStack.getDamageValue()) + "/" + toolStack.getMaxDamage();
    }

    private static boolean isUnsafeGatheringBlock(BlockState state) {
        return state.is(Blocks.TNT)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.RESPAWN_ANCHOR);
    }

    private static Optional<List<ItemStack>> getGatheringDrops(CopperGolem golem, ServerLevel level, BlockPos pos, BlockState state, ItemStack toolStack) {
        if (toolStack.isEmpty() || (state.requiresCorrectToolForDrops() && !toolStack.isCorrectToolForDrops(state))) {
            return Optional.empty();
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> rawDrops = Block.getDrops(state, level, pos, blockEntity, golem, toolStack);
        List<ItemStack> normalized = new ArrayList<>();
        int totalCount = 0;
        for (ItemStack rawDrop : rawDrops) {
            if (rawDrop.isEmpty()) {
                continue;
            }

            if (!normalized.isEmpty() && !ItemStack.isSameItemSameComponents(normalized.getFirst(), rawDrop)) {
                return Optional.empty();
            }

            if (normalized.isEmpty()) {
                normalized.add(rawDrop.copy());
            } else {
                normalized.getFirst().grow(rawDrop.getCount());
            }
            totalCount += rawDrop.getCount();
            if (totalCount > TRANSPORTED_ITEM_MAX_STACK_SIZE) {
                return Optional.empty();
            }
        }

        return normalized.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(normalized));
    }

    private static boolean canGatherDropsIntoStorage(ItemStack storageStack, List<ItemStack> drops) {
        ItemStack simulated = storageStack.copy();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }

            if (simulated.isEmpty()) {
                if (drop.getCount() > TRANSPORTED_ITEM_MAX_STACK_SIZE) {
                    return false;
                }
                simulated = drop.copy();
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(simulated, drop)) {
                return false;
            }

            if (simulated.getCount() + drop.getCount() > TRANSPORTED_ITEM_MAX_STACK_SIZE) {
                return false;
            }
            simulated.grow(drop.getCount());
        }

        return !simulated.isEmpty();
    }

    private static ItemStack addDropsToGatheringStorage(ItemStack storageStack, List<ItemStack> drops) {
        ItemStack result = storageStack.copy();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }

            if (result.isEmpty()) {
                result = drop.copy();
            } else if (ItemStack.isSameItemSameComponents(result, drop)) {
                result.grow(drop.getCount());
            }
        }

        if (!result.isEmpty() && result.getCount() > TRANSPORTED_ITEM_MAX_STACK_SIZE) {
            result.setCount(TRANSPORTED_ITEM_MAX_STACK_SIZE);
        }
        return result;
    }

    private static boolean damageGatheringToolAfterBreak(CopperGolem golem, ServerLevel level, ItemStack toolStack) {
        if (toolStack.isEmpty() || !toolStack.isDamageableItem()) {
            return false;
        }

        ItemStack damaged = toolStack.copy();
        int damageAmount = EnchantmentHelper.processDurabilityChange(level, damaged, 1);
        damaged.setDamageValue(damaged.getDamageValue() + Math.max(0, damageAmount));
        if (damaged.getDamageValue() >= damaged.getMaxDamage()) {
            setGatheringToolStack(golem, ItemStack.EMPTY);
            setActivity(golem, CopperGolemActivity.BLOCKED_TOOL_BROKEN);
            return true;
        } else {
            setGatheringToolStack(golem, damaged, false);
            return false;
        }
    }

    private static boolean canInsertAll(Container container, List<ItemStack> stacks) {
        List<ItemStack> simulated = new ArrayList<>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            simulated.add(container.getItem(slot).copy());
        }

        for (ItemStack stack : stacks) {
            ItemStack remaining = stack.copy();
            simulateInsert(container, simulated, remaining);
            if (!remaining.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static void simulateInsert(Container container, List<ItemStack> slots, ItemStack remaining) {
        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            ItemStack existing = slots.get(slot);
            if (existing.isEmpty()
                    || !ItemStack.isSameItemSameComponents(existing, remaining)
                    || !container.canPlaceItem(slot, remaining)) {
                continue;
            }

            int maxStackSize = Math.min(existing.getMaxStackSize(), container.getMaxStackSize(remaining));
            int moveCount = Math.min(remaining.getCount(), maxStackSize - existing.getCount());
            if (moveCount <= 0) {
                continue;
            }

            existing.grow(moveCount);
            remaining.shrink(moveCount);
            slots.set(slot, existing);
        }

        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            if (!slots.get(slot).isEmpty() || !container.canPlaceItem(slot, remaining)) {
                continue;
            }

            int moveCount = Math.min(remaining.getCount(), container.getMaxStackSize(remaining));
            ItemStack moved = remaining.copyWithCount(moveCount);
            remaining.shrink(moveCount);
            slots.set(slot, moved);
        }
    }

    private static boolean insertAll(Container container, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack remaining = stack.copy();
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                mergeIntoSlot(container, slot, remaining);
            }
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                placeIntoEmptySlot(container, slot, remaining);
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Optional<BlockPos> getGatheringTarget(CopperGolem golem) {
        return readBlockPos(getEntityCustomDataTag(golem), TAG_GATHERING_TARGET_X, TAG_GATHERING_TARGET_Y, TAG_GATHERING_TARGET_Z);
    }

    private static void clearGatheringTarget(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        clearGatheringTargetTags(tag);
        tag.remove(TAG_ACTIVITY);
        setEntityCustomDataTag(golem, tag);
    }

    private static void clearGatheringTargetTags(CompoundTag tag) {
        tag.remove(TAG_GATHERING_TARGET_X);
        tag.remove(TAG_GATHERING_TARGET_Y);
        tag.remove(TAG_GATHERING_TARGET_Z);
        tag.remove(TAG_GATHERING_BREAK_TICKS);
        tag.remove(TAG_GATHERING_BREAK_REQUIRED_TICKS);
        tag.remove(TAG_GATHERING_BREAK_STATE);
        tag.remove(TAG_GATHERING_MOVE_BEST_DISTANCE);
        tag.remove(TAG_GATHERING_MOVE_STUCK_TICKS);
    }

    private static void resetGatheringSearch(CompoundTag tag) {
        resetGatheringSearch(tag, true);
    }

    private static void resetGatheringSearch(CompoundTag tag, boolean clearSkippedTargets) {
        clearGatheringTargetTags(tag);
        tag.remove(TAG_GATHERING_SCAN_INDEX);
        tag.remove(TAG_GATHERING_NEAREST_SCAN_RADIUS);
        tag.remove(TAG_GATHERING_NEAREST_SCAN_CURSOR);
        if (clearSkippedTargets) {
            tag.remove(TAG_GATHERING_SKIPPED_TARGETS);
        }
        tag.remove(TAG_GATHERING_LLM_WARMUP_INDEX);
        tag.remove(TAG_GATHERING_RETRY_TICK);
        tag.remove(TAG_ACTIVITY);
    }

    private static void setActivity(CopperGolem golem, CopperGolemActivity activity) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        tag.putString(TAG_ACTIVITY, activity.id());
        setEntityCustomDataTag(golem, tag);
    }

    private static void stopGatheringNavigation(CopperGolem golem) {
        golem.getNavigation().stop();
        golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private static boolean isGatheringStorageFull(ItemStack storageStack) {
        return !storageStack.isEmpty() && storageStack.getCount() >= TRANSPORTED_ITEM_MAX_STACK_SIZE;
    }

    private static boolean shouldContinueGatheringDeposit(CopperGolem golem, ItemStack storageStack) {
        if (isGatheringStorageFull(storageStack)) {
            return true;
        }

        CopperGolemActivity activity = CopperGolemActivity.fromId(getEntityCustomDataTag(golem).getStringOr(TAG_ACTIVITY, ""));
        return activity == CopperGolemActivity.RETURNING_HOME
                || activity == CopperGolemActivity.DEPOSITING
                || activity == CopperGolemActivity.BLOCKED_NO_VALID_TARGET;
    }

    private static boolean isGatheringBlockedNoValidTarget(CopperGolem golem) {
        return CopperGolemActivity.fromId(getEntityCustomDataTag(golem).getStringOr(TAG_ACTIVITY, ""))
                == CopperGolemActivity.BLOCKED_NO_VALID_TARGET;
    }

    private static void showGatheringToolInHand(CopperGolem golem) {
        ItemStack toolStack = getGatheringToolStack(golem);
        setGatheringDisplayedItem(golem, toolStack);
    }

    private static void showGatheringStorageInHand(CopperGolem golem, ItemStack storageStack) {
        setGatheringDisplayedItem(golem, storageStack);
    }

    private static void setGatheringDisplayedItem(CopperGolem golem, ItemStack stack) {
        ItemStack displayStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!displayStack.isEmpty()) {
            displayStack.setCount(Math.min(displayStack.getCount(), TRANSPORTED_ITEM_MAX_STACK_SIZE));
        }
        if (!ItemStack.isSameItemSameComponents(golem.getMainHandItem(), displayStack)
                || golem.getMainHandItem().getCount() != displayStack.getCount()) {
            golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, displayStack);
        }
    }

    public static void clearGatheringDisplayedItem(CopperGolem golem) {
        if (getMode(golem) == CopperGolemMode.GATHERING && !golem.getMainHandItem().isEmpty()) {
            golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    private static boolean isVirtualGatheringDisplayedItem(CopperGolem golem) {
        if (getMode(golem) != CopperGolemMode.GATHERING || golem.getMainHandItem().isEmpty()) {
            return false;
        }

        return isSameDisplayedStack(golem.getMainHandItem(), getGatheringToolStack(golem))
                || isSameDisplayedStack(golem.getMainHandItem(), getGatheringStorageStack(golem));
    }

    private static boolean isSameDisplayedStack(ItemStack displayed, ItemStack stored) {
        if (displayed.isEmpty() || stored.isEmpty()) {
            return false;
        }

        int displayCount = Math.min(stored.getCount(), TRANSPORTED_ITEM_MAX_STACK_SIZE);
        return displayed.getCount() == displayCount && ItemStack.isSameItemSameComponents(displayed, stored);
    }

    private static boolean isGolemCloseToGatheringTarget(CopperGolem golem, BlockPos pos) {
        if (isWithinUpwardGatheringRange(golem, pos)) {
            return true;
        }

        if (isWithinDownwardGatheringRange(golem, pos)) {
            return true;
        }

        return golem.distanceToSqr(Vec3.atCenterOf(pos)) <= GATHERING_BREAK_REACH_DISTANCE_SQR;
    }

    private static boolean isWithinUpwardGatheringRange(CopperGolem golem, BlockPos pos) {
        BlockPos headPos = golem.blockPosition().above();
        int dx = Math.abs(headPos.getX() - pos.getX());
        int dy = pos.getY() - headPos.getY();
        int dz = Math.abs(headPos.getZ() - pos.getZ());
        return dy >= 1
                && dy <= GATHERING_UPWARD_REACH_HEIGHT
                && dx <= GATHERING_UPWARD_REACH_HORIZONTAL
                && dz <= GATHERING_UPWARD_REACH_HORIZONTAL;
    }

    private static boolean isWithinDownwardGatheringRange(CopperGolem golem, BlockPos pos) {
        BlockPos feetPos = golem.blockPosition();
        int dx = Math.abs(feetPos.getX() - pos.getX());
        int dy = feetPos.getY() - pos.getY();
        int dz = Math.abs(feetPos.getZ() - pos.getZ());
        return dy >= 1
                && dy <= GATHERING_DOWNWARD_REACH_DEPTH
                && dx <= GATHERING_DOWNWARD_REACH_HORIZONTAL
                && dz <= GATHERING_DOWNWARD_REACH_HORIZONTAL;
    }

    private static boolean isGolemCloseToDepositTarget(CopperGolem golem, BlockPos pos) {
        return golem.distanceToSqr(Vec3.atCenterOf(pos)) <= GATHERING_DEPOSIT_REACH_DISTANCE_SQR;
    }

    private static String modeTranslationKey(CopperGolemMode mode) {
        return "message.deadrecall.copper_wrench.mode_" + mode.id();
    }

    private static void insertFuelFromAvailableInventory(ServerPlayer player, CopperGolem golem) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack fuelStack = getFuelStack(golem);
        if (!fuelStack.isEmpty() && fuelStack.getCount() >= fuelStack.getMaxStackSize()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.fuel_full"));
            return;
        }

        ItemStack source = findFuelSourceStack(player, level, fuelStack);
        if (source.isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.fuel_need_item"));
            return;
        }

        if (!fuelStack.isEmpty() && !ItemStack.isSameItemSameComponents(fuelStack, source)) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.fuel_mismatch"));
            return;
        }

        int maxCount = fuelStack.isEmpty() ? source.getMaxStackSize() : fuelStack.getMaxStackSize();
        int moveCount = Math.min(source.getCount(), maxCount - fuelStack.getCount());
        if (moveCount <= 0) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.fuel_full"));
            return;
        }

        ItemStack newFuelStack;
        if (fuelStack.isEmpty()) {
            newFuelStack = source.copyWithCount(moveCount);
        } else {
            newFuelStack = fuelStack.copy();
            newFuelStack.grow(moveCount);
        }

        source.shrink(moveCount);
        player.getInventory().setChanged();
        setFuelStack(golem, newFuelStack);
        resetTransportMemories(golem);
        notify(player, Component.translatable("message.deadrecall.copper_wrench.fuel_inserted", moveCount));
    }

    private static ItemStack findFuelSourceStack(ServerPlayer player, ServerLevel level, ItemStack existingFuel) {
        ItemStack held = player.getMainHandItem();
        if (canMoveFuelFrom(level, existingFuel, held)) {
            return held;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (canMoveFuelFrom(level, existingFuel, stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean canMoveFuelFrom(ServerLevel level, ItemStack existingFuel, ItemStack source) {
        if (!isFuel(level, source)) {
            return false;
        }
        return existingFuel.isEmpty() || ItemStack.isSameItemSameComponents(existingFuel, source);
    }

    private static void takeFuel(ServerPlayer player, CopperGolem golem) {
        ItemStack fuelStack = getFuelStack(golem);
        if (fuelStack.isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.fuel_empty"));
            return;
        }

        setFuelStack(golem, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(fuelStack.copy());
        player.getInventory().setChanged();
        resetTransportMemories(golem);
        notify(player, Component.translatable("message.deadrecall.copper_wrench.fuel_taken"));
    }

    private static void insertGatheringToolFromAvailableInventory(ServerPlayer player, CopperGolem golem) {
        if (!getGatheringToolStack(golem).isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_tool_full"));
            return;
        }

        ItemStack source = findGatheringToolSourceStack(player);
        if (source.isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_tool_need_item"));
            return;
        }

        ItemStack moved = source.copyWithCount(1);
        source.shrink(1);
        player.getInventory().setChanged();
        setGatheringToolStack(golem, moved);
        bumpGolemRevision(golem);
        resetTransportMemories(golem);
        notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_tool_inserted"));
    }

    private static ItemStack findGatheringToolSourceStack(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (isGatheringTool(held)) {
            return held;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isGatheringTool(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void takeGatheringTool(ServerPlayer player, CopperGolem golem) {
        ItemStack toolStack = getGatheringToolStack(golem);
        if (toolStack.isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_tool_empty"));
            return;
        }

        setGatheringToolStack(golem, ItemStack.EMPTY);
        bumpGolemRevision(golem);
        player.getInventory().placeItemBackInInventory(toolStack);
        player.getInventory().setChanged();
        resetTransportMemories(golem);
        notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_tool_taken"));
    }

    private static void takeGatheringStorage(ServerPlayer player, CopperGolem golem) {
        ItemStack storageStack = getGatheringStorageStack(golem);
        if (storageStack.isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_storage_empty"));
            return;
        }

        setGatheringStorageStack(golem, ItemStack.EMPTY);
        bumpGolemRevision(golem);
        player.getInventory().placeItemBackInInventory(storageStack);
        player.getInventory().setChanged();
        resetTransportMemories(golem);
        notify(player, Component.translatable("message.deadrecall.copper_wrench.gathering_storage_taken"));
    }

    private static boolean isGatheringTool(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModItems.COPPER_WRENCH)) {
            return false;
        }

        if (stack.isDamageableItem()) {
            return true;
        }

        BlockState[] sampleStates = {
                Blocks.STONE.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(),
                Blocks.OAK_LOG.defaultBlockState(),
                Blocks.SAND.defaultBlockState(),
                Blocks.GRAVEL.defaultBlockState(),
                Blocks.COBWEB.defaultBlockState()
        };
        for (BlockState sampleState : sampleStates) {
            if (stack.getDestroySpeed(sampleState) > 1.0F || stack.isCorrectToolForDrops(sampleState)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeFuelForTransport(CopperGolem golem, ServerLevel level) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        int fuelTicks = tag.getIntOr(TAG_FUEL_TICKS, 0);
        if (fuelTicks <= 0) {
            ItemStack fuelStack = readFuelStack(tag);
            if (!isFuel(level, fuelStack)) {
                return false;
            }

            fuelTicks = Math.max(1, level.fuelValues().burnDuration(fuelStack));
            writeFuelStack(tag, consumeOneFuelItem(fuelStack));
        }

        fuelTicks = Math.max(0, fuelTicks - FUEL_TICKS_PER_TRANSPORT);
        if (fuelTicks > 0) {
            tag.putInt(TAG_FUEL_TICKS, fuelTicks);
        } else {
            tag.remove(TAG_FUEL_TICKS);
        }
        removeSortingBlockedTags(tag);
        setEntityCustomDataTag(golem, tag);
        return true;
    }

    private static ItemStack consumeOneFuelItem(ItemStack fuelStack) {
        Item item = fuelStack.getItem();
        fuelStack.shrink(1);
        if (!fuelStack.isEmpty()) {
            return fuelStack;
        }

        var craftingRemainder = item.getCraftingRemainder();
        return craftingRemainder == null ? ItemStack.EMPTY : craftingRemainder.create();
    }

    private static boolean isFuel(ServerLevel level, ItemStack stack) {
        return !stack.isEmpty() && level.fuelValues().isFuel(stack);
    }

    private static ItemStack getFuelStack(CopperGolem golem) {
        return readFuelStack(getEntityCustomDataTag(golem));
    }

    private static int getFuelTicks(CopperGolem golem) {
        return getEntityCustomDataTag(golem).getIntOr(TAG_FUEL_TICKS, 0);
    }

    private static void setFuelStack(CopperGolem golem, ItemStack fuelStack) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        writeFuelStack(tag, fuelStack);
        removeSortingBlockedTags(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
    }

    private static ItemStack getGatheringToolStack(CopperGolem golem) {
        return readItemStack(getEntityCustomDataTag(golem), TAG_GATHERING_TOOL_STACK);
    }

    private static void setGatheringToolStack(CopperGolem golem, ItemStack toolStack) {
        setGatheringToolStack(golem, toolStack, true);
    }

    private static void setGatheringToolStack(CopperGolem golem, ItemStack toolStack, boolean clearSkippedTargets) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        writeItemStack(tag, TAG_GATHERING_TOOL_STACK, toolStack.isEmpty() ? ItemStack.EMPTY : toolStack.copyWithCount(1));
        resetGatheringSearch(tag, clearSkippedTargets);
        setEntityCustomDataTag(golem, tag);
    }

    private static ItemStack getGatheringStorageStack(CopperGolem golem) {
        ItemStack stack = readItemStack(getEntityCustomDataTag(golem), TAG_GATHERING_STORAGE_STACK);
        if (stack.getCount() > TRANSPORTED_ITEM_MAX_STACK_SIZE) {
            stack.setCount(TRANSPORTED_ITEM_MAX_STACK_SIZE);
        }
        return stack;
    }

    private static void setGatheringStorageStack(CopperGolem golem, ItemStack storageStack) {
        setGatheringStorageStack(golem, storageStack, true);
    }

    private static void setGatheringStorageStack(CopperGolem golem, ItemStack storageStack, boolean resetSearch) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        if (!storageStack.isEmpty() && storageStack.getCount() > TRANSPORTED_ITEM_MAX_STACK_SIZE) {
            storageStack = storageStack.copyWithCount(TRANSPORTED_ITEM_MAX_STACK_SIZE);
        }
        writeItemStack(tag, TAG_GATHERING_STORAGE_STACK, storageStack);
        if (resetSearch) {
            resetGatheringSearch(tag, false);
        }
        setEntityCustomDataTag(golem, tag);
    }

    private static ItemStack readFuelStack(CompoundTag tag) {
        return readItemStack(tag, TAG_FUEL_STACK);
    }

    private static void writeFuelStack(CompoundTag tag, ItemStack fuelStack) {
        writeItemStack(tag, TAG_FUEL_STACK, fuelStack);
    }

    private static ItemStack readItemStack(CompoundTag tag, String key) {
        return tag.read(key, ItemStack.OPTIONAL_CODEC)
                .orElse(ItemStack.EMPTY)
                .copy();
    }

    private static void writeItemStack(CompoundTag tag, String key, ItemStack stack) {
        if (stack.isEmpty()) {
            tag.remove(key);
        } else {
            tag.store(key, ItemStack.OPTIONAL_CODEC, stack.copy());
        }
    }

    private static void resetTransportMemories(CopperGolem golem) {
        golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        golem.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
        golem.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
        golem.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
    }

    private static Optional<Container> resolveContainer(ServerLevel level, BlockPos targetPos) {
        TransportItemTarget target = TransportItemTarget.tryCreatePossibleTarget(targetPos, level);
        return target == null ? Optional.empty() : Optional.of(target.container());
    }

    private static Optional<Container> resolveCopperSourceContainer(ServerLevel level, BlockPos targetPos) {
        if (!isCopperSourceContainer(level, targetPos)) {
            return Optional.empty();
        }

        BlockEntity blockEntity = level.getBlockEntity(targetPos);
        return blockEntity instanceof Container container ? Optional.of(container) : Optional.empty();
    }

    private static TransportItemTarget tryCreateBoundTarget(ServerLevel level, BlockPos targetPos) {
        if (isCopperSourceContainer(level, targetPos)) {
            return null;
        }

        return TransportItemTarget.tryCreatePossibleTarget(targetPos, level);
    }

    private static Optional<Container> resolveBoundContainer(ServerLevel level, BlockPos targetPos) {
        TransportItemTarget target = tryCreateBoundTarget(level, targetPos);
        return target == null ? Optional.empty() : Optional.of(target.container());
    }

    private static boolean isCopperSourceContainer(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.COPPER_CHESTS);
    }

    private static boolean pruneUnavailableSourceContainer(CopperGolem golem, MinecraftServer server) {
        Optional<Binding> source = getSourceContainer(golem);
        if (source.isEmpty()) {
            return false;
        }

        ServerLevel sourceLevel = server.getLevel(source.get().dimension());
        if (sourceLevel == null || !sourceLevel.isLoaded(source.get().containerPos())) {
            return false;
        }

        if (resolveCopperSourceContainer(sourceLevel, source.get().containerPos()).isPresent()) {
            return false;
        }

        clearSourceContainer(golem);
        return true;
    }

    private static boolean pruneUnavailableBindings(CopperGolem golem, MinecraftServer server) {
        List<Binding> bindings = getBindings(golem);
        if (bindings.isEmpty()) {
            return false;
        }

        List<Binding> keptBindings = new ArrayList<>(bindings.size());
        for (Binding binding : bindings) {
            ServerLevel bindingLevel = server.getLevel(binding.dimension());
            if (bindingLevel == null || !bindingLevel.isLoaded(binding.containerPos())) {
                keptBindings.add(binding);
                continue;
            }

            if (resolveBoundContainer(bindingLevel, binding.containerPos()).isPresent()) {
                keptBindings.add(binding);
            }
        }

        if (keptBindings.size() == bindings.size()) {
            return false;
        }

        setBindings(golem, keptBindings);
        return true;
    }

    private static InteractionResult showBoundContainerPaths(Player player, ServerLevel level, CopperGolem golem) {
        pruneUnavailableBindings(golem, level.getServer());
        List<Binding> bindings = getBindings(golem);
        if (bindings.isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.binding_list_empty"));
            return InteractionResult.SUCCESS;
        }

        List<Binding> sameDimensionBindings = bindings.stream()
                .filter(binding -> binding.dimension().equals(level.dimension()))
                .toList();
        if (sameDimensionBindings.isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.target_other_dimension"));
            return InteractionResult.SUCCESS;
        }

        List<Binding> availableBindings = sameDimensionBindings.stream()
                .filter(binding -> resolveBoundContainer(level, binding.containerPos()).isPresent())
                .toList();
        if (availableBindings.isEmpty()) {
            notify(player, Component.translatable("message.deadrecall.copper_wrench.target_unavailable"));
            return InteractionResult.SUCCESS;
        }

        for (Binding binding : availableBindings) {
            showParticlePath(level, golem, binding.containerPos());
        }
        notify(player, Component.translatable("message.deadrecall.copper_wrench.path_shown"));
        return InteractionResult.SUCCESS;
    }

    private static void sendBindingListUi(ServerPlayer player, CopperGolem golem) {
        pruneUnavailableSourceContainer(golem, player.level().getServer());
        pruneUnavailableBindings(golem, player.level().getServer());
        List<Binding> bindings = getBindings(golem);
        List<CopperWrenchBindingsPayload.BindingEntry> entries = new ArrayList<>(bindings.size());
        MinecraftServer server = player.level().getServer();
        for (Binding binding : bindings) {
            entries.add(createBindingEntry(server, golem, binding));
        }

        boolean canManageLlmConfig = canManageLlmConfig(player);
        GolemLlmConfig llmConfig = getGolemLlmConfig(golem);
        GatheringLlmConfig gatheringLlmConfig = getGatheringLlmConfig(golem);
        ItemStack fuelStack = getFuelStack(golem);
        ItemStack gatheringToolStack = getGatheringToolStack(golem);
        ItemStack gatheringStorageStack = getGatheringStorageStack(golem);
        ServerPlayNetworking.send(player, new CopperWrenchBindingsPayload(
                golem.getUUID(),
                getRevision(golem),
                isTransportEnabled(golem),
                getMode(golem).id(),
                getActivity(golem, (ServerLevel) player.level()).id(),
                fuelStack.isEmpty() ? BuiltInRegistries.ITEM.getKey(Items.AIR).toString() : BuiltInRegistries.ITEM.getKey(fuelStack.getItem()).toString(),
                fuelStack.getCount(),
                getFuelTicks(golem),
                gatheringToolStack.isEmpty() ? BuiltInRegistries.ITEM.getKey(Items.AIR).toString() : BuiltInRegistries.ITEM.getKey(gatheringToolStack.getItem()).toString(),
                gatheringToolStack.getCount(),
                gatheringToolStack.isDamageableItem() ? gatheringToolStack.getDamageValue() : 0,
                gatheringToolStack.isDamageableItem() ? gatheringToolStack.getMaxDamage() : 0,
                gatheringStorageStack.isEmpty() ? BuiltInRegistries.ITEM.getKey(Items.AIR).toString() : BuiltInRegistries.ITEM.getKey(gatheringStorageStack.getItem()).toString(),
                gatheringStorageStack.getCount(),
                llmConfig.apiUrl(),
                canManageLlmConfig ? llmConfig.apiKey() : "",
                llmConfig.model(),
                countActiveLlmBindings(golem),
                getSourceContainer(golem)
                        .map(binding -> createSourceEntry(server, binding))
                        .orElse(null),
                createGatheringAreaEntry(golem),
                getGatheringManualTargets(golem),
                gatheringLlmConfig.enabled(),
                gatheringLlmConfig.prompt(),
                gatheringLlmConfig.allowedBlockIds().size() + gatheringLlmConfig.deniedBlockIds().size(),
                gatheringLlmConfig.allowedTags().size() + gatheringLlmConfig.deniedTags().size(),
                gatheringLlmConfig.allowedBlockIds(),
                gatheringLlmConfig.deniedBlockIds(),
                gatheringLlmConfig.allowedTags(),
                gatheringLlmConfig.deniedTags(),
                entries
        ));
    }

    private static CopperWrenchBindingsPayload.GatheringAreaEntry createGatheringAreaEntry(CopperGolem golem) {
        return getGatheringArea(golem)
                .map(area -> {
                    BlockPos cornerA = area.cornerA().orElse(BlockPos.ZERO);
                    BlockPos cornerB = area.cornerB().orElse(BlockPos.ZERO);
                    return new CopperWrenchBindingsPayload.GatheringAreaEntry(
                            area.dimension().identifier().toString(),
                            area.cornerA().isPresent(),
                            cornerA.getX(),
                            cornerA.getY(),
                            cornerA.getZ(),
                            area.cornerB().isPresent(),
                            cornerB.getX(),
                            cornerB.getY(),
                            cornerB.getZ()
                    );
                })
                .orElse(null);
    }

    private static CopperGolemVisualizationPayload.AreaEntry createVisualizationArea(CopperGolem golem) {
        return getGatheringArea(golem)
                .map(area -> {
                    BlockPos cornerA = area.cornerA().orElse(BlockPos.ZERO);
                    BlockPos cornerB = area.cornerB().orElse(BlockPos.ZERO);
                    return new CopperGolemVisualizationPayload.AreaEntry(
                            area.dimension().identifier().toString(),
                            area.cornerA().isPresent(),
                            cornerA.getX(),
                            cornerA.getY(),
                            cornerA.getZ(),
                            area.cornerB().isPresent(),
                            cornerB.getX(),
                            cornerB.getY(),
                            cornerB.getZ()
                    );
                })
                .orElse(null);
    }

    private static CopperGolemVisualizationPayload.PosEntry createVisualizationPos(MinecraftServer server, Binding binding) {
        ServerLevel bindingLevel = server.getLevel(binding.dimension());
        BlockPos pos = binding.containerPos();
        boolean available = bindingLevel != null
                && bindingLevel.isLoaded(pos)
                && bindingLevel.getBlockEntity(pos) instanceof Container;
        return new CopperGolemVisualizationPayload.PosEntry(
                binding.dimension().identifier().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                available
        );
    }

    private static CopperWrenchBindingsPayload.BindingEntry createSourceEntry(MinecraftServer server, Binding binding) {
        ServerLevel bindingLevel = server.getLevel(binding.dimension());
        BlockPos pos = binding.containerPos();
        if (bindingLevel == null || !bindingLevel.isLoaded(pos)) {
            return new CopperWrenchBindingsPayload.BindingEntry(
                    binding.dimension().identifier().toString(),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    "unloaded",
                    BuiltInRegistries.ITEM.getKey(Items.CHEST).toString(),
                    false,
                    false,
                    false,
                    "",
                    0,
                    0,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        BlockState state = bindingLevel.getBlockState(pos);
        boolean available = resolveCopperSourceContainer(bindingLevel, pos).isPresent();
        Item displayItem = state.getBlock().asItem();
        if (displayItem == Items.AIR) {
            displayItem = available ? Items.CHEST : Items.BARRIER;
        }
        return new CopperWrenchBindingsPayload.BindingEntry(
                binding.dimension().identifier().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                BuiltInRegistries.ITEM.getKey(displayItem).toString(),
                true,
                available,
                false,
                "",
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static CopperWrenchBindingsPayload.BindingEntry createBindingEntry(MinecraftServer server, CopperGolem golem, Binding binding) {
        BindingLlmConfig llmConfig = getBindingLlmConfig(golem, binding);
        ServerLevel bindingLevel = server.getLevel(binding.dimension());
        BlockPos pos = binding.containerPos();
        if (bindingLevel == null || !bindingLevel.isLoaded(pos)) {
            return new CopperWrenchBindingsPayload.BindingEntry(
                    binding.dimension().identifier().toString(),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    "unloaded",
                    BuiltInRegistries.ITEM.getKey(Items.CHEST).toString(),
                    false,
                    false,
                    llmConfig.enabled(),
                    llmConfig.prompt(),
                    llmConfig.allowedItemIds().size() + llmConfig.deniedItemIds().size(),
                    llmConfig.allowedTags().size() + llmConfig.deniedTags().size(),
                    llmConfig.allowedItemIds(),
                    llmConfig.deniedItemIds(),
                    llmConfig.allowedTags(),
                    llmConfig.deniedTags()
            );
        }

        BlockState state = bindingLevel.getBlockState(pos);
        boolean available = resolveBoundContainer(bindingLevel, pos).isPresent();
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        Item displayItem = state.getBlock().asItem();
        if (displayItem == Items.AIR) {
            displayItem = available ? Items.CHEST : Items.BARRIER;
        }
        List<String> acceptedPreviewItemIds = available
                ? mergePreviewItemIds(collectManualAcceptedItemIds(resolveBoundContainer(bindingLevel, pos).orElse(null), llmConfig), llmConfig.allowedItemIds(), llmConfig)
                : llmConfig.allowedItemIds();

        return new CopperWrenchBindingsPayload.BindingEntry(
                binding.dimension().identifier().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                blockId,
                BuiltInRegistries.ITEM.getKey(displayItem).toString(),
                true,
                available,
                llmConfig.enabled(),
                llmConfig.prompt(),
                llmConfig.allowedItemIds().size() + llmConfig.deniedItemIds().size(),
                llmConfig.allowedTags().size() + llmConfig.deniedTags().size(),
                acceptedPreviewItemIds,
                llmConfig.deniedItemIds(),
                llmConfig.allowedTags(),
                llmConfig.deniedTags()
        );
    }

    private static List<String> collectManualAcceptedItemIds(Container container, BindingLlmConfig llmConfig) {
        if (container == null) {
            return List.of();
        }

        Set<String> itemIds = new LinkedHashSet<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            addManualAcceptedPreviewItem(container, itemIds, stack, llmConfig);
            if (isSortableBackpack(stack)) {
                for (ItemStack nestedStack : loadBackpackItems(stack)) {
                    addManualAcceptedPreviewItem(container, itemIds, nestedStack, llmConfig);
                }
            }
        }
        return List.copyOf(itemIds);
    }

    private static void addManualAcceptedPreviewItem(Container container, Set<String> itemIds, ItemStack stack, BindingLlmConfig llmConfig) {
        if (stack.isEmpty() || BackpackItemHelper.isBackpackItem(stack)) {
            return;
        }

        ItemStack previewStack = stack.copyWithCount(1);
        if (!getCachedLlmDecision(llmConfig, CopperGolemLlmService.itemId(stack), CopperGolemLlmService.itemTags(stack)).orElse(true)) {
            return;
        }

        if (canSortInto(container, previewStack) || canSortIntoAnyBackpack(container, previewStack)) {
            addUnique(itemIds, CopperGolemLlmService.itemId(stack));
        }
    }

    private static List<String> mergePreviewItemIds(List<String> manualItemIds, List<String> llmItemIds, BindingLlmConfig llmConfig) {
        Set<String> merged = new LinkedHashSet<>();
        for (String itemId : manualItemIds) {
            addUnique(merged, itemId);
        }
        for (String itemId : llmItemIds) {
            if (!llmConfig.deniedItemIds().contains(itemId)) {
                addUnique(merged, itemId);
            }
        }
        return List.copyOf(merged);
    }

    private static boolean canSortInto(Container container, ItemStack carried) {
        boolean hasMatchingItem = false;
        boolean hasEmptySlot = false;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                if (container.canPlaceItem(slot, carried)) {
                    hasEmptySlot = true;
                }
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(stack, carried) || !container.canPlaceItem(slot, carried)) {
                continue;
            }

            hasMatchingItem = true;
            int maxStackSize = Math.min(stack.getMaxStackSize(), container.getMaxStackSize(carried));
            if (stack.getCount() < maxStackSize) {
                return true;
            }
        }

        return hasMatchingItem && hasEmptySlot;
    }

    private static boolean canSortInto(CopperGolem golem, ServerLevel level, Binding binding, Container container, ItemStack carried) {
        BindingLlmConfig config = getBindingLlmConfig(golem, binding);
        String itemId = CopperGolemLlmService.itemId(carried);
        List<String> itemTags = CopperGolemLlmService.itemTags(carried);
        Optional<Boolean> cachedDecision = getCachedLlmDecision(config, itemId, itemTags);
        if (cachedDecision.isPresent() && !cachedDecision.get()) {
            return false;
        }

        if (canSortInto(container, carried) || canSortIntoAnyBackpack(container, carried)) {
            return true;
        }

        boolean hasAvailableSpace = canPlaceSomewhere(container, carried) || canPlaceSomewhereInAnyBackpack(container, carried);
        if (cachedDecision.orElse(false) && hasAvailableSpace) {
            return true;
        }

        GolemLlmConfig golemConfig = getGolemLlmConfig(golem);
        if (!config.enabled() || config.prompt().isBlank() || !golemConfig.isConfigured() || !hasAvailableSpace) {
            return false;
        }

        CopperGolemLlmService.requestClassification(
                level.getServer(),
                golem.getUUID(),
                binding,
                carried.copyWithCount(1),
                itemId,
                itemTags,
                config.prompt(),
                golemConfig.apiUrl(),
                golemConfig.apiKey(),
                golemConfig.model()
        );
        return false;
    }

    private static boolean canSortIntoAnyBackpack(Container container, ItemStack carried) {
        if (BackpackItemHelper.isBackpackItem(carried)) {
            return false;
        }

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isSortableBackpack(stack) && canSortIntoBackpack(stack, carried)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canPlaceSomewhere(Container container, ItemStack carried) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                if (container.canPlaceItem(slot, carried)) {
                    return true;
                }
                continue;
            }

            if (ItemStack.isSameItemSameComponents(stack, carried) && container.canPlaceItem(slot, carried)) {
                int maxStackSize = Math.min(stack.getMaxStackSize(), container.getMaxStackSize(carried));
                if (stack.getCount() < maxStackSize) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canPlaceSomewhereInAnyBackpack(Container container, ItemStack carried) {
        if (BackpackItemHelper.isBackpackItem(carried)) {
            return false;
        }

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isSortableBackpack(stack) && canPlaceSomewhereInBackpack(stack, carried)) {
                return true;
            }
        }
        return false;
    }

    private static NestedBackpackTarget findNestedBackpackTarget(CopperGolem golem, Binding binding, Container container, ItemStack carried) {
        if (BackpackItemHelper.isBackpackItem(carried)) {
            return null;
        }

        NestedBackpackTarget llmAllowedTarget = null;
        boolean allowLlmFallback = !canSortInto(container, carried);

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!isSortableBackpack(stack)) {
                continue;
            }

            if (canSortIntoBackpack(stack, carried)) {
                return new NestedBackpackTarget(slot, stack);
            }

            if (llmAllowedTarget == null
                    && allowLlmFallback
                    && canPlaceSomewhereInBackpack(stack, carried)
                    && hasCachedAllowedLlmDecision(golem, binding, carried)) {
                llmAllowedTarget = new NestedBackpackTarget(slot, stack);
            }
        }

        return llmAllowedTarget;
    }

    private static boolean hasCachedAllowedLlmDecision(CopperGolem golem, Binding binding, ItemStack carried) {
        BindingLlmConfig config = getBindingLlmConfig(golem, binding);
        String itemId = CopperGolemLlmService.itemId(carried);
        List<String> itemTags = CopperGolemLlmService.itemTags(carried);
        return getCachedLlmDecision(config, itemId, itemTags).orElse(false);
    }

    private static boolean canSortIntoBackpack(ItemStack backpackStack, ItemStack carried) {
        boolean hasMatchingItem = false;
        boolean hasEmptySlot = false;

        for (ItemStack stack : loadBackpackItems(backpackStack)) {
            if (stack.isEmpty()) {
                hasEmptySlot = true;
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(stack, carried)) {
                continue;
            }

            hasMatchingItem = true;
            if (stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }

        return hasMatchingItem && hasEmptySlot;
    }

    private static boolean canPlaceSomewhereInBackpack(ItemStack backpackStack, ItemStack carried) {
        for (ItemStack stack : loadBackpackItems(backpackStack)) {
            if (stack.isEmpty()) {
                return true;
            }

            if (ItemStack.isSameItemSameComponents(stack, carried) && stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack insertIntoBackpack(ItemStack backpackStack, ItemStack carried) {
        NonNullList<ItemStack> items = loadBackpackItems(backpackStack);
        ItemStack remaining = carried.copy();

        for (int slot = 0; slot < items.size() && !remaining.isEmpty(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, remaining)) {
                continue;
            }

            int moveCount = Math.min(remaining.getCount(), stack.getMaxStackSize() - stack.getCount());
            if (moveCount <= 0) {
                continue;
            }

            stack.grow(moveCount);
            remaining.shrink(moveCount);
            items.set(slot, stack);
        }

        for (int slot = 0; slot < items.size() && !remaining.isEmpty(); slot++) {
            if (!items.get(slot).isEmpty()) {
                continue;
            }

            int moveCount = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack moved = remaining.copyWithCount(moveCount);
            remaining.shrink(moveCount);
            items.set(slot, moved);
        }

        backpackStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    private static NonNullList<ItemStack> loadBackpackItems(ItemStack backpackStack) {
        int size = backpackSize(backpackStack);
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    private static int backpackSize(ItemStack backpackStack) {
        return backpackStack.getItem() instanceof TieredBackpackItem backpackItem ? backpackItem.getTier().getSlots() : 0;
    }

    private static boolean isSortableBackpack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TieredBackpackItem;
    }

    private static Optional<Boolean> getCachedLlmDecision(BindingLlmConfig config, String itemId, List<String> itemTags) {
        if (config.allowedItemIds().contains(itemId)) {
            return Optional.of(true);
        }
        if (config.deniedItemIds().contains(itemId)) {
            return Optional.of(false);
        }

        for (String tag : itemTags) {
            if (config.allowedTags().contains(tag)) {
                return Optional.of(true);
            }
            if (config.deniedTags().contains(tag)) {
                return Optional.of(false);
            }
        }

        return Optional.empty();
    }

    private static boolean hasItems(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnySortableItem(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int transportCount = Math.min(stack.getCount(), TRANSPORTED_ITEM_MAX_STACK_SIZE);
            ItemStack candidate = stack.copyWithCount(transportCount);
            if (hasSortableTarget(golem, level, candidate, sourcePos)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasSortableTarget(CopperGolem golem, ServerLevel level, ItemStack candidate, BlockPos sourcePos) {
        for (Binding binding : getBindings(golem)) {
            if (!binding.dimension().equals(level.dimension()) || binding.containerPos().equals(sourcePos)) {
                continue;
            }

            TransportItemTarget target = tryCreateBoundTarget(level, binding.containerPos());
            if (target != null && canSortInto(golem, level, binding, target.container(), candidate)) {
                return true;
            }
        }

        return false;
    }

    private static void markSortingBlocked(CopperGolem golem, ServerLevel level, BlockPos sourcePos, Container source) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        tag.putBoolean(TAG_SORTING_BLOCKED, true);
        writeBinding(tag, new Binding(level.dimension(), sourcePos.immutable()), TAG_BLOCKED_SOURCE_CONTAINER_DIM, TAG_BLOCKED_SOURCE_CONTAINER_X, TAG_BLOCKED_SOURCE_CONTAINER_Y, TAG_BLOCKED_SOURCE_CONTAINER_Z);
        tag.putInt(TAG_BLOCKED_SOURCE_HASH, hashContainer(source));
        tag.putInt(TAG_BLOCKED_BINDINGS_HASH, hashBindings(getBindings(golem)));
        tag.putInt(TAG_BLOCKED_TARGETS_HASH, hashTargetContainers(golem, level));
        tag.remove(TAG_TRIED_DESTINATIONS);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
    }

    private static void tickSortingBlockedGolem(CopperGolem golem, ServerLevel level) {
        if (golem.tickCount % SORTING_BLOCKED_JUMP_INTERVAL_TICKS == 0 && shouldClearSortingBlocked(golem, level)) {
            clearSortingBlocked(golem);
            return;
        }

        golem.getNavigation().stop();
        golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        golem.setDeltaMovement(0.0, golem.getDeltaMovement().y, 0.0);

        if (golem.onGround() && golem.tickCount % SORTING_BLOCKED_JUMP_INTERVAL_TICKS == 0) {
            golem.jumpFromGround();
            golem.setJumping(true);
        }
    }

    private static boolean shouldClearSortingBlocked(CopperGolem golem, ServerLevel level) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        Optional<Binding> blockedSource = readBinding(tag, TAG_BLOCKED_SOURCE_CONTAINER_DIM, TAG_BLOCKED_SOURCE_CONTAINER_X, TAG_BLOCKED_SOURCE_CONTAINER_Y, TAG_BLOCKED_SOURCE_CONTAINER_Z);
        if (blockedSource.isEmpty() || !blockedSource.get().dimension().equals(level.dimension())) {
            return true;
        }

        TransportItemTarget sourceTarget = TransportItemTarget.tryCreatePossibleTarget(blockedSource.get().containerPos(), level);
        if (sourceTarget == null) {
            return true;
        }

        int sourceHash = hashContainer(sourceTarget.container());
        int bindingsHash = hashBindings(getBindings(golem));
        int targetsHash = hashTargetContainers(golem, level);
        return tag.getIntOr(TAG_BLOCKED_SOURCE_HASH, 0) != sourceHash
                || tag.getIntOr(TAG_BLOCKED_BINDINGS_HASH, 0) != bindingsHash
                || tag.getIntOr(TAG_BLOCKED_TARGETS_HASH, 0) != targetsHash;
    }

    private static void clearSortingBlocked(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        if (removeSortingBlockedTags(tag)) {
            setEntityCustomDataTag(golem, tag);
            resetTransportMemories(golem);
        }
    }

    private static boolean removeSortingBlockedTags(CompoundTag tag) {
        boolean hadBlockedState = tag.getBooleanOr(TAG_SORTING_BLOCKED, false)
                || tag.contains(TAG_BLOCKED_SOURCE_CONTAINER_DIM)
                || tag.contains(TAG_BLOCKED_SOURCE_HASH)
                || tag.contains(TAG_BLOCKED_BINDINGS_HASH)
                || tag.contains(TAG_BLOCKED_TARGETS_HASH);

        tag.remove(TAG_SORTING_BLOCKED);
        tag.remove(TAG_BLOCKED_SOURCE_CONTAINER_DIM);
        tag.remove(TAG_BLOCKED_SOURCE_CONTAINER_X);
        tag.remove(TAG_BLOCKED_SOURCE_CONTAINER_Y);
        tag.remove(TAG_BLOCKED_SOURCE_CONTAINER_Z);
        tag.remove(TAG_BLOCKED_SOURCE_HASH);
        tag.remove(TAG_BLOCKED_BINDINGS_HASH);
        tag.remove(TAG_BLOCKED_TARGETS_HASH);
        return hadBlockedState;
    }

    private static int hashContainer(Container container) {
        int hash = 1;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            hash = 31 * hash + slot;
            if (stack.isEmpty()) {
                hash = 31 * hash;
                continue;
            }

            hash = 31 * hash + ItemStack.hashItemAndComponents(stack);
            hash = 31 * hash + stack.getCount();
        }
        return hash;
    }

    private static int hashBindings(List<Binding> bindings) {
        int hash = 1;
        for (Binding binding : bindings) {
            hash = 31 * hash + binding.dimension().identifier().hashCode();
            hash = 31 * hash + binding.containerPos().hashCode();
        }
        return hash;
    }

    private static int hashTargetContainers(CopperGolem golem, ServerLevel level) {
        int hash = 1;
        for (Binding binding : getBindings(golem)) {
            hash = 31 * hash + binding.dimension().identifier().hashCode();
            hash = 31 * hash + binding.containerPos().hashCode();
            if (!binding.dimension().equals(level.dimension())) {
                continue;
            }

            TransportItemTarget target = TransportItemTarget.tryCreatePossibleTarget(binding.containerPos(), level);
            hash = 31 * hash + (target == null ? 0 : hashContainer(target.container()));
        }
        return hash;
    }

    private static void rememberSource(CopperGolem golem, Level level, BlockPos sourcePos, int slot) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        writeBinding(tag, new Binding(level.dimension(), sourcePos.immutable()), TAG_SOURCE_CONTAINER_DIM, TAG_SOURCE_CONTAINER_X, TAG_SOURCE_CONTAINER_Y, TAG_SOURCE_CONTAINER_Z);
        tag.putInt(TAG_SOURCE_SLOT, slot);
        tag.remove(TAG_TRIED_DESTINATIONS);
        removeSortingBlockedTags(tag);
        setEntityCustomDataTag(golem, tag);
    }

    private static Optional<Source> getRememberedSource(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        if (!tag.contains(TAG_SOURCE_SLOT)) {
            return Optional.empty();
        }

        return readBinding(tag, TAG_SOURCE_CONTAINER_DIM, TAG_SOURCE_CONTAINER_X, TAG_SOURCE_CONTAINER_Y, TAG_SOURCE_CONTAINER_Z)
                .map(binding -> new Source(binding.dimension(), binding.containerPos(), tag.getIntOr(TAG_SOURCE_SLOT, 0)));
    }

    private static List<Binding> getTriedDestinations(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        return readBindingList(tag, TAG_TRIED_DESTINATIONS);
    }

    private static void rememberTriedDestination(CopperGolem golem, Binding binding) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        List<Binding> triedDestinations = new ArrayList<>(readBindingList(tag, TAG_TRIED_DESTINATIONS));
        if (!triedDestinations.contains(binding)) {
            triedDestinations.add(binding);
            writeBindingList(tag, TAG_TRIED_DESTINATIONS, triedDestinations);
            setEntityCustomDataTag(golem, tag);
        }
    }

    private static ItemStack insertIntoRangeBackToFront(Container container, ItemStack stack, int minSlot, int maxSlot) {
        if (stack.isEmpty() || minSlot > maxSlot) {
            return stack;
        }

        ItemStack remaining = stack.copy();
        for (int slot = maxSlot; slot >= minSlot && !remaining.isEmpty(); slot--) {
            mergeIntoSlot(container, slot, remaining);
        }
        for (int slot = maxSlot; slot >= minSlot && !remaining.isEmpty(); slot--) {
            placeIntoEmptySlot(container, slot, remaining);
        }

        return remaining;
    }

    private static ItemStack moveCarriedStackToSourceBack(Container container, ItemStack carried, int rememberedSlot) {
        if (carried.isEmpty() || container.getContainerSize() == 0) {
            return carried;
        }

        int sourceSlot = Math.max(0, Math.min(rememberedSlot, container.getContainerSize() - 1));
        ItemStack returning = carried.copy();
        returning = absorbSourceRemainder(container, sourceSlot, returning);

        ItemStack remaining = insertIntoRangeBackToFront(container, returning, sourceSlot + 1, container.getContainerSize() - 1);
        if (remaining.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (container.getItem(sourceSlot).isEmpty()) {
            if (sourceSlot < container.getContainerSize() - 1 && container.canPlaceItem(container.getContainerSize() - 1, remaining)) {
                shiftRangeLeft(container, sourceSlot, container.getContainerSize() - 1);
                container.setItem(container.getContainerSize() - 1, remaining.copy());
                return ItemStack.EMPTY;
            }

            if (container.canPlaceItem(sourceSlot, remaining)) {
                container.setItem(sourceSlot, remaining.copy());
                return ItemStack.EMPTY;
            }
        }

        return remaining;
    }

    private static ItemStack absorbSourceRemainder(Container container, int sourceSlot, ItemStack returning) {
        ItemStack sourceStack = container.getItem(sourceSlot);
        if (sourceStack.isEmpty()
                || !ItemStack.isSameItemSameComponents(sourceStack, returning)
                || !container.canPlaceItem(sourceSlot, returning)) {
            return returning;
        }

        ItemStack combined = returning.copy();
        int maxStackSize = Math.min(sourceStack.getMaxStackSize(), container.getMaxStackSize(combined));
        int moveCount = Math.min(sourceStack.getCount(), maxStackSize - combined.getCount());
        if (moveCount <= 0) {
            return combined;
        }

        combined.grow(moveCount);
        sourceStack.shrink(moveCount);
        container.setItem(sourceSlot, sourceStack.isEmpty() ? ItemStack.EMPTY : sourceStack);
        return combined;
    }

    private static void shiftRangeLeft(Container container, int emptySlot, int lastSlot) {
        for (int slot = emptySlot; slot < lastSlot; slot++) {
            container.setItem(slot, container.getItem(slot + 1).copy());
        }
    }

    private static void mergeIntoSlot(Container container, int slot, ItemStack remaining) {
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, remaining) || !container.canPlaceItem(slot, remaining)) {
            return;
        }

        int maxStackSize = Math.min(stack.getMaxStackSize(), container.getMaxStackSize(remaining));
        int moveCount = Math.min(remaining.getCount(), maxStackSize - stack.getCount());
        if (moveCount <= 0) {
            return;
        }

        stack.grow(moveCount);
        remaining.shrink(moveCount);
        container.setItem(slot, stack);
    }

    private static void placeIntoEmptySlot(Container container, int slot, ItemStack remaining) {
        if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, remaining)) {
            return;
        }

        int moveCount = Math.min(remaining.getCount(), container.getMaxStackSize(remaining));
        ItemStack moved = remaining.copyWithCount(moveCount);
        remaining.shrink(moveCount);
        container.setItem(slot, moved);
    }

    private static void showParticlePath(ServerLevel world, Entity golem, BlockPos targetPos) {
        Vec3 start = golem.position().add(0.0, golem.getBbHeight() * 0.6, 0.0);
        Vec3 end = Vec3.atCenterOf(targetPos);
        int points = 28;

        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            double x = start.x + (end.x - start.x) * t;
            double y = start.y + (end.y - start.y) * t;
            double z = start.z + (end.z - start.z) * t;
            world.sendParticles(ParticleTypes.WAX_ON, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static boolean isCopperGolem(Entity entity) {
        return entity instanceof CopperGolem;
    }

    public static UUID getSelectedGolem(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.read(TAG_SELECTED_GOLEM, UUIDUtil.CODEC).orElse(null);
    }

    private static void setSelectedGolem(ItemStack stack, UUID golemId) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.store(TAG_SELECTED_GOLEM, UUIDUtil.CODEC, golemId);
        setCustomDataTag(stack, tag);
    }

    private static void clearSelection(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.remove(TAG_SELECTED_GOLEM);
        setCustomDataTag(stack, tag);
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag();
    }

    private static void setCustomDataTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag getEntityCustomDataTag(Entity entity) {
        CustomData customData = entity.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private static void setEntityCustomDataTag(Entity entity, CompoundTag tag) {
        entity.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static List<Binding> readBindings(CompoundTag tag) {
        List<Binding> bindings = new ArrayList<>(readBindingList(tag, TAG_BOUND_CONTAINERS));

        readBinding(tag, TAG_BOUND_CONTAINER_DIM, TAG_BOUND_CONTAINER_X, TAG_BOUND_CONTAINER_Y, TAG_BOUND_CONTAINER_Z)
                .filter(binding -> !bindings.contains(binding))
                .ifPresent(bindings::add);
        return List.copyOf(bindings);
    }

    private static List<Binding> readBindingList(CompoundTag tag, String listKey) {
        List<Binding> bindings = new ArrayList<>();
        tag.getList(listKey).ifPresent(list -> {
            for (CompoundTag bindingTag : list.compoundStream().toList()) {
                readBinding(bindingTag, TAG_BINDING_DIM, TAG_BINDING_X, TAG_BINDING_Y, TAG_BINDING_Z)
                        .ifPresent(bindings::add);
            }
        });
        return bindings;
    }

    private static Optional<Binding> readBinding(CompoundTag tag, String dimKey, String xKey, String yKey, String zKey) {
        if (!tag.contains(dimKey) || !tag.contains(xKey) || !tag.contains(yKey) || !tag.contains(zKey)) {
            return Optional.empty();
        }

        Identifier dimensionId = Identifier.tryParse(tag.getStringOr(dimKey, ""));
        if (dimensionId == null) {
            return Optional.empty();
        }

        return Optional.of(new Binding(
                net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimensionId),
                new BlockPos(tag.getIntOr(xKey, 0), tag.getIntOr(yKey, 0), tag.getIntOr(zKey, 0))
        ));
    }

    private static void writeBindings(CompoundTag tag, List<Binding> bindings) {
        writeBindingList(tag, TAG_BOUND_CONTAINERS, bindings);
        tag.remove(TAG_BOUND_CONTAINER_DIM);
        tag.remove(TAG_BOUND_CONTAINER_X);
        tag.remove(TAG_BOUND_CONTAINER_Y);
        tag.remove(TAG_BOUND_CONTAINER_Z);
    }

    private static void writeBindingList(CompoundTag tag, String listKey, List<Binding> bindings) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (Binding binding : bindings) {
            CompoundTag bindingTag = new CompoundTag();
            writeBinding(bindingTag, binding, TAG_BINDING_DIM, TAG_BINDING_X, TAG_BINDING_Y, TAG_BINDING_Z);
            list.add(bindingTag);
        }

        tag.put(listKey, list);
    }

    private static void writeBinding(CompoundTag tag, Binding binding, String dimKey, String xKey, String yKey, String zKey) {
        tag.putString(dimKey, binding.dimension().identifier().toString());
        tag.putInt(xKey, binding.containerPos().getX());
        tag.putInt(yKey, binding.containerPos().getY());
        tag.putInt(zKey, binding.containerPos().getZ());
    }

    private static Optional<BlockPos> readBlockPos(CompoundTag tag, String xKey, String yKey, String zKey) {
        if (!tag.contains(xKey) || !tag.contains(yKey) || !tag.contains(zKey)) {
            return Optional.empty();
        }

        return Optional.of(new BlockPos(tag.getIntOr(xKey, 0), tag.getIntOr(yKey, 0), tag.getIntOr(zKey, 0)));
    }

    private static void writeBlockPos(CompoundTag tag, BlockPos pos, String xKey, String yKey, String zKey) {
        tag.putInt(xKey, pos.getX());
        tag.putInt(yKey, pos.getY());
        tag.putInt(zKey, pos.getZ());
    }

    private static void clearGatheringAreaTags(CompoundTag tag) {
        tag.remove(TAG_GATHERING_AREA_DIM);
        tag.remove(TAG_GATHERING_CORNER_A_X);
        tag.remove(TAG_GATHERING_CORNER_A_Y);
        tag.remove(TAG_GATHERING_CORNER_A_Z);
        tag.remove(TAG_GATHERING_CORNER_B_X);
        tag.remove(TAG_GATHERING_CORNER_B_Y);
        tag.remove(TAG_GATHERING_CORNER_B_Z);
    }

    private static BindingLlmConfig getBindingLlmConfig(CopperGolem golem, Binding binding) {
        return getBindingLlmConfig(readBindingLlmConfigs(getEntityCustomDataTag(golem)), binding);
    }

    private static GolemLlmConfig getGolemLlmConfig(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        return new GolemLlmConfig(
                tag.getStringOr(TAG_LLM_API_URL, ""),
                tag.getStringOr(TAG_LLM_API_KEY, ""),
                tag.getStringOr(TAG_LLM_MODEL, "")
        );
    }

    private static void setGolemLlmConfig(CopperGolem golem, String apiUrl, String apiKey, String model) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        putOrRemoveString(tag, TAG_LLM_API_URL, apiUrl);
        putOrRemoveString(tag, TAG_LLM_API_KEY, apiKey);
        putOrRemoveString(tag, TAG_LLM_MODEL, model);
        removeSortingBlockedTags(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
    }

    private static GatheringLlmConfig getGatheringLlmConfig(CopperGolem golem) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        return new GatheringLlmConfig(
                tag.getBooleanOr(TAG_GATHERING_LLM_ENABLED, false),
                tag.getStringOr(TAG_GATHERING_LLM_PROMPT, ""),
                tag.getIntOr(TAG_GATHERING_LLM_PROMPT_REVISION, 0),
                readStringList(tag, TAG_GATHERING_LLM_ALLOWED_BLOCK_IDS),
                readStringList(tag, TAG_GATHERING_LLM_DENIED_BLOCK_IDS),
                readStringList(tag, TAG_GATHERING_LLM_ALLOWED_TAGS),
                readStringList(tag, TAG_GATHERING_LLM_DENIED_TAGS)
        );
    }

    private static void setGatheringLlmConfig(CopperGolem golem, boolean enabled, String prompt) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        GatheringLlmConfig current = getGatheringLlmConfig(golem);
        String normalizedPrompt = prompt == null ? "" : prompt.trim();
        boolean promptChanged = !normalizedPrompt.equals(current.prompt());

        tag.putBoolean(TAG_GATHERING_LLM_ENABLED, enabled);
        putOrRemoveString(tag, TAG_GATHERING_LLM_PROMPT, normalizedPrompt);
        if (promptChanged) {
            tag.putInt(TAG_GATHERING_LLM_PROMPT_REVISION, current.promptRevision() + 1);
            tag.remove(TAG_GATHERING_LLM_ALLOWED_BLOCK_IDS);
            tag.remove(TAG_GATHERING_LLM_DENIED_BLOCK_IDS);
            tag.remove(TAG_GATHERING_LLM_ALLOWED_TAGS);
            tag.remove(TAG_GATHERING_LLM_DENIED_TAGS);
        }
        resetGatheringSearch(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
    }

    public static void recordGatheringLlmDecision(
            CopperGolem golem,
            String blockId,
            List<String> blockTags,
            boolean allowed,
            List<String> acceptedTags,
            int promptRevision) {
        GatheringLlmConfig config = getGatheringLlmConfig(golem);
        if (promptRevision != config.promptRevision()) {
            return;
        }

        CompoundTag tag = getEntityCustomDataTag(golem);
        List<String> allowedBlockIds = new ArrayList<>(config.allowedBlockIds());
        List<String> deniedBlockIds = new ArrayList<>(config.deniedBlockIds());
        List<String> allowedTags = new ArrayList<>(config.allowedTags());
        List<String> deniedTags = new ArrayList<>(config.deniedTags());

        if (allowed) {
            addUnique(allowedBlockIds, blockId);
            deniedBlockIds.remove(blockId);
            for (String tagId : acceptedTags) {
                if (blockTags.contains(tagId)) {
                    addUnique(allowedTags, tagId);
                    deniedTags.remove(tagId);
                }
            }
        } else {
            addUnique(deniedBlockIds, blockId);
            allowedBlockIds.remove(blockId);
            for (String tagId : acceptedTags) {
                if (blockTags.contains(tagId)) {
                    addUnique(deniedTags, tagId);
                    allowedTags.remove(tagId);
                }
            }
        }

        writeStringList(tag, TAG_GATHERING_LLM_ALLOWED_BLOCK_IDS, allowedBlockIds);
        writeStringList(tag, TAG_GATHERING_LLM_DENIED_BLOCK_IDS, deniedBlockIds);
        writeStringList(tag, TAG_GATHERING_LLM_ALLOWED_TAGS, allowedTags);
        writeStringList(tag, TAG_GATHERING_LLM_DENIED_TAGS, deniedTags);
        resetGatheringSearch(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
    }

    private static boolean removeGatheringLlmCache(CopperGolem golem, String value, boolean tagValue, boolean allowed) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return false;
        }

        CompoundTag tag = getEntityCustomDataTag(golem);
        GatheringLlmConfig config = getGatheringLlmConfig(golem);
        List<String> allowedBlockIds = new ArrayList<>(config.allowedBlockIds());
        List<String> deniedBlockIds = new ArrayList<>(config.deniedBlockIds());
        List<String> allowedTags = new ArrayList<>(config.allowedTags());
        List<String> deniedTags = new ArrayList<>(config.deniedTags());
        boolean removed = tagValue
                ? (allowed ? allowedTags : deniedTags).remove(normalized)
                : (allowed ? allowedBlockIds : deniedBlockIds).remove(normalized);
        if (!removed) {
            return false;
        }

        writeStringList(tag, TAG_GATHERING_LLM_ALLOWED_BLOCK_IDS, allowedBlockIds);
        writeStringList(tag, TAG_GATHERING_LLM_DENIED_BLOCK_IDS, deniedBlockIds);
        writeStringList(tag, TAG_GATHERING_LLM_ALLOWED_TAGS, allowedTags);
        writeStringList(tag, TAG_GATHERING_LLM_DENIED_TAGS, deniedTags);
        resetGatheringSearch(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
        return true;
    }

    private static BindingLlmConfig getBindingLlmConfig(List<BindingLlmConfig> configs, Binding binding) {
        for (BindingLlmConfig config : configs) {
            if (config.binding().equals(binding)) {
                return config;
            }
        }

        return new BindingLlmConfig(binding, false, "", List.of(), List.of(), List.of(), List.of());
    }

    private static void setBindingLlmConfig(CopperGolem golem, Binding binding, boolean enabled, String prompt) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        List<BindingLlmConfig> configs = new ArrayList<>(readBindingLlmConfigs(tag));
        BindingLlmConfig current = getBindingLlmConfig(configs, binding);
        String normalizedPrompt = prompt == null ? "" : prompt.trim();
        boolean promptChanged = !normalizedPrompt.equals(current.prompt());
        putBindingLlmConfig(configs, new BindingLlmConfig(
                binding,
                enabled,
                normalizedPrompt,
                promptChanged ? List.of() : current.allowedItemIds(),
                promptChanged ? List.of() : current.deniedItemIds(),
                promptChanged ? List.of() : current.allowedTags(),
                promptChanged ? List.of() : current.deniedTags()
        ));
        writeBindingLlmConfigs(tag, configs);
        removeSortingBlockedTags(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
    }

    private static void moveBindingLlmCache(CopperGolem golem, Binding binding, String value, boolean tagValue, boolean allowed) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        List<BindingLlmConfig> configs = new ArrayList<>(readBindingLlmConfigs(tag));
        BindingLlmConfig current = getBindingLlmConfig(configs, binding);

        List<String> allowedItemIds = new ArrayList<>(current.allowedItemIds());
        List<String> deniedItemIds = new ArrayList<>(current.deniedItemIds());
        List<String> allowedTags = new ArrayList<>(current.allowedTags());
        List<String> deniedTags = new ArrayList<>(current.deniedTags());

        if (tagValue) {
            moveCacheValue(value, allowed, allowedTags, deniedTags);
        } else {
            moveCacheValue(value, allowed, allowedItemIds, deniedItemIds);
        }

        putBindingLlmConfig(configs, new BindingLlmConfig(
                binding,
                current.enabled(),
                current.prompt(),
                allowedItemIds,
                deniedItemIds,
                allowedTags,
                deniedTags
        ));
        writeBindingLlmConfigs(tag, configs);
        removeSortingBlockedTags(tag);
        bumpRevision(tag);
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
    }

    private static void moveCacheValue(String value, boolean allowed, List<String> allowedValues, List<String> deniedValues) {
        if (allowed) {
            addUnique(allowedValues, value);
            deniedValues.remove(value);
        } else {
            addUnique(deniedValues, value);
            allowedValues.remove(value);
        }
    }

    private static int countActiveLlmBindings(CopperGolem golem) {
        int count = 0;
        for (BindingLlmConfig config : readBindingLlmConfigs(getEntityCustomDataTag(golem))) {
            if (config.enabled()) {
                count++;
            }
        }
        return count;
    }

    private static List<BindingLlmConfig> readBindingLlmConfigs(CompoundTag tag) {
        List<BindingLlmConfig> configs = new ArrayList<>();
        tag.getList(TAG_LLM_BINDINGS).ifPresent(list -> {
            for (CompoundTag configTag : list.compoundStream().toList()) {
                Optional<Binding> binding = readBinding(configTag, TAG_BINDING_DIM, TAG_BINDING_X, TAG_BINDING_Y, TAG_BINDING_Z);
                if (binding.isEmpty()) {
                    continue;
                }

                configs.add(new BindingLlmConfig(
                        binding.get(),
                        configTag.getBooleanOr(TAG_LLM_ENABLED, false),
                        configTag.getStringOr(TAG_LLM_PROMPT, ""),
                        readStringList(configTag, TAG_LLM_ALLOWED_ITEM_IDS),
                        readStringList(configTag, TAG_LLM_DENIED_ITEM_IDS),
                        readStringList(configTag, TAG_LLM_ALLOWED_TAGS),
                        readStringList(configTag, TAG_LLM_DENIED_TAGS)
                ));
            }
        });
        return configs;
    }

    private static void writeBindingLlmConfigs(CompoundTag tag, List<BindingLlmConfig> configs) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (BindingLlmConfig config : configs) {
            if (!shouldKeepBindingLlmConfig(config)) {
                continue;
            }

            CompoundTag configTag = new CompoundTag();
            writeBinding(configTag, config.binding(), TAG_BINDING_DIM, TAG_BINDING_X, TAG_BINDING_Y, TAG_BINDING_Z);
            configTag.putBoolean(TAG_LLM_ENABLED, config.enabled());
            configTag.putString(TAG_LLM_PROMPT, config.prompt());
            writeStringList(configTag, TAG_LLM_ALLOWED_ITEM_IDS, config.allowedItemIds());
            writeStringList(configTag, TAG_LLM_DENIED_ITEM_IDS, config.deniedItemIds());
            writeStringList(configTag, TAG_LLM_ALLOWED_TAGS, config.allowedTags());
            writeStringList(configTag, TAG_LLM_DENIED_TAGS, config.deniedTags());
            list.add(configTag);
        }

        if (list.isEmpty()) {
            tag.remove(TAG_LLM_BINDINGS);
        } else {
            tag.put(TAG_LLM_BINDINGS, list);
        }
    }

    private static void pruneBindingLlmConfigs(CompoundTag tag, List<Binding> bindings) {
        List<BindingLlmConfig> kept = readBindingLlmConfigs(tag).stream()
                .filter(config -> bindings.contains(config.binding()))
                .toList();
        writeBindingLlmConfigs(tag, kept);
    }

    private static boolean shouldKeepBindingLlmConfig(BindingLlmConfig config) {
        return config.enabled()
                || !config.prompt().isBlank()
                || !config.allowedItemIds().isEmpty()
                || !config.deniedItemIds().isEmpty()
                || !config.allowedTags().isEmpty()
                || !config.deniedTags().isEmpty();
    }

    private static void putBindingLlmConfig(List<BindingLlmConfig> configs, BindingLlmConfig config) {
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).binding().equals(config.binding())) {
                configs.set(i, config);
                return;
            }
        }
        configs.add(config);
    }

    private static List<String> readStringList(CompoundTag tag, String key) {
        List<String> values = new ArrayList<>();
        tag.getList(key).ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) {
                String value = list.getStringOr(i, "");
                if (!value.isBlank() && !values.contains(value)) {
                    values.add(value);
                }
            }
        });
        return List.copyOf(values);
    }

    private static void writeStringList(CompoundTag tag, String key, List<String> values) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (String value : values) {
            if (list.size() >= LLM_CACHE_VALUE_LIMIT) {
                break;
            }
            if (!value.isBlank()) {
                list.add(StringTag.valueOf(value));
            }
        }

        if (list.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, list);
        }
    }

    private static void addUnique(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }

    private static void addUnique(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private static void putOrRemoveString(CompoundTag tag, String key, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            tag.remove(key);
        } else {
            tag.putString(key, normalized);
        }
    }

    private static boolean canManageLlmConfig(ServerPlayer player) {
        if (player.getAbilities().instabuild || player.isCreative()) {
            return true;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        if (server.isSingleplayer()) {
            var owner = server.getSingleplayerProfile();
            if (owner != null && owner.id().equals(player.getGameProfile().id())) {
                return true;
            }
        }

        return server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));
    }

    private static void notify(Player player, Component message) {
        player.sendOverlayMessage(message);
    }

    public record Binding(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos containerPos) {
    }

    private record GatheringTargetClickKey(
            UUID playerId,
            UUID golemId,
            net.minecraft.resources.ResourceKey<Level> dimension,
            BlockPos pos,
            String blockId) {
    }

    private record GatheringHome(Binding binding, Container container) {
    }

    private record GatheringArea(
            net.minecraft.resources.ResourceKey<Level> dimension,
            Optional<BlockPos> cornerA,
            Optional<BlockPos> cornerB) {
    }

    private record GatheringAreaBounds(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            long volume) {
        private boolean contains(BlockPos pos) {
            return pos.getX() >= this.minX
                    && pos.getX() <= this.maxX
                    && pos.getY() >= this.minY
                    && pos.getY() <= this.maxY
                    && pos.getZ() >= this.minZ
                    && pos.getZ() <= this.maxZ;
        }

        private BlockPos positionAt(long index) {
            long sizeX = (long) this.maxX - this.minX + 1L;
            long sizeY = (long) this.maxY - this.minY + 1L;
            long xOffset = index % sizeX;
            long yOffset = (index / sizeX) % sizeY;
            long zOffset = index / (sizeX * sizeY);
            return new BlockPos(
                    this.minX + (int) xOffset,
                    this.minY + (int) yOffset,
                    this.minZ + (int) zOffset
            );
        }

        private BlockPos topDownPositionAt(long index) {
            long sizeX = (long) this.maxX - this.minX + 1L;
            long sizeZ = (long) this.maxZ - this.minZ + 1L;
            long layerSize = sizeX * sizeZ;
            long layerIndex = index % layerSize;
            long yOffset = index / layerSize;
            long xOffset = layerIndex % sizeX;
            long zOffset = layerIndex / sizeX;
            return new BlockPos(
                    this.minX + (int) xOffset,
                    this.maxY - (int) yOffset,
                    this.minZ + (int) zOffset
            );
        }
    }

    private record BindingLlmConfig(
            Binding binding,
            boolean enabled,
            String prompt,
            List<String> allowedItemIds,
            List<String> deniedItemIds,
            List<String> allowedTags,
            List<String> deniedTags) {
    }

    private record GolemLlmConfig(String apiUrl, String apiKey, String model) {
        private boolean isConfigured() {
            return !apiUrl.isBlank() && !model.isBlank();
        }
    }

    private record GatheringLlmConfig(
            boolean enabled,
            String prompt,
            int promptRevision,
            List<String> allowedBlockIds,
            List<String> deniedBlockIds,
            List<String> allowedTags,
            List<String> deniedTags) {
        private boolean isUsable(GolemLlmConfig golemConfig) {
            return enabled && !prompt.isBlank() && golemConfig.isConfigured();
        }
    }

    private record Source(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos containerPos, int slot) {
    }

    private record NestedBackpackTarget(int containerSlot, ItemStack backpackStack) {
    }
}
