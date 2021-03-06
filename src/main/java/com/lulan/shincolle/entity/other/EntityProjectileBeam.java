package com.lulan.shincolle.entity.other;

import java.util.List;

import com.lulan.shincolle.client.render.IShipCustomTexture;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipEquipAttrs;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TargetHelper;
import com.lulan.shincolle.utility.TeamHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

/** Beam entity
 *  fly to target, create beam particle between host and target
 *  damage everything on the way
 *  setDead after X ticks
 *  
 *  未使用移動向量碰撞法, 因此速度必須設定為
 *	(width一半 + onImpact範圍) * 2, 以盡量碰撞到路徑上所有物體
 *
 *	實際最大射程約為 lifeLength * acc (格)
 */
public class EntityProjectileBeam extends Entity implements IShipOwner, IShipEquipAttrs, IShipCustomTexture
{

	//host data
	private IShipAttackBase host;	//main host type
    private Entity host2;			//second host type: entity living
    private int playerUID;			//owner UID, for owner check
    
    //beam data
	private int type, lifeLength;
	private float atk, kbValue;
	private float acc, accX, accY, accZ;
	

	public EntityProjectileBeam(World world)
	{
		super(world);
		this.ignoreFrustumCheck = true;  //always render
		this.noClip = true;				 //can't block
		this.stepHeight = 0F;
		this.setSize(1F, 1F);
	}
	
	//init attrs
	public void initAttrs(IShipAttackBase host, int type, float ax, float ay, float az, float atk, float kb)
	{
		//host
		this.host = host;
		this.host2 = (Entity) host;
		this.playerUID = host.getPlayerUID();
		
		//type
		this.type = type;
		
		switch (type)
		{
		default:  //normal beam
			this.setPosition(host2.posX + ax, host2.posY + host2.height * 0.5D, host2.posZ + az);
			this.lifeLength = 31;
			this.acc = 4F;
		break;
		}
		
		this.prevPosX = this.posX;
    	this.prevPosY = this.posY;
    	this.prevPosZ = this.posZ;
		
		//beam data
		this.accX = ax * acc;
		this.accY = ay * acc;
		this.accZ = az * acc;
		this.atk = atk;
		this.kbValue = kb;
	}

	@Override
	public float getEffectEquip(int id)
	{
		if (host != null) return host.getEffectEquip(id);
		return 0;
	}

	@Override
	public int getPlayerUID()
	{
		return this.playerUID;
	}

	@Override
	public void setPlayerUID(int uid) {}

	@Override
	public Entity getHostEntity()
	{
		return host2;
	}

	@Override
	protected void entityInit() {}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {}
	
	@Override
	public boolean isEntityInvulnerable(DamageSource attacker)
	{
        return true;
    }
	
	@Override
    public boolean canBeCollidedWith()
	{
        return false;
    }

    @Override
    public boolean canBePushed()
    {
        return false;
    }
    
    public void setProjectileType(int par1)
    {
    	this.type = par1;
    }

    @Override
	public void onUpdate()
    {
    	/**************** BOTH SIDE ******************/
    	//set speed
    	this.motionX = this.accX;
    	this.motionY = this.accY;
    	this.motionZ = this.accZ;
    	
    	//set position
    	this.setPosition(this.posX, this.posY, this.posZ);
		this.posX += this.motionX;
		this.posY += this.motionY;
        this.posZ += this.motionZ;
        super.onUpdate();
        
        /*************** SERVER SIDE *****************/
        if (!this.world.isRemote)
        {
        	//check life
        	if (this.ticksExisted > this.lifeLength || this.host == null)
        	{
        		this.setDead();
        		return;
        	}
        	
        	//sync beam type at start
    		if (this.ticksExisted == 1)
    		{
    			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
    			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, this.type, S2CEntitySync.PID.SyncProjectile), point);
    		}
        	
    		//判定bounding box內是否有可以觸發爆炸的entity
            List<Entity> hitList = this.world.getEntitiesWithinAABB(Entity.class,
            								this.getEntityBoundingBox().expand(1.5D, 1.5D, 1.5D));
            
            //搜尋list, 找出可碰撞目標執行onImpact
            for (Entity ent : hitList)
            { 
            	if (ent.canBeCollidedWith() && EntityHelper.isNotHost(this, ent))
            	{
            		this.onImpact(ent);
            	}
            }
        }
        /*************** CLIENT SIDE *****************/
        else
        {
    		//spawn beam head particle
        	ParticleHelper.spawnAttackParticleAtEntity(this, 0D, 32 - this.ticksExisted, 0D, (byte)4);
        }
    }
    
    @Override
	public boolean attackEntityFrom(DamageSource attacker, float atk)
    {
    	return false;
    }
    
	//撞擊判定時呼叫此方法
    protected void onImpact(Entity target)
    {
    	//play sound
    	this.playSound(ModSounds.SHIP_EXPLODE, ConfigHandler.volumeFire * 1.5F, 0.7F / (this.rand.nextFloat() * 0.4F + 0.8F));
    	
    	//set attack value
    	float beamAtk = this.atk;

	    //計算範圍爆炸傷害: 判定bounding box內是否有可以吃傷害的entity
	    TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
    
    	//check target attackable
  		if (!TargetHelper.checkUnattackTargetList(target))
  		{
  			//calc equip special dmg: AA, ASM
        	beamAtk = CalcHelper.calcDamageBySpecialEffect(this, target, beamAtk, 1);
        	
    		//若owner相同, 則傷害設為0 (但是依然觸發擊飛特效)
    		if (TeamHelper.checkSameOwner(host2, target))
    		{
    			beamAtk = 0F;
        	}
    		else
    		{
    			//calc critical
        		if (this.host != null && (this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_CRI)))
        		{
        			beamAtk *= 3F;
            		//spawn critical particle
                	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host2, 11, false), point);
            	}
        		
          		//calc damage to player
          		if (target instanceof EntityPlayer)
          		{
          			beamAtk *= 0.25F;
          			if (beamAtk > 59F) beamAtk = 59F;	//same with TNT
          		}
          		
          		//check friendly fire
        		if (!TeamHelper.doFriendlyFire(this.host, target)) beamAtk = 0F;
    		}
    		
    		//if attack success
    	    if (target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, host2).setExplosion(), beamAtk))
    	    {
    	        //send packet to client for display partical effect
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point);
    	    }
  		}//end is attackable
    }

	@Override
	public int getTextureID()
	{
		return ID.ShipMisc.Invisible;
	}

    
}
