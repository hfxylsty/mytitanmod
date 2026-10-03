package com.titanreborn.client.model;

import com.titanreborn.TitanReborn;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * 腐骸泰坦的 GeckoLib 模型。
 *
 * <p>{@link DefaultedEntityGeoModel} 会按固定约定推导三个资源路径
 * （依据 GeckoLib 4.9.3 的 {@code DefaultedGeoModel}）：
 * <ul>
 *   <li>模型：{@code assets/<ns>/geo/entity/<path>.geo.json}</li>
 *   <li>贴图：{@code assets/<ns>/textures/entity/<path>.png}</li>
 *   <li>动画：{@code assets/<ns>/animations/entity/<path>.animation.json}</li>
 * </ul>
 *
 * <p>构造参数里的路径用 {@code rotten_colossus}（而不是实体注册名
 * {@code titan_zombie}），因为「腐骸泰坦」是这个泰坦的名字，
 * 实体 id 是历史包袱。{@code withAltModel} 等可覆盖单个资源路径。
 *
 * <p>{@code headBone = "head"} 让 GeckoLib 自动处理头部朝向——
 * 骨骼名必须与 geo 文件里的 {@code head} 一致。
 */
public class RottenColossusModel extends DefaultedEntityGeoModel<TitanZombie> {

    public static final ResourceLocation ASSET_PATH =
            ResourceLocation.fromNamespaceAndPath(TitanReborn.MODID, "rotten_colossus");

    public RottenColossusModel() {
        super(ASSET_PATH, true);
    }
}
