package com.titanreborn.client.model;

import com.titanreborn.entity.TitanZombie;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 僵尸泰坦模型。
 *
 * <p><b>为什么不直接用原版 {@code ZombieModel}</b>：它的类型参数是
 * {@code ZombieModel<T extends Zombie>}，要求实体是 {@code Zombie} 的子类。
 * 我们的泰坦统一继承 {@code Monster}（否则每个泰坦都得继承不同的原版怪，
 * 无法共用 {@code AbstractTitan} 基类），因此这里继承 {@link HumanoidModel}——
 * 它对 {@code T extends LivingEntity} 无额外约束。
 *
 * <p>骨架直接复用原版僵尸模型的层定义 {@code ModelLayers.ZOMBIE}
 * （见 {@link com.titanreborn.client.TitanClientSetup}）。
 * 后续泰坦会换成 GeckoLib 的 Bedrock 模型，见 docs/04-client-rendering.md。
 */
public class TitanZombieModel extends HumanoidModel<TitanZombie> {

    public TitanZombieModel(ModelPart root) {
        super(root);
    }
}
