package com.lulan.shincolle.entity.destroyer;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.IShipSummonAttack;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.entity.other.EntityRensouhouS;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class EntityDestroyerShimakaze extends BasicEntityShipSmall implements IShipSummonAttack
{

	public int numRensouhou;
	
	
	public EntityDestroyerShimakaze(World world)
	{
		super(world);
		this.setSize(0.5F, 1.6F);
		this.setStateMinor(ID.M.ShipType, ID.ShipType.DESTROYER);
		this.setStateMinor(ID.M.ShipClass, ID.Ship.DestroyerShimakaze);
		this.setStateMinor(ID.M.DamageType, ID.ShipDmgType.DESTROYER);
		this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[ID.ShipConsume.DD]);
		this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[ID.ShipConsume.DD]);
		this.ModelPos = new float[] {0F, 15F, 0F, 40F};
		
		this.numRensouhou = 6;
		
		//set attack type
		this.StateFlag[ID.F.HaveRingEffect] = true;
		this.StateFlag[ID.F.AtkType_AirLight] = false;
		this.StateFlag[ID.F.AtkType_AirHeavy] = false;
		this.StateFlag[ID.F.CanPickItem] = true;
		
		this.postInit();
	}
	
	//for morph
	@Override
	public float getEyeHeight()
	{
		return 1.5F;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType()
	{
		return 1;
	}
	
	@Override
	public void setAIList()
	{
		super.setAIList();
		
		//use range attack (light)
		this.tasks.addTask(11, new EntityAIShipRangeAttack(this));
		
		//pick item
		this.tasks.addTask(20, new EntityAIShipPickItem(this, 4F));
	}
    
    //check entity state every tick
  	@Override
  	public void onLivingUpdate()
  	{
  		super.onLivingUpdate();
  		
  		if (!world.isRemote)
  		{
  			//add aura to master every 128 ticks
  			if (this.ticksExisted % 128 == 0)
  			{
  				//add num of rensouhou
  				if (this.numRensouhou < 6) numRensouhou++;
  				
  				//apply ring effect
  				EntityPlayerMP player = (EntityPlayerMP) EntityHelper.getEntityPlayerByUID(this.getPlayerUID());
  				if (getStateFlag(ID.F.IsMarried) && getStateFlag(ID.F.UseRingEffect) &&
  					getStateMinor(ID.M.NumGrudge) > 0 && player != null &&
  					getDistanceSqToEntity(player) < 256D)
  				{
  					//potion effect: id, time, level
  	  	  			player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 300, getStateMinor(ID.M.ShipLevel) / 25 + 1));
  				}
  			}
  		}    
  	}
  	
  	//招喚連裝砲進行攻擊
  	@Override
  	public boolean attackEntityWithAmmo(Entity target)
  	{
  		//check num rensouhou
  		if  (this.numRensouhou <= 0)
  		{
  			return false;
  		}
  		else
  		{
  			this.numRensouhou--;
  		}
        
        //experience++
  		addShipExp(ConfigHandler.expGain[1] * 2);
  		
  		//grudge--
  		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.LAtk] * 4);
  		
  		//morale--
  		decrMorale(1);
  		setCombatTick(this.ticksExisted);
  		
  		//light ammo--
        if (!decrAmmoNum(0, 4 * this.getAmmoConsumption())) return false;
        
        //play attack sound
		if (this.rand.nextInt(10) > 7)
		{
			this.playSound(this.getCustomSound(1, this), this.getSoundVolume(), this.getSoundPitch());
        }

        //發射者煙霧特效 (招喚連裝砲不使用特效, 但是要發送封包來設定attackTime)
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);
  		
		//spawn rensouhou
    	if (this.getStateEmotion(ID.S.State2) > ID.State.NORMAL_2)
    	{
    		EntityRensouhouS rensoho2 = new EntityRensouhouS(this.world);
    		rensoho2.initAttrs(this, target, 0);
            this.world.spawnEntity(rensoho2);
    	}
    	else
    	{
    		EntityRensouhou rensoho1 = new EntityRensouhou(this.world);
    		rensoho1.initAttrs(this, target, 0);
            this.world.spawnEntity(rensoho1);
    	}
    	
    	//show emotes
    	applyEmotesReaction(3);
    	
    	if (ConfigHandler.canFlare) this.flareTarget(target);
    	
        return true;
	}
  	
  	//五連裝酸素魚雷
  	@Override
  	public boolean attackEntityWithHeavyAmmo(Entity target)
  	{	
		//get attack value
		float atk = StateFinal[ID.ATK_H] * 0.3F;
		float kbValue = 0.15F;
		
		//飛彈是否採用直射
		boolean isDirect = false;
		//計算目標距離
		float tarX = (float)target.posX;	//for miss chance calc
		float tarY = (float)target.posY;
		float tarZ = (float)target.posZ;
		float distX = tarX - (float)this.posX;
		float distY = tarY - (float)this.posY;
		float distZ = tarZ - (float)this.posZ;
        float distSqrt = MathHelper.sqrt(distX*distX + distY*distY + distZ*distZ);
        float launchPos = (float)posY + height * 0.7F;
        
        //超過一定距離/水中 , 則採用拋物線,  在水中時發射高度較低
        if ((distX*distX+distY*distY+distZ*distZ) < 36F)
        {
        	isDirect = true;
        }
        if (this.getShipDepth() > 0D)
        {
        	isDirect = true;
        	launchPos = (float)posY;
        }
		
		//experience++
		addShipExp(ConfigHandler.expGain[2]);
		
		//grudge--
		decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.HAtk]);
		
  		//morale--
		decrMorale(2);
  		setCombatTick(this.ticksExisted);
	
		//play cannon fire sound at attacker
  		applySoundAtAttacker(2, target);
  		this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.8F);
  		this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.9F);
        
        //heavy ammo--
        if(!decrAmmoNum(1, this.getAmmoConsumption())) return false;
        
        //calc miss chance, miss: add random offset(0~6) to missile target 
        float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.M.ShipLevel];
        missChance -= EffectEquip[ID.EF_MISS];	//equip miss reduce
        if (missChance > 0.35F) missChance = 0.35F;	//max miss chance = 30%
       
        if (this.rand.nextFloat() < missChance)
        {
        	tarX = tarX - 5F + this.rand.nextFloat() * 10F;
        	tarY = tarY + this.rand.nextFloat() * 5F;
        	tarZ = tarZ - 5F + this.rand.nextFloat() * 10F;
        	//spawn miss particle
        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
        }
        
        //發射者煙霧特效 (不使用特效, 但是要發送封包來設定attackTime)
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 0, true), point);

        //spawn missile
        EntityAbyssMissile missile1 = new EntityAbyssMissile(this.world, this, 
        		tarX, tarY+target.height*0.2F, tarZ, launchPos, atk, kbValue, isDirect, -1F);
        EntityAbyssMissile missile2 = new EntityAbyssMissile(this.world, this, 
        		tarX+3F, tarY+target.height*0.2F, tarZ+5F, launchPos, atk, kbValue, isDirect, -1F);
        EntityAbyssMissile missile3 = new EntityAbyssMissile(this.world, this, 
        		tarX+3F, tarY+target.height*0.2F, tarZ-5F, launchPos, atk, kbValue, isDirect, -1F);
        EntityAbyssMissile missile4 = new EntityAbyssMissile(this.world, this, 
        		tarX-3F, tarY+target.height*0.2F, tarZ+5F, launchPos, atk, kbValue, isDirect, -1F);
        EntityAbyssMissile missile5 = new EntityAbyssMissile(this.world, this, 
        		tarX-3F, tarY+target.height*0.2F, tarZ-5F, launchPos, atk, kbValue, isDirect, -1F);
        
        this.world.spawnEntity(missile1);
        this.world.spawnEntity(missile2);
        this.world.spawnEntity(missile3);
        this.world.spawnEntity(missile4);
        this.world.spawnEntity(missile5);
        
        //show emotes
      	applyEmotesReaction(3);
      	
      	if (ConfigHandler.canFlare) flareTarget(target);
      	
        return true;
	}
  	
  	@Override
	public int getKaitaiType()
  	{
		return 2;
	}
  	
  	@Override
	public int getNumServant()
  	{
		return this.numRensouhou;
	}

	@Override
	public void setNumServant(int num)
	{
		this.numRensouhou = num;
	}

	@Override
	public double getMountedYOffset()
	{
		if (this.isSitting())
		{
			if (getStateEmotion(ID.S.Emotion) == ID.Emotion.BORED)
			{
				return 0F;
  			}
  			else
  			{
  				return (double)this.height * 0.1F;
  			}
  		}
  		else
  		{
  			return (double)this.height * 0.45F;
  		}
	}

	@Override
	public void setShipOutfit(boolean isSneaking)
	{
		if (isSneaking)
		{
			switch (getStateEmotion(ID.S.State2))
			{
			case ID.State.NORMAL_2:
				setStateEmotion(ID.S.State2, ID.State.EQUIP00_2, true);
			break;
			case ID.State.EQUIP00_2:
				setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
			break;	
			default:
				setStateEmotion(ID.S.State2, ID.State.NORMAL_2, true);
			break;
			}
		}
		else
		{
			switch (getStateEmotion(ID.S.State))
			{
			case ID.State.NORMAL:
				setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
			break;
			case ID.State.EQUIP00:
				setStateEmotion(ID.S.State, ID.State.NORMAL, true);
			break;
			default:
				setStateEmotion(ID.S.State, ID.State.NORMAL, true);
			break;
			}
		}
	}


}