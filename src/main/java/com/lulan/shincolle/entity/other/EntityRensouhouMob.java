package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.BasicEntitySummon;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.BlockHelper;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TeamHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityRensouhouMob extends BasicEntitySummon
{
	
	private BasicEntityShipHostile host2;
	
	
    public EntityRensouhouMob(World world)
    {
		super(world);
		this.setSize(0.3F, 0.7F);
	}
    
    public void initAttrs(IShipAttackBase host, Entity target, int scaleLevel, float... par2)
    {
    	this.host2 = (BasicEntityShipHostile) host;
        this.host = this.host2;
        this.atkTarget = target;
        this.setScaleLevel(scaleLevel);
        
        //basic attr
        this.atk = this.host2.getAttackDamage();
        this.atkSpeed = this.host2.getAttackSpeed();
        this.atkRange = this.host2.getAttackRange();
        this.defValue = this.host2.getDefValue() * 0.5F;
        this.movSpeed = this.host2.getMoveSpeed() * 0.2F + 0.35F;
           
        //設定發射位置
        this.posX = this.host2.posX + rand.nextDouble() * 6D - 3D;
        this.posY = this.host2.posY + 0.5D;
        this.posZ = this.host2.posZ + rand.nextDouble() * 6D - 3D;
        
        //check the place is safe to summon
    	if (!BlockHelper.checkBlockSafe(this.world, (int)posX, (int)posY, (int)posZ))
    	{
    		this.posX = host2.posX;
            this.posY = host2.posY;
            this.posZ = host2.posZ;
    	}
        
    	this.prevPosX = this.posX;
    	this.prevPosY = this.posY;
    	this.prevPosZ = this.posZ;
        this.setPosition(this.posX, this.posY, this.posZ);
 
	    //設定基本屬性
	    getEntityAttribute(MAX_HP).setBaseValue(this.host2.getMaxHealth() * 0.15D);
		getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.movSpeed);
		getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64); //此為找目標, 路徑的範圍
		getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.35D);
		if (this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
				
		//設定AI
		this.shipNavigator = new ShipPathNavigate(this);
		this.shipMoveHelper = new ShipMoveHelper(this, 60F);
		this.setAIList();
	}

	//setup AI
	protected void setAIList()
	{
		this.clearAITasks();
		this.clearAITargetTasks();
		
		this.tasks.addTask(1, new EntityAIShipRangeAttack(this));
		this.setEntityTarget(atkTarget);
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		//client side
		if (this.world.isRemote)
		{
			//有移動時, 產生水花特效
			//(注意此entity因為設為非高速更新, client端不會更新motionX等數值, 需自行計算)
			double motX = this.posX - this.prevPosX;
			double motZ = this.posZ - this.prevPosZ;
			double parH = this.posY - (int)this.posY;
			
			if (motX != 0 || motZ != 0)
			{
				ParticleHelper.spawnAttackParticleAt(this.posX + motX*1.5D, this.posY, this.posZ + motZ*1.5D, 
						-motX*0.5D, 0D, -motZ*0.5D, (byte)15);
			}
		}
	}

	@Override
	public boolean getStateFlag(int flag)
	{	//hostile mob: for attack and headTile check
		switch (flag)
		{
		default:
			return true;
		case ID.F.HeadTilt:
			return this.headTilt;
		case ID.F.OnSightChase:
			return false;
		}
	}

	@Override
	public void setStateFlag(int id, boolean flag)
	{
		this.headTilt = flag;
	}

    @Override
	//light attack
	public boolean attackEntityWithAmmo(Entity target)
    {
    	//host check
    	if (this.host2 == null)
    	{
    		this.setDead();
    		return false;
    	}
    	
		//light ammo -1
  		if (numAmmoLight > 0)
  		{
  			numAmmoLight--;
  			if (numAmmoLight <= 0) this.setDead();
  		}
        
  		//get attack value
  		float atk = getAttackBaseDamage(1, target);
  		
        //calc dist to target
        float[] distVec = new float[4];  //x, y, z, dist
        distVec[0] = (float) (target.posX - this.posX);
        distVec[1] = (float) (target.posY - this.posY);
        distVec[2] = (float) (target.posZ - this.posZ);
        distVec[3] = MathHelper.sqrt(distVec[0]*distVec[0] + distVec[1]*distVec[1] + distVec[2]*distVec[2]);
        distVec[0] = distVec[0] / distVec[3];
        distVec[1] = distVec[1] / distVec[3];
        distVec[2] = distVec[2] / distVec[3];
        
        //play cannon fire sound at attacker
        applySoundAtAttacker(1, target);
	    applyParticleAtAttacker(1, target, distVec);

        //calc miss -> crit -> double -> tripple
  		if (rand.nextFloat() < 0.25F)
  		{
          	atk = 0F;	//still attack, but no damage
          	applyParticleSpecialEffect(0);
  		}
  		else
  		{
  			//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
  			//calc critical
          	if (rand.nextFloat() < 0.1F)
          	{
          		atk *= 1.5F;
          		applyParticleSpecialEffect(1);
          	}
          	else
          	{
          		//calc double hit
              	if (rand.nextFloat() < 0.15F)
              	{
              		atk *= 2F;
              		applyParticleSpecialEffect(2);
              	}
              	else
              	{
              		//calc triple hit
                  	if (rand.nextFloat() < 0.05F)
                  	{
                  		atk *= 3F;
                  		applyParticleSpecialEffect(3);
                  	}
              	}
          	}
  		}
  		
 		//calc damage to player
  		if (target instanceof EntityPlayer)
  		{
  			atk *= 0.25F;
  			if (atk > 59F) atk = 59F;	//same with TNT
  		}
  		
  		//check friendly fire
		if (!TeamHelper.doFriendlyFire(this, target)) atk = 0F;
  		
  		//確認攻擊是否成功
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);

	    //if attack success
	    if (isTargetHurt)
	    {
	    	applySoundAtTarget(1, target);
	        applyParticleAtTarget(1, target, distVec);
	        
	        if (ConfigHandler.canFlare)
	        {
	        	this.host2.flareTarget(target);
			}
        }

	    return isTargetHurt;
	}

	@Override
	public boolean attackEntityWithHeavyAmmo(Entity target)
	{
        return false;
	}
	
	@Override
	public float getJumpSpeed()
	{
		return 1.5F;
	}
	
	@Override
	public int getLevel()
	{
		return 150;
	}
	
	@Override
	public boolean useAmmoLight()
	{
		return true;
	}

	@Override
	public boolean useAmmoHeavy()
	{
		return false;
	}

	@Override
	public float getEffectEquip(int id)
	{
		switch (id)
		{
		case ID.EF_CRI:
			return 0.15F;
		case ID.EF_ASM:  //destroyer AA,ASM++
		case ID.EF_AA:
			return this.atk * 0.5F;
		case ID.EF_DODGE:
			return 20F;
		default:
			return 0F;
		}
	}
	
	@Override
	public int getPlayerUID()
	{
		return -100;	//-100 for hostile mob
	}

	@Override
	public void setPlayerUID(int uid) {}

	@Override
	public int getDamageType()
	{
		return ID.ShipDmgType.DESTROYER;
	}

	@Override
	public int getTextureID()
	{
		return ID.ShipMisc.Rensouhou;
	}

	@Override
	protected void setSizeWithScaleLevel()
	{
		switch (this.getScaleLevel())
		{
		case 3:
			this.setSize(1.5F, 2.8F);
		break;
		case 2:
			this.setSize(1.1F, 2.1F);
		break;
		case 1:
			this.setSize(0.7F, 1.4F);
		break;
		default:
			this.setSize(0.3F, 0.7F);
		break;
		}
	}

	@Override
	protected void setAttrsWithScaleLevel() {}

	@Override
	protected void returnSummonResource() {}

	@Override
	public float getAttackBaseDamage(int type, Entity target)
	{
		return CalcHelper.calcDamageBySpecialEffect(this.host, target, this.atk, 0);
	}
	
	
}