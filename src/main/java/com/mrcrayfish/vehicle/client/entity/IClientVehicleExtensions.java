package com.mrcrayfish.vehicle.client.entity;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mrcrayfish.vehicle.common.entity.Transform;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.entity.properties.VehicleProperties;

public interface IClientVehicleExtensions
{
    static Matrix4f getTransformMatrix(VehicleEntity entity, float partialTicks)
    {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.multiply(Vector3f.YP.rotationDegrees(-entity.getBodyRotationYaw(partialTicks)));
        matrix.multiply(Vector3f.XP.rotationDegrees(entity.getBodyRotationPitch(partialTicks)));
        matrix.multiply(Vector3f.ZP.rotationDegrees(entity.getBodyRotationRoll(partialTicks)));

        VehicleProperties properties = entity.getProperties();
        Transform bodyPosition = properties.getBodyTransform();
        matrix.multiply((Matrix4f.createScaleMatrix((float) bodyPosition.getScale(), (float) bodyPosition.getScale(), (float) bodyPosition.getScale())));

        float epsilon = 0.0625F;

        float x = (float) (bodyPosition.getX() * epsilon);
        float y = (float) (bodyPosition.getY() * epsilon);
        float z = (float) (bodyPosition.getZ() * epsilon);

        y += 0.5F;
        y += properties.getAxleOffset() * epsilon;
        y += properties.getWheelOffset() * epsilon;

        matrix.multiply(Matrix4f.createTranslateMatrix(x, y, z));
        return matrix;
    }
}