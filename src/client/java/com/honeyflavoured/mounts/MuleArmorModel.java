
package com.honeyflavoured.mounts;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;

import net.minecraft.client.model.animal.equine.DonkeyModel;

public class MuleArmorModel {

    public static LayerDefinition createLayer() {

        MeshDefinition mesh = AbstractEquineModel.createBodyMesh(new CubeDeformation(0.1F));

        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");

        CubeListBuilder chest = CubeListBuilder.create()
            .texOffs(26, 21)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 3.0F);

        body.addOrReplaceChild("left_chest", chest,
            PartPose.offsetAndRotation(6.0F, -8.0F, 0.0F, 0.0F, (float) (-Math.PI / 2), 0.0F));

        body.addOrReplaceChild("right_chest", chest,
            PartPose.offsetAndRotation(-6.0F, -8.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F));

        PartDefinition head = root.getChild("head_parts").getChild("head");

        CubeListBuilder ear = CubeListBuilder.create()
            .texOffs(0, 12)
            .addBox(-1.0F, -7.0F, 0.0F, 2.0F, 7.0F, 1.0F);

        head.addOrReplaceChild("left_ear", ear,
            PartPose.offsetAndRotation(1.25F, -10.0F, 4.0F, (float) (Math.PI / 12), 0.0F, (float) (Math.PI / 12)));

        head.addOrReplaceChild("right_ear", ear,
            PartPose.offsetAndRotation(-1.25F, -10.0F, 4.0F, (float) (Math.PI / 12), 0.0F, (float) (-Math.PI / 12)));

        return LayerDefinition.create(mesh, 64, 64)
            .apply(MeshTransformer.scaling(DonkeyModel.MULE_SCALE));
    }
}
