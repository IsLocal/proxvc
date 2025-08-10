package fiveavian.proxvc.util;

import fiveavian.proxvc.vc.StreamingAudioSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.util.helper.Time;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Scans enviroment for audio sources and outputs
 * Detailed info about reverberation, occlusion, etc.
 */
public class EnvironmentDescriptor {
    public final Minecraft client;
    public Map<Integer, StreamingAudioSource> sources;
    public EnvironmentDescriptor(Minecraft client, Map<Integer, StreamingAudioSource> sources) {
        this.client = client;
        this.sources = sources;

    }

    private static final List<Vec3> directions = Collections.unmodifiableList(Arrays.asList(
            Vec3.getPermanentVec3(1, 0, 0),
            Vec3.getPermanentVec3(-1, 0, 0),
            Vec3.getPermanentVec3(0, 1, 0),
            Vec3.getPermanentVec3(0, -1, 0),
            Vec3.getPermanentVec3(0, 0, 1),
            Vec3.getPermanentVec3(0, 0, -1),
            Vec3.getPermanentVec3(1, 1, 0),
            Vec3.getPermanentVec3(1, -1, 0),
            Vec3.getPermanentVec3(-1, 1, 0),
            Vec3.getPermanentVec3(-1, -1, 0),
            Vec3.getPermanentVec3(1, 0, 1),
            Vec3.getPermanentVec3(1, 0, -1),
            Vec3.getPermanentVec3(-1, 0, 1),
            Vec3.getPermanentVec3(-1, 0, -1),
            Vec3.getPermanentVec3(0, 1, 1),
            Vec3.getPermanentVec3(0, 1, -1),
            Vec3.getPermanentVec3(0, -1, 1),
            Vec3.getPermanentVec3(0, -1, -1)
    ));

    private float bouncedBackRays = 0f;
    private float escapedRays = 0f;
    private float totalDistance = 0f;


    public void scan() {
        //First get the player's location
        Vec3 pos = client.thePlayer.getPosition(client.timer.partialTicks, true);
        bouncedBackRays = 0f;
        escapedRays = 0f;
        totalDistance = 0f;
        //Now shoot rays out in diagonal and cardinal directions from the player

        for (Vec3 dir : directions) {
            Vec3 normalizedDir = Vec3.getTempVec3(dir.x, dir.y, dir.z).normalize();
            trace(pos, normalizedDir, 50, 0);
        }
        System.out.println("Bounced back rays: " + bouncedBackRays
            + "Avg distance: " + totalDistance / (bouncedBackRays)
        );

    }

    public void trace(Vec3 start, Vec3 direction, int maxDistance, int bounces) {
        Vec3 pos = client.thePlayer.getPosition(client.timer.partialTicks, true);

        if (pos.distanceTo(start) < 0.5) {
            start = pos.add(direction.x * 1f, direction.y * 1f, direction.z * 1f);
        }

        if (bounces > 15) {
            return;
        }

        HitResult hit = client.currentWorld.checkBlockCollisionBetweenPoints(
                start,
                start.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance),
                true);

        if (hit == null || hit.hitType != HitResult.HitType.TILE) {
            escapedRays++;
            return;
        }

        // Calculate reflection
        Vec3 normal = Vec3.getTempVec3(
                hit.side.getOffsetX(),
                hit.side.getOffsetY(),
                hit.side.getOffsetZ()
        );

        float dotProduct = (float) direction.dotProduct(normal);
        Vec3 reflectedDirection = Vec3.getTempVec3(
                direction.x - 2.0f * dotProduct * normal.x,
                direction.y - 2.0f * dotProduct * normal.y,
                direction.z - 2.0f * dotProduct * normal.z
        ).normalize();

        HitResult bounceBackHit = client.thePlayer.bb.clip(start,
                start.add(reflectedDirection.x * maxDistance, reflectedDirection.y * maxDistance, reflectedDirection.z * maxDistance));

        if (bounceBackHit != null) {
            totalDistance += (float) pos.distanceTo(start);
            bouncedBackRays++;
            return;
        }

        //check valid bounce bac
        trace(hit.location, reflectedDirection, maxDistance, bounces + 1);
    }

    public void visualizeRay(Vec3 start, Vec3 direction) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(start.x, start.y, start.z);
        GL11.glVertex3d(start.x + direction.x, start.y + direction.y, start.z + direction.z);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
