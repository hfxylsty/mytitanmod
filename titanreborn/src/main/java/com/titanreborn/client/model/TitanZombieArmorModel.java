package com.titanreborn.client.model;

import com.titanreborn.TitanReborn;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * 僵尸泰坦的**护甲层**模型（低血量时包覆全身的亮绿色透明护甲）。
 *
 * <p>与原版设定对应：「低血量时会穿上一层包覆全身的亮绿色透明护甲」。
 *
 * <p>资源路径按 GeckoLib 约定由 {@code titanreborn:titan_zombie_armor} 推导：
 * <ul>
 *   <li>模型 {@code geo/entity/titan_zombie_armor.geo.json}</li>
 *   <li>贴图 {@code textures/entity/titan_zombie_armor.png}（亮绿 + alpha≈140 半透明）</li>
 * </ul>
 * 护甲的骨骼名与本体一致（body / torso_* / *_arm / *_leg / head），
 * 因此动画会自动同步——GeckoLib 用同一套骨骼名驱动。
 *
 * <p>注意：护甲**不包含甲片**（{@code bone_plate_*} 在护甲 geo 里立方体数为 0）。
 * 甲片被打碎属于破防机制，与护甲层无关。
 */
public class TitanZombieArmorModel extends DefaultedEntityGeoModel<TitanZombie> {

    public static final ResourceLocation ASSET_PATH =
            ResourceLocation.fromNamespaceAndPath(TitanReborn.MODID, "titan_zombie_armor");

    public TitanZombieArmorModel() {
        super(ASSET_PATH, true);
    }
}
