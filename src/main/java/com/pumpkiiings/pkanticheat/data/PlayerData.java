package com.pumpkiiings.pkanticheat.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;

    // Movement history
    private Vec3 lastLocation = Vec3.ZERO;
    private Vec3 currentLocation = Vec3.ZERO;
    private Vec3 lastValidGroundLocation = Vec3.ZERO;
    private boolean wasOnGround = true;
    private double lastYDelta = 0.0;

    // FastBreak & Scaffold
    private BlockPos miningBlock = null;
    private long miningStartTime = 0;
    public int scaffoldVL = 0;

    // Killaura / Aim
    public final Deque<Float> yawDeltas   = new ArrayDeque<>();
    public final Deque<Float> pitchDeltas = new ArrayDeque<>();

    private float lastYaw;
    private float lastPitch;

    // Network Rotations (Netty Phase 1)
    private float networkYaw;
    private float networkPitch;

    // Network Packets (Timer/Blink)
    public long lastMovementPacketTime = System.currentTimeMillis();
    public long timerBalance = 0;

    // Ping Tracker
    public int currentPing = 0;
    public int previousPing = 0;
    public int pingJitter = 0;

    // Violation Levels (Lag Buffers)
    public int flyVL = 0;
    public int spiderVL = 0;
    public int waterWalkVL = 0;
    public int noFallVL = 0;
    public int strafeVL = 0;
    public int slowTimerVL = 0;
    public int airTicks = 0;

    // NoSwingCheck — per-tick flags
    public boolean swungThisTick = false;
    public boolean attackedThisTick = false;
    public int noSwingVL = 0;

    // PacketOrderCheck — per-tick flags
    public boolean swingBeforeAttack = false;
    public int packetOrderVL = 0;

    // GCDCheck — Euclidean sensitivity analysis
    public float gcdSensVL = 0f;
    public int   gcdMicroVL = 0;
    public float prevPitchGCD = 0f;

    // RotationSnapCheck — rolling yaw history (last 2 ticks)
    public double prevTickYawDelta = 0.0;
    public int rotationSnapVL = 0;
    public long lastAttackTimestamp = 0L;

    // RotationStdDevCheck — sample lists for pitch/yaw vs perfect target angle
    public final java.util.ArrayDeque<Float> yawToPerfectSamples   = new java.util.ArrayDeque<>();
    public final java.util.ArrayDeque<Float> pitchToPerfectSamples = new java.util.ArrayDeque<>();
    public double rotStdDevYawBalance   = 0.0;
    public double rotStdDevPitchBalance = 0.0;

    // HitboxAccuracyCheck
    public int hitboxAccuracyAttacks = 0;
    public int hitboxAccuracySwings  = 0;
    public double hitboxAccuracyVL   = 0.0;
    public final java.util.ArrayDeque<Float> distToPerfectYawList  = new java.util.ArrayDeque<>();
    public final java.util.ArrayDeque<Float> yawSpeedList          = new java.util.ArrayDeque<>();
    
    // Tracking true fall distance
    public float realFallDistance = 0.0f;

    // Velocity (Anti-Knockback)
    private Vec3 expectedVelocity = Vec3.ZERO;
    private int velocityTicks = 0;

    public Vec3 getExpectedVelocity() {
        return expectedVelocity;
    }

    public void setExpectedVelocity(Vec3 expectedVelocity) {
        this.expectedVelocity = expectedVelocity;
    }

    public int getVelocityTicks() {
        return velocityTicks;
    }

    public void setVelocityTicks(int velocityTicks) {
        this.velocityTicks = velocityTicks;
    }

    // Phase 6 Trackers
    private int bhopFlags = 0;
    private net.minecraft.world.entity.Entity lastLookedEntity = null;
    private long lastLookedEntityTime = 0;

    // Strafe Check
    public int getStrafeFlags() { return strafeVL; }
    public int addStrafeFlag() { return ++strafeVL; }
    public void resetStrafeFlags() { strafeVL = 0; }

    // Ping Tracker logic
    public void updatePing(int newPing) {
        if (this.currentPing != newPing) {
            this.pingJitter = Math.abs(this.currentPing - newPing);
            this.previousPing = this.currentPing;
            this.currentPing = newPing;
        }
    }

    public int getBhopFlags() {
        return bhopFlags;
    }

    public void setBhopFlags(int bhopFlags) {
        this.bhopFlags = bhopFlags;
    }

    public net.minecraft.world.entity.Entity getLastLookedEntity() {
        return lastLookedEntity;
    }

    public void setLastLookedEntity(net.minecraft.world.entity.Entity lastLookedEntity) {
        this.lastLookedEntity = lastLookedEntity;
    }

    public long getLastLookedEntityTime() {
        return lastLookedEntityTime;
    }

    public void setLastLookedEntityTime(long lastLookedEntityTime) {
        this.lastLookedEntityTime = lastLookedEntityTime;
    }

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Vec3 getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Vec3 lastLocation) {
        this.lastLocation = lastLocation;
    }

    public Vec3 getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Vec3 currentLocation) {
        this.lastLocation = this.currentLocation;
        this.currentLocation = currentLocation;
    }

    public boolean wasOnGround() {
        return wasOnGround;
    }

    public void setWasOnGround(boolean wasOnGround) {
        this.wasOnGround = wasOnGround;
    }

    public BlockPos getMiningBlock() {
        return miningBlock;
    }

    public void setMiningBlock(BlockPos miningBlock) {
        this.miningBlock = miningBlock;
    }

    public long getMiningStartTime() {
        return miningStartTime;
    }

    public void setMiningStartTime(long miningStartTime) {
        this.miningStartTime = miningStartTime;
    }

    public Vec3 getLastValidGroundLocation() {
        return lastValidGroundLocation;
    }

    public void setLastValidGroundLocation(Vec3 lastValidGroundLocation) {
        this.lastValidGroundLocation = lastValidGroundLocation;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }

    public double getLastYDelta() {
        return lastYDelta;
    }

    public void setLastYDelta(double lastYDelta) {
        this.lastYDelta = lastYDelta;
    }

    public float getNetworkYaw() {
        return networkYaw;
    }

    public void setNetworkYaw(float networkYaw) {
        this.networkYaw = networkYaw;
    }

    public float getNetworkPitch() {
        return networkPitch;
    }

    public void setNetworkPitch(float networkPitch) {
        this.networkPitch = networkPitch;
    }
}
