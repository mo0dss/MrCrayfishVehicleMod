package com.mrcrayfish.vehicle.client.render;

import com.mrcrayfish.vehicle.entity.EntityVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public abstract class RenderVehicle<T extends EntityVehicle> extends Render<T>
{
    protected List<Wheel> wheels = new ArrayList<>();

    private PartPosition enginePosition = new PartPosition(0, 0, 0, 0, 1.0F);

    protected RenderVehicle(RenderManager renderManager)
    {
        super(renderManager);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(T entity)
    {
        return null;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        wheels.forEach(wheel -> wheel.render(entity, partialTicks));

        if(entity.shouldRenderEngine())
        {
            GlStateManager.pushMatrix();
            {
                GlStateManager.translate(enginePosition.x * 0.0625, enginePosition.y * 0.0625, enginePosition.z * 0.0625);
                GlStateManager.translate(0, -0.5, 0);
                GlStateManager.scale(enginePosition.scale, enginePosition.scale, enginePosition.scale);
                GlStateManager.translate(0, 0.5, 0);
                GlStateManager.rotate(enginePosition.rotation, 0, 1, 0);
                Minecraft.getMinecraft().getRenderItem().renderItem(entity.engine, ItemCameraTransforms.TransformType.NONE);
            }
            GlStateManager.popMatrix();
        }
    }

    public void setupBreakAnimation(EntityVehicle vehicle, float partialTicks)
    {
        float timeSinceHit = (float) vehicle.getTimeSinceHit() - partialTicks;
        float damageTaken = vehicle.getDamageTaken() - partialTicks;

        if (damageTaken < 0.0F)
        {
            damageTaken = 0.0F;
        }

        if (timeSinceHit > 0.0F)
        {
            GlStateManager.rotate(MathHelper.sin(timeSinceHit) * timeSinceHit * damageTaken / 10.0F, 0, 0, 1);
        }
    }

    public void setEnginePosition(float x, float y, float z, float rotation, float scale)
    {
        this.enginePosition.x = x;
        this.enginePosition.y = y;
        this.enginePosition.z = z;
        this.enginePosition.rotation = rotation;
        this.enginePosition.scale = scale;
    }

    private static class PartPosition
    {
        private float x, y, z;
        private float rotation;
        private float scale;

        public PartPosition(float x, float y, float z, float rotation, float scale)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotation = rotation;
            this.scale = scale;
        }
    }
}