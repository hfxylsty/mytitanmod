package com.titanreborn.client;

import com.titanreborn.TitanReborn;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * 僵尸泰坦的**橙黄色周身粒子**。
 *
 * <p>对应原版设定：「周身散发橙黄色粒子效果」。
 *
 * <p><b>为什么放在客户端</b>：粒子是纯视觉，服务端生成再同步会浪费带宽。
 * 在客户端 tick 里对视野内的泰坦直接生成即可。
 *
 * <p>当前用原版 {@code FLAME}（橙黄）占位；若以后要更精确的"神性光尘"观感，
 * 可注册自定义 {@code ParticleType} 并替换 {@link #PARTICLE}。
 */
@EventBusSubscriber(modid = TitanReborn.MODID, value = Dist.CLIENT)
public final class TitanParticles {

    /**
     * 粒子类型。原版 FLAME 是橙黄色，直接满足"橙黄"要求，先不引自定义贴图。
     * 换自定义粒子时只改这一行（类型需能在客户端 spawn）。
     */
    private static final net.minecraft.core.particles.SimpleParticleType PARTICLE = ParticleTypes.FLAME;

    /** 每 tick 每只泰坦生成的粒子数。设计值，待平衡（太大影响帧率）。 */
    private static final int PARTICLES_PER_TICK = 2;

    /** 只在玩家这个距离内生成粒子（格）。 */
    private static final double RANGE = 96.0D;

    /** 粒子生成的高度范围（格）：避开脚部与地面。 */
    private static final double MIN_Y = 2.0D;
    private static final double MAX_Y = 32.0D;

    /** 水平散布半径（格）。泰坦宽 16 格，粒子稍微超出体表更自然。 */
    private static final double RADIUS = 8.0D;

    private TitanParticles() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }

        AABB area = mc.player.getBoundingBox().inflate(RANGE);
        List<TitanZombie> titans = mc.level.getEntitiesOfClass(TitanZombie.class, area);
        if (titans.isEmpty()) {
            return;
        }

        RandomSource random = mc.level.random;
        for (TitanZombie titan : titans) {
            spawnAround(mc, titan, random);
        }
    }

    private static void spawnAround(Minecraft mc, TitanZombie titan, RandomSource random) {
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = random.nextDouble() * RADIUS;
            double x = titan.getX() + Math.cos(angle) * dist;
            double z = titan.getZ() + Math.sin(angle) * dist;
            double y = titan.getY() + MIN_Y + random.nextDouble() * (MAX_Y - MIN_Y);

            // 极小的向上速度，让粒子有"升腾"感而不是静止
            mc.level.addParticle(PARTICLE, x, y, z,
                    0.0D, 0.02D + random.nextDouble() * 0.02D, 0.0D);
        }
    }
}
