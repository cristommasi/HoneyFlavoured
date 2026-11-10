
package com.honeyflavoured.animalarmor.mixin;

import com.honeyflavoured.animalarmor.MountModelLayers;

import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.DonkeyRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DonkeyRenderer.class)
public abstract class MuleArmorRenderMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void honeyflavoured$addArmorLayer(EntityRendererProvider.Context context, EquipmentClientInfo.LayerType saddleLayer, ModelLayerLocation saddleModel, DonkeyRenderer.Type adult, DonkeyRenderer.Type baby, CallbackInfo ci) {

        ((LivingEntityRendererAccessor) this).honeyflavoured$addLayer(
            new SimpleEquipmentLayer<>(
                honeyflavoured$getRenderer(),
                context.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.HORSE_BODY,
                state -> state.bodyArmorItem,
                new DonkeyModel(context.bakeLayer(MountModelLayers.MULE_ARMOR)),
                null
            )
        );
    }


    @Unique
    private RenderLayerParent<DonkeyRenderState, DonkeyModel> honeyflavoured$getRenderer() {

        return (DonkeyRenderer<?>) (Object) this;
    }
}
