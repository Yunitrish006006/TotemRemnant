package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.network.CopperWrenchBindingsPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CopperGolemWrenchHandler {
    private static final String TAG_SELECTED_GOLEM = "deadrecall_selected_golem";

    private static final String TAG_BOUND_CONTAINER_DIM = "deadrecall_bound_container_dim";
    private static final String TAG_BOUND_CONTAINER_X = "deadrecall_bound_container_x";
    private static final String TAG_BOUND_CONTAINER_Y = "deadrecall_bound_container_y";
    private static final String TAG_BOUND_CONTAINER_Z = "deadrecall_bound_container_z";
    private static final String TAG_BOUND_CONTAINERS = "deadrecall_bound_containers";
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
    private static final String TAG_LLM_ENABLED = "llm_enabled";
    private static final String TAG_LLM_PROMPT = "llm_prompt";
    private static final String TAG_LLM_ALLOWED_ITEM_IDS = "llm_allowed_item_ids";
    private static final String TAG_LLM_DENIED_ITEM_IDS = "llm_denied_item_ids";
    private static final String TAG_LLM_ALLOWED_TAGS = "llm_allowed_tags";
    private static final String TAG_LLM_DENIED_TAGS = "llm_denied_tags";
    private static final int TRANSPORTED_ITEM_MAX_STACK_SIZE = 16;
    private static final int SORTING_BLOCKED_JUMP_INTERVAL_TICKS = 10;
    private static final int PRUNE_BINDINGS_INTERVAL_TICKS = 20;
    private static int pruneBindingsTicker = 0;

    private CopperGolemWrenchHandler() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.COPPER_WRENCH) || !isCopperGolem(entity)) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            CopperGolem golem = (CopperGolem) entity;
            if (player.isShiftKeyDown()) {
                return showBoundContainerPaths(player, (ServerLevel) world, golem);
            }

            setSelectedGolem(stack, golem.getUUID());
            notify(player, Component.translatable("message.deadrecall.copper_wrench.select_container"));
            return InteractionResult.SUCCESS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, clickedPos, direction) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isSpectator() || !stack.is(ModItems.COPPER_WRENCH)) {
                return InteractionResult.PASS;
            }

            BlockEntity blockEntity = world.getBlockEntity(clickedPos);
            if (!(blockEntity instanceof Container)) {
                return InteractionResult.PASS;
            }

            UUID selectedGolem = getSelectedGolem(stack);
            if (selectedGolem == null) {
                if (!world.isClientSide()) {
                    notify(player, Component.translatable("message.deadrecall.copper_wrench.left_click_select"));
                }
                return InteractionResult.SUCCESS;
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

            Binding binding = new Binding(world.dimension(), clickedPos.immutable());
            boolean removed = removeBinding(golem, binding);
            notify(player, Component.translatable(removed
                    ? "message.deadrecall.copper_wrench.unbind_success"
                    : "message.deadrecall.copper_wrench.unbind_missing"));
            return InteractionResult.SUCCESS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.COPPER_WRENCH) || !isCopperGolem(entity)) {
                return InteractionResult.PASS;
            }

            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            CopperGolem golem = (CopperGolem) entity;
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
            BlockEntity blockEntity = world.getBlockEntity(clickedPos);
            if (!(blockEntity instanceof Container)) {
                notify(player, Component.translatable("message.deadrecall.copper_wrench.need_container"));
                return InteractionResult.SUCCESS;
            }

            if (isCopperSourceContainer(world, clickedPos)) {
                notify(player, Component.translatable("message.deadrecall.copper_wrench.cannot_bind_copper_source"));
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
            boolean added = addBinding(golem, binding);
            showParticlePath(serverLevel, golem, clickedPos);
            notify(player, Component.translatable(added
                    ? "message.deadrecall.copper_wrench.bind_success"
                    : "message.deadrecall.copper_wrench.bind_duplicate"));
            return InteractionResult.SUCCESS;
        });
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

    public static boolean isTransportEnabled(CopperGolem golem) {
        return getEntityCustomDataTag(golem).getBooleanOr(TAG_TRANSPORT_ENABLED, true);
    }

    public static boolean isSortingBlocked(CopperGolem golem) {
        return getEntityCustomDataTag(golem).getBooleanOr(TAG_SORTING_BLOCKED, false);
    }

    public static void setTransportEnabledFromUi(ServerPlayer player, UUID golemId, boolean enabled) {
        Entity entity = player.level().getEntityInAnyDimension(golemId);
        if (!(entity instanceof CopperGolem golem)) {
            return;
        }

        setTransportEnabled(golem, enabled);
        sendBindingListUi(player, golem);
    }

    public static void setBindingLlmFromUi(ServerPlayer player, UUID golemId, String dimensionId, int x, int y, int z, boolean enabled, String prompt) {
        Entity entity = player.level().getEntityInAnyDimension(golemId);
        if (!(entity instanceof CopperGolem golem)) {
            return;
        }

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

    public static void setGolemLlmConfigFromUi(ServerPlayer player, UUID golemId, String apiUrl, String apiKey, String model) {
        Entity entity = player.level().getEntityInAnyDimension(golemId);
        if (!(entity instanceof CopperGolem golem)) {
            return;
        }

        setGolemLlmConfig(golem, apiUrl, apiKey, model);
        sendBindingListUi(player, golem);
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
                        pruneUnavailableBindings(golem, server);
                    }
                    if (isTransportEnabled(golem) && isSortingBlocked(golem)) {
                        tickSortingBlockedGolem(golem, level);
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

    private static void setBindings(CopperGolem golem, List<Binding> bindings) {
        CompoundTag tag = getEntityCustomDataTag(golem);
        writeBindings(tag, bindings);
        pruneBindingLlmConfigs(tag, bindings);
        removeSortingBlockedTags(tag);
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
        setEntityCustomDataTag(golem, tag);
        if (!enabled && golem.level() instanceof ServerLevel level) {
            returnCarriedItemToSource(golem, level);
        }
        resetTransportMemories(golem);
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
        pruneUnavailableBindings(golem, player.level().getServer());
        List<Binding> bindings = getBindings(golem);
        List<CopperWrenchBindingsPayload.BindingEntry> entries = new ArrayList<>(bindings.size());
        MinecraftServer server = player.level().getServer();
        for (Binding binding : bindings) {
            entries.add(createBindingEntry(server, golem, binding));
        }

        boolean canManageLlmConfig = canManageLlmConfig(player);
        GolemLlmConfig llmConfig = getGolemLlmConfig(golem);
        ServerPlayNetworking.send(player, new CopperWrenchBindingsPayload(
                golem.getUUID(),
                isTransportEnabled(golem),
                llmConfig.apiUrl(),
                canManageLlmConfig ? llmConfig.apiKey() : "",
                llmConfig.model(),
                countActiveLlmBindings(golem),
                entries
        ));
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
                llmConfig.allowedItemIds(),
                llmConfig.deniedItemIds(),
                llmConfig.allowedTags(),
                llmConfig.deniedTags()
        );
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
        if (canSortInto(container, carried)) {
            return true;
        }

        BindingLlmConfig config = getBindingLlmConfig(golem, binding);
        GolemLlmConfig golemConfig = getGolemLlmConfig(golem);
        if (!config.enabled() || config.prompt().isBlank() || !golemConfig.isConfigured() || !canPlaceSomewhere(container, carried)) {
            return false;
        }

        String itemId = CopperGolemLlmService.itemId(carried);
        List<String> itemTags = CopperGolemLlmService.itemTags(carried);
        Optional<Boolean> cachedDecision = getCachedLlmDecision(config, itemId, itemTags);
        if (cachedDecision.isPresent()) {
            return cachedDecision.get();
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

    private static UUID getSelectedGolem(ItemStack stack) {
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
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
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
        setEntityCustomDataTag(golem, tag);
        resetTransportMemories(golem);
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

    private record Source(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos containerPos, int slot) {
    }
}
