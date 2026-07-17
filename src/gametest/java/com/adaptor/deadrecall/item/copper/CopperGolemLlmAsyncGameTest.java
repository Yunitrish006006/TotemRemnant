package com.adaptor.deadrecall.item.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public final class CopperGolemLlmAsyncGameTest {
    private static final BlockPos GOLEM_POS = new BlockPos(2, 2, 2);

    @GameTest(maxTicks = 30)
    public void gatheringCallbackRejectsOldPromptRevision(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        try {
            setGatheringLlmConfig(golem, true, "old gathering prompt");
            int oldRevision = gatheringPromptRevision(golem);

            setGatheringLlmConfig(golem, true, "new gathering prompt");
            int currentRevision = gatheringPromptRevision(golem);
            require(helper, currentRevision > oldRevision,
                    "Changing the gathering prompt did not advance its revision");

            List<String> tags = List.of("minecraft:mineable/pickaxe");
            CopperGolemLlmClient.Decision allowed = new CopperGolemLlmClient.Decision(true, tags);
            BlockLlmClassifier.applyDecisionIfCurrent(
                    golem,
                    "minecraft:stone",
                    tags,
                    allowed,
                    oldRevision
            );

            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            require(helper, CopperGolemData.readStringList(tag, "deadrecall_gathering_llm_allowed_block_ids").isEmpty(),
                    "A stale gathering response repopulated the block cache");
            require(helper, CopperGolemData.readStringList(tag, "deadrecall_gathering_llm_allowed_tags").isEmpty(),
                    "A stale gathering response repopulated the tag cache");

            BlockLlmClassifier.applyDecisionIfCurrent(
                    golem,
                    "minecraft:stone",
                    tags,
                    allowed,
                    currentRevision
            );
            tag = CopperGolemData.readEntityTag(golem);
            require(helper, CopperGolemData.readStringList(tag, "deadrecall_gathering_llm_allowed_block_ids")
                            .equals(List.of("minecraft:stone")),
                    "The current gathering response was not cached");
            require(helper, CopperGolemData.readStringList(tag, "deadrecall_gathering_llm_allowed_tags")
                            .equals(tags),
                    "The current gathering response did not preserve its accepted provided tag");

            CopperGolemLlmClient.Decision staleDenied = new CopperGolemLlmClient.Decision(false, tags);
            BlockLlmClassifier.applyDecisionIfCurrent(
                    golem,
                    "minecraft:stone",
                    tags,
                    staleDenied,
                    oldRevision
            );
            tag = CopperGolemData.readEntityTag(golem);
            require(helper, CopperGolemData.readStringList(tag, "deadrecall_gathering_llm_allowed_block_ids")
                            .equals(List.of("minecraft:stone")),
                    "A late stale denial replaced the current gathering decision");
            require(helper, CopperGolemData.readStringList(tag, "deadrecall_gathering_llm_denied_block_ids").isEmpty(),
                    "A late stale denial polluted the current gathering cache");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    @GameTest(maxTicks = 30)
    public void sortingCallbackRequiresCurrentEnabledPrompt(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        CopperGolemWrenchHandler.Binding binding = new CopperGolemWrenchHandler.Binding(
                Level.OVERWORLD,
                helper.absolutePos(new BlockPos(5, 2, 2))
        );
        try {
            setBindings(golem, List.of(binding));
            setBindingLlmConfig(golem, binding, true, "old sorting prompt");
            setBindingLlmConfig(golem, binding, true, "new sorting prompt");

            List<String> tags = List.of("minecraft:ores");
            CopperGolemLlmClient.Decision allowed = new CopperGolemLlmClient.Decision(true, tags);
            CopperGolemLlmService.applyDecisionIfCurrent(
                    golem,
                    binding,
                    "old sorting prompt",
                    "minecraft:diamond",
                    tags,
                    allowed
            );

            CopperGolemWrenchHandler.BindingLlmConfig config =
                    CopperGolemWrenchHandler.getBindingLlmConfig(golem, binding);
            require(helper, config.allowedItemIds().isEmpty() && config.allowedTags().isEmpty(),
                    "A stale sorting response repopulated the cleared cache");

            CopperGolemLlmService.applyDecisionIfCurrent(
                    golem,
                    binding,
                    "new sorting prompt",
                    "minecraft:diamond",
                    tags,
                    allowed
            );
            config = CopperGolemWrenchHandler.getBindingLlmConfig(golem, binding);
            require(helper, config.allowedItemIds().equals(List.of("minecraft:diamond")),
                    "The current sorting response was not cached");
            require(helper, config.allowedTags().equals(tags),
                    "The current sorting response did not cache its accepted tags");

            CopperGolemLlmClient.Decision staleDenied = new CopperGolemLlmClient.Decision(false, tags);
            CopperGolemLlmService.applyDecisionIfCurrent(
                    golem,
                    binding,
                    "old sorting prompt",
                    "minecraft:diamond",
                    tags,
                    staleDenied
            );
            config = CopperGolemWrenchHandler.getBindingLlmConfig(golem, binding);
            require(helper, config.allowedItemIds().equals(List.of("minecraft:diamond"))
                            && config.deniedItemIds().isEmpty(),
                    "A late stale sorting denial replaced the current decision");

            setBindingLlmConfig(golem, binding, false, "new sorting prompt");
            CopperGolemLlmService.applyDecisionIfCurrent(
                    golem,
                    binding,
                    "new sorting prompt",
                    "minecraft:emerald",
                    List.of(),
                    new CopperGolemLlmClient.Decision(true, List.of())
            );
            config = CopperGolemWrenchHandler.getBindingLlmConfig(golem, binding);
            require(helper, !config.allowedItemIds().contains("minecraft:emerald"),
                    "A response was cached after the sorting classifier was disabled");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    private static int gatheringPromptRevision(CopperGolem golem) {
        return CopperGolemData.readEntityTag(golem)
                .getIntOr("deadrecall_gathering_llm_prompt_revision", 0);
    }

    private static void setGatheringLlmConfig(CopperGolem golem, boolean enabled, String prompt) {
        invoke("setGatheringLlmConfig",
                new Class<?>[]{CopperGolem.class, boolean.class, String.class},
                golem, enabled, prompt);
    }

    private static void setBindingLlmConfig(
            CopperGolem golem,
            CopperGolemWrenchHandler.Binding binding,
            boolean enabled,
            String prompt
    ) {
        invoke("setBindingLlmConfig",
                new Class<?>[]{CopperGolem.class, CopperGolemWrenchHandler.Binding.class, boolean.class, String.class},
                golem, binding, enabled, prompt);
    }

    private static void setBindings(CopperGolem golem, List<CopperGolemWrenchHandler.Binding> bindings) {
        invoke("setBindings", new Class<?>[]{CopperGolem.class, List.class}, golem, bindings);
    }

    private static CopperGolem createGolem(GameTestHelper helper, BlockPos relativePos) {
        Object entityType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
        if (entityType == null) {
            throw helper.assertionException("Missing minecraft:copper_golem entity type");
        }

        try {
            for (Constructor<?> constructor : CopperGolem.class.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 2
                        || !parameterTypes[0].isInstance(entityType)
                        || !parameterTypes[1].isInstance(helper.getLevel())) {
                    continue;
                }

                constructor.setAccessible(true);
                CopperGolem golem = (CopperGolem) constructor.newInstance(entityType, helper.getLevel());
                BlockPos absolutePos = helper.absolutePos(relativePos);
                golem.snapTo(
                        absolutePos.getX() + 0.5D,
                        absolutePos.getY(),
                        absolutePos.getZ() + 0.5D,
                        0.0F,
                        0.0F
                );
                require(helper, helper.getLevel().addFreshEntity(golem),
                        "Could not add copper golem to the GameTest level");
                return golem;
            }
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not construct copper golem fixture", exception);
        }
        throw helper.assertionException("No compatible CopperGolem constructor was found");
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = CopperGolemWrenchHandler.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not invoke CopperGolemWrenchHandler#" + name, exception);
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
