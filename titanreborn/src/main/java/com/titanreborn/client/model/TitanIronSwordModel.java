package com.titanreborn.client.model;

import com.titanreborn.TitanReborn;
import com.titanreborn.entity.TitanZombie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * 泰坦巨型铁剑的模型。
 *
 * <p>剑是一个**独立的 Bedrock 模型**（{@code geo/entity/titan_iron_sword.geo.json}，12 个立方体），
 * 不烘进泰坦本体——这样换武器不用重做模型、掉落时能自然掉出剑。
 *
 * <p>资源路径按 GeckoLib 约定由 {@code titanreborn:titan_iron_sword} 推导：
 * <ul>
 *   <li>模型 {@code geo/entity/titan_iron_sword.geo.json}</li>
 *   <li>贴图 {@code textures/entity/titan_iron_sword.png}</li>
 * </ul>
 *
 * <p>模型以**握把为原点**（pivot 0,0,0），剑刃朝 +Y 延伸约 172 像素。
 * 渲染层把它摆到泰坦右手上。
 */
public class TitanIronSwordModel extends DefaultedEntityGeoModel<TitanZombie> {

    public static final ResourceLocation ASSET_PATH =
            ResourceLocation.fromNamespaceAndPath(TitanReborn.MODID, "titan_iron_sword");

    public TitanIronSwordModel() {
        super(ASSET_PATH, false);
    }
}
