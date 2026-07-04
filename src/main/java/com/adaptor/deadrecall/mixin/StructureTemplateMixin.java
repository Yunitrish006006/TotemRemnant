package com.adaptor.deadrecall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {

    @Inject(
            method = "processBlockInfos",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void deadrecall$replaceBookshelfBlockInfos(
            ServerLevelAccessor level,
            BlockPos origin,
            BlockPos pivot,
            StructurePlaceSettings settings,
            List<StructureTemplate.StructureBlockInfo> infos,
            CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> cir
    ) {
        List<StructureTemplate.StructureBlockInfo> original = cir.getReturnValue();
        List<StructureTemplate.StructureBlockInfo> replaced = new ArrayList<>(original.size());

        for (StructureTemplate.StructureBlockInfo info : original) {
            if (info.state().is(Blocks.BOOKSHELF)) {
                replaced.add(new StructureTemplate.StructureBlockInfo(
                        info.pos(),
                        Blocks.CHISELED_BOOKSHELF.defaultBlockState(),
                        null
                ));
            } else {
                replaced.add(info);
            }
        }

        cir.setReturnValue(replaced);
    }

    @Inject(method = "placeInWorld", at = @At("RETURN"))
    private void deadrecall$fillChiseledBookshelves(
            ServerLevelAccessor level,
            BlockPos origin,
            BlockPos pivot,
            StructurePlaceSettings settings,
            RandomSource random,
            int flags,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) {
            return;
        }

        StructureTemplate self = (StructureTemplate) (Object) this;
        BoundingBox box = self.getBoundingBox(settings, origin);

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
                    if (!(be instanceof ChiseledBookShelfBlockEntity shelf)) {
                        continue;
                    }

                    for (int slot = 0; slot < shelf.getItems().size(); slot++) {
                        if (shelf.getItems().get(slot).isEmpty()) {
                            shelf.setItem(slot, new ItemStack(Items.BOOK));
                        }
                    }
                }
            }
        }
    }
}
