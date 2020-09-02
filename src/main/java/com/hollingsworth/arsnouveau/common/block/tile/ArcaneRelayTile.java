package com.hollingsworth.arsnouveau.common.block.tile;

import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.NBTUtil;
import com.hollingsworth.arsnouveau.common.block.BlockRegistry;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketANEffect;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;

public class ArcaneRelayTile extends AbstractManaTile{

    public ArcaneRelayTile() {
        super(BlockRegistry.ARCANE_RELAY_TILE);
    }

    private BlockPos toPos;
    private BlockPos fromPos;


    public boolean setTakeFrom(BlockPos pos){
        if(BlockUtil.distanceFrom(pos, this.pos) > 10){
            return false;
        }
        this.fromPos = pos;
        update();
        return true;
    }

    public boolean setSendTo(BlockPos pos ){
        if(BlockUtil.distanceFrom(pos, this.pos) > 10){
            return false;
        }
        this.toPos = pos;
        update();
        return true;
    }

    public void clearPos(){
        this.toPos = null;
        this.fromPos = null;
          update();
    }

    @Override
    public int getTransferRate() {
        return 100;
    }

    @Override
    public int getMaxMana() {
        return 200;
    }

    @Override
    public void tick() {
        if(world.isRemote){
//            System.out.println(this.fromPos);
        }

        if(world.getGameTime() % 20 != 0 || toPos == null)
            return;

        if(fromPos != null){
            // Block has been removed
            if(!(world.getTileEntity(fromPos) instanceof AbstractManaTile)){
                fromPos = null;
                world.notifyBlockUpdate(this.pos, world.getBlockState(pos),  world.getBlockState(pos), 2);
                return;
            }else if(world.getTileEntity(fromPos) instanceof AbstractManaTile){
                // Transfer mana fromPos to this

                AbstractManaTile fromTile = (AbstractManaTile) world.getTileEntity(fromPos);
                if(fromTile.getCurrentMana() >= this.getTransferRate() && this.getCurrentMana() + this.getTransferRate() <= this.getMaxMana()){
                    if(world.isRemote){
//                        System.out.println(this.getCurrentMana());
//                        ParticleEngine.getInstance().addEffect(new TimedBeam(pos, fromPos, 5, (ClientWorld) world));
                    }else{

                        fromTile.removeMana(this.getTransferRate());
                        this.addMana(this.getTransferRate());
                        System.out.println("packet");
                        Networking.INSTANCE.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), 15, world.dimension.getType())),
                                new PacketANEffect(PacketANEffect.EffectType.TIMED_GLOW, pos.getX(), pos.getY(), pos.getZ(), fromPos.getX(), fromPos.getY(), fromPos.getZ(), 5));

                    }
                }
            }
        }
        if(!(world.getTileEntity(toPos) instanceof AbstractManaTile)){
            toPos = null;
            update();

            return;
        }
        AbstractManaTile toTile = (AbstractManaTile) this.world.getTileEntity(toPos);
        if(this.getCurrentMana() >= this.getTransferRate() && toTile.getCurrentMana() + this.getTransferRate() <= toTile.getMaxMana()){

            if(world.isRemote){

//                ParticleEngine.getInstance().addEffect(new TimedBeam(toPos, pos, 3,(ClientWorld) world));
            }else{

                this.removeMana(this.getTransferRate());
                toTile.addMana(this.getTransferRate());
                Networking.INSTANCE.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), 15, world.dimension.getType())),
                        new PacketANEffect(PacketANEffect.EffectType.TIMED_GLOW,  toPos.getX(), toPos.getY(), toPos.getZ(),pos.getX(), pos.getY(), pos.getZ(), 5));

            }
        }
    }

    @Override
    public void read(CompoundNBT tag) {
        if(NBTUtil.hasBlockPos(tag, "to")){
            this.toPos = NBTUtil.getBlockPos(tag, "to");
        }
        if(NBTUtil.hasBlockPos(tag, "from")){
            this.fromPos = NBTUtil.getBlockPos(tag, "from");
        }
        super.read(tag);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        if(toPos != null)
            NBTUtil.storeBlockPos(tag, "to", toPos);
        if(fromPos != null)
            NBTUtil.storeBlockPos(tag, "from", fromPos);
        return super.write(tag);
    }

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.pos, 3, this.getUpdateTag());
    }
    @Override
    public CompoundNBT getUpdateTag() {
        return this.write(new CompoundNBT());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getNbtCompound());
    }

}