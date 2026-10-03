package com.titanreborn.entity.part;

import com.titanreborn.entity.TitanZombie;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 泰坦的身体部件。
 *
 * <p>M0.3 验证项：NeoForge 的多部件机制能否用于自定义实体。
 *
 * <p>机制要点（依据 NeoForge 21.1.252 源码 {@code IEntityExtension} 注释）：
 * <ul>
 *   <li>部件<b>不存档</b>（{@code readAdditionalSaveData} 空实现）；</li>
 *   <li>部件<b>不独立同步</b>（基类 {@code getAddEntityPacket} 直接抛异常）；</li>
 *   <li><b>宿主负责每 tick 摆放它们</b>，见 {@link TitanZombie#tick()}。</li>
 * </ul>
 *
 * <p>伤害转发：命中部件 → 转给宿主，并把「命中哪个部件」告给宿主的破防逻辑。
 * 这是 docs/03-gameplay-systems.md 里"击碎 N 片甲片"破防条件的基础。
 */
public class TitanPart extends PartEntity<TitanZombie> {

    private final String partName;
    private final EntityDimensions size;

    /** 相对宿主的偏移（宿主本地坐标系，未随朝向旋转）。 */
    private final Vec3 offset;

    /** 该部件被打碎的累计次数（供破防逻辑读取）。 */
    private int hitCount;

    public TitanPart(TitanZombie owner, String partName, float width, float height, Vec3 offset) {
        super(owner);
        this.partName = partName;
        this.offset = offset;
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    public String getPartName() {
        return partName;
    }

    public Vec3 getOffset() {
        return offset;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void resetHitCount() {
        this.hitCount = 0;
    }

    // ---------- 摆放：由宿主每 tick 调用 ----------

    /** 把部件移动到宿主当前位置 + 偏移（按宿主朝向旋转水平偏移）。 */
    public void moveToOwner(float yawDegrees, double yawRadians) {
        TitanZombie owner = getParent();
        double cos = Math.cos(-yawRadians);
        double sin = Math.sin(-yawRadians);
        double dx = offset.x * cos - offset.z * sin;
        double dz = offset.x * sin + offset.z * cos;
        this.setPos(owner.getX() + dx, owner.getY() + offset.y, owner.getZ() + dz);
        this.setYRot(yawDegrees);
    }

    // ---------- 受击转发 ----------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        this.hitCount++;
        // 转发给宿主：伤害规则、破防判定都由宿主统一处理
        return this.getParent().hurtPart(this, source, amount);
    }

    @Override
    public boolean isPickable() {
        return true;   // 必须为 true，否则射线检测不到，打不中
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;  // 不阻挡玩家移动
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    // ---------- 不存档、不独立同步 ----------

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // 部件不独立同步任何数据：状态由宿主同步（原版 EnderDragonPart 同样为空实现）
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 部件不存档：宿主重建它们
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 同上
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Nullable
    @Override
    public net.minecraft.world.item.ItemStack getPickResult() {
        return this.getParent().getPickResult();
    }

    @Override
    public boolean is(net.minecraft.world.entity.Entity other) {
        return this == other || this.getParent() == other;
    }
}
