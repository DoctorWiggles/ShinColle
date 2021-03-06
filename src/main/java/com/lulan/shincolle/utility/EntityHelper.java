package com.lulan.shincolle.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPath;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.ai.path.ShipPathPoint;
import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipEquipAttrs;
import com.lulan.shincolle.entity.IShipFloating;
import com.lulan.shincolle.entity.IShipGuardian;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.entity.IShipNavigator;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.server.CacheDataPlayer;
import com.lulan.shincolle.server.CacheDataShip;
import com.lulan.shincolle.tileentity.ITileWaypoint;
import com.lulan.shincolle.tileentity.TileEntityCrane;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** helper about entity
 * 
 */
public class EntityHelper
{
	
	private static Random rand = new Random();

	
	public EntityHelper() {}

	/**check entity is in (stand on, y+0D) liquid (not air or solid block) */
	public static boolean checkEntityIsInLiquid(Entity entity)
	{
		IBlockState block = entity.world.getBlockState(new BlockPos(MathHelper.floor(entity.posX), (int)(entity.getEntityBoundingBox().minY), MathHelper.floor(entity.posZ)));
		return BlockHelper.checkBlockIsLiquid(block);
	}
	
	/**check entity is free to move (stand in, y+0.5D) the block */
	public static boolean checkEntityIsFree(Entity entity)
	{
		IBlockState block = entity.world.getBlockState(new BlockPos(MathHelper.floor(entity.posX), (int)(entity.getEntityBoundingBox().minY + 0.5D), MathHelper.floor(entity.posZ)));
		return BlockHelper.checkBlockSafe(block);
	}
	
	/**check entity is air or underwater mob, return 0:default 1:air 2:water */
	public static int checkEntityTypeForEquipEffect(Entity entity)
	{
		if (entity instanceof IShipAttackBase)
		{
			switch (((IShipAttackBase) entity).getDamageType())
			{
			case ID.ShipDmgType.AIRPLANE:
				return 1;
			case ID.ShipDmgType.SUBMARINE:
				return 2;
			default:	//default type
				return 0;
			}
		}
		else if (entity instanceof EntityWaterMob || entity instanceof EntityGuardian)
		{
			return 2;
		}
		else if (entity instanceof EntityBlaze || entity instanceof EntityWither ||
				 entity instanceof EntityDragon || entity instanceof EntityBat ||
				 entity instanceof EntityFlying)
		{
			return 1;
		}
		
		return 0;
	}
	
	/**replace isInWater, check water block with NO extend AABB */
	public static void checkDepth(IShipFloating host)
	{
		Entity host2 = (Entity) host;
		World w = host2.world;
		int px = MathHelper.floor(host2.posX);
		int py = MathHelper.floor(host2.getEntityBoundingBox().minY);
		int pz = MathHelper.floor(host2.posZ);
		BlockPos pos = new BlockPos(px, py, pz);
		IBlockState state = w.getBlockState(pos);
		double depth = 0;
		
		if (BlockHelper.checkBlockIsLiquid(state))
		{
			depth = 1;

			for (int i = 1; py + i < 255D; i++)
			{
				pos = new BlockPos(px, py + i, pz);
				state = w.getBlockState(pos);
				
				//若為液體類方塊, 則深度+1
				if (BlockHelper.checkBlockIsLiquid(state))
				{
					depth++;
				}
				//若碰到非液體方塊, 判定可否上浮
				else
				{
					//最上面碰到空氣類方塊才可以上浮, 否則不上浮
					if (state.getMaterial() == Material.AIR)
					{
						host.setStateFlag(ID.F.CanFloatUp, true);
					}
					else
					{
						host.setStateFlag(ID.F.CanFloatUp, false);
					}
					break;
				}
			}
			
			depth = depth - (host2.posY - (int)host2.posY);
		}
		else
		{
			depth = 0;	
			host.setStateFlag(ID.F.CanFloatUp, false);
		}
		
		host.setShipDepth(depth);
	}
	
	/** check ship is out of combat */
	public static boolean checkShipOutOfCombat(BasicEntityShip ship)
	{
		if (ship != null && ship.ticksExisted - ship.getCombatTick() > 128)
		{
			return true;
		}
		
		return false;
	}
	
	/** check ship is in colled list */
	public static boolean checkShipColled(int classID, CapaTeitoku capa)
	{
		if (capa != null && capa.getColleShipList() != null && capa.getColleShipList().contains(classID))
		{
			return true;
		}
		
		return false;
	}
	
	/** check ship is in colled list */
	public static boolean checkEquipColled(int equipID, CapaTeitoku capa)
	{
		if (capa != null && capa.getColleEquipList() != null && capa.getColleEquipList().contains(equipID))
		{
			return true;
		}
		
		return false;
	}
	
	/** check player is OP */
	public static boolean checkOP(EntityPlayer player)
	{
		if (player != null && !player.world.isRemote)
		{
			return player.world.getMinecraftServer().getPlayerList().canSendCommands(player.getGameProfile());
		}
		
		return false;
	}
	
	/** get player name */
	public static String getOwnerName(BasicEntityShip ship)
	{
		if (ship == null) return "";
		
		String name = "";
		
		//1. get name from ship
		if (ship.ownerName != null && ship.ownerName.length() > 0)
		{
			name = ship.ownerName;
		}
		else
		{
			//2. get name from playerUID -> server cache
			EntityPlayer player = EntityHelper.getEntityPlayerByUID(ship.getPlayerUID());
			
			if (player != null)
			{
				name = player.getName();
			}
			else
			{
				//3. get name from getOwner()
				Entity ent = ship.getOwner();
				
				if (ent instanceof EntityPlayer)
				{
					name = ((EntityPlayer) ent).getName();
				}
			}
		}

		return name;
	}
	
	/** get (loaded) entity by entity ID */
	public static Entity getEntityByID(int entityID, int worldID, boolean isClient)
	{
		World world;
		
		if (isClient)
		{
			world = ClientProxy.getClientWorld();
		}
		else
		{
			world = ServerProxy.getServerWorld(worldID);
		}
		
		if (world != null && entityID > 0)
		{
			return world.getEntityByID(entityID);
		}
			
		return null;
	}
	
	/** get ship entity by ship UID, server side only */
	public static BasicEntityShip getShipByUID(int sid)
	{
		if (sid > 0)
		{
			CacheDataShip data = ServerProxy.getShipWorldData(sid);
			
			if (data != null)
			{
				Entity getEnt = getEntityByID(data.entityID, data.worldID, false);
				
				if (getEnt instanceof BasicEntityShip)
				{
					return (BasicEntityShip) getEnt;
				}
			}
		}
		else
		{
			return null;
		}
		
		return null;
	}
	
	/** get (online) player by entity ID */
	public static EntityPlayer getEntityPlayerByID(int entityID, int worldID, boolean isClient)
	{
		World world;
		
		if (isClient)
		{
			world = ClientProxy.getClientWorld();
		}
		else
		{
			world = ServerProxy.getServerWorld(worldID);
		}
		
		if (world != null && entityID > 0)
		{
			for (EntityPlayer p : world.playerEntities)
			{
				if (p != null && p.getEntityId() == entityID)
				{
					return p;
				}
			}
		}
		
		return null;
	}
	
	/** get (online) player by player name, SERVER SIDE ONLY */
	public static EntityPlayer getEntityPlayerByName(String name)
	{
		if (name != null)
		{
			//get all worlds
			World[] worlds = ServerProxy.getServerWorld();
			
			try
			{
				//check world list
				for (World w : worlds)
				{
					//check entity list
					for (EntityPlayer p : w.playerEntities)
					{
						//check player name
						if (p != null && p.getDisplayNameString().equals(name))
						{
							return p;
						}
					}
				}
			}
			catch (Exception e)
			{
				LogHelper.info("DEBUG : get EntityPlayer by name fail: "+e);
			}
		}
		
		return null;
	}
	
	/** get cached player entity by player UID, no world id check, SERVER SIDE ONLY */
	public static EntityPlayer getEntityPlayerByUID(int uid)
	{
		if (uid > 0)
		{
			//get all worlds
			World[] worlds = ServerProxy.getServerWorld();
			
			//get player data
			int peid = getPlayerEID(uid);
			
			//get player entity
			try
			{
				//check all world
				for (World w : worlds)
				{
					//check player entity list
					for (EntityPlayer p : w.playerEntities)
					{
						if (p != null && p.getEntityId() == peid)
						{
							return p;
						}
					}
				}
			}
			catch (Exception e)
			{
				LogHelper.info("DEBUG : get EntityPlayer by name fail: "+e);
			}
		}
		
		return null;
	}
	
	/** get cached player entity id by player UID, SERVER SIDE ONLY */
	public static int getPlayerEID(int uid)
	{
		if (uid > 0)
		{
			//從server proxy抓出player uid cache
			CacheDataPlayer pdata = ServerProxy.getPlayerWorldData(uid);
			
			//get data
			if (pdata != null)
			{
				return pdata.entityID;
			}
		}
		
		return -1;
	}
	
	/** get player UID by entity */
	public static int getPlayerUID(Entity ent)
	{
		//player entity
		if(ent instanceof EntityPlayer)
		{
			CapaTeitoku capa = CapaTeitoku.getTeitokuCapability((EntityPlayer) ent);
			if (capa != null) return capa.getPlayerUID();
		}
		
		//shincolle entity
		if (ent instanceof IShipOwner)
		{
			return ((IShipOwner) ent).getPlayerUID();
		}
		
		//tameable entity
		if (ent instanceof IEntityOwnable)
		{
			Entity owner = ((IEntityOwnable) ent).getOwner();
			
			//get player UID
			if (owner instanceof EntityPlayer)
			{
				CapaTeitoku capa = CapaTeitoku.getTeitokuCapability((EntityPlayer) owner);
				if (capa != null) return capa.getPlayerUID();
			}
		}
		
		return -1;
	}
	
	/** get player is opening a GUI, SERVER SIDE ONLY */
	public static ArrayList<EntityPlayer> getEntityPlayerUsingGUI()
	{
		ArrayList<EntityPlayer> plist = new ArrayList();
	
		for (World w : ServerProxy.getServerWorld())
		{
			if (w != null)
			{
				for (EntityPlayer p : w.playerEntities)
				{
					CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(p);
					
					if (capa != null && capa.isOpeningGUI())
					{
						plist.add(p);
					}
				}
			}
		}
		
		return plist;
	}
	
	/** get player uuid */
	public static String getPetPlayerUUID(EntityTameable pet)
	{
		if (pet != null)
		{
			return pet.getOwnerId().toString();
		}
		
		return null;
	}
	
	/** set player UID for pet, SERVER SIDE ONLY */
	public static void setPetPlayerUID(EntityPlayer player, IShipOwner pet)
	{
		setPetPlayerUID(getPlayerUID(player), pet);
	}
	
	/** set player UID for pet */
	public static void setPetPlayerUID(int pid, IShipOwner pet)
	{
		if (pet != null && pid > 0)
		{
			pet.setPlayerUID(pid);
		}
	}
	
	/** set owner uuid for pet by player UID and pet entity, SERVER SIDE ONLY */
	public static void setPetPlayerUUID(int pid, EntityTameable pet)
	{
		if (pet != null)
		{
			EntityPlayer owner = getEntityPlayerByUID(pid);
			setPetPlayerUUID(owner, pet);
		}
	}
	
	/** set owner uuid for pet by player entity and pet entity */
	public static void setPetPlayerUUID(EntityPlayer player, EntityTameable pet)
	{
		if (player != null)
		{
			setPetPlayerUUID(player.getUniqueID(), pet);
		}
	}
	
	/** set owner uuid for pet by player uuid and pet entity */
	public static void setPetPlayerUUID(UUID uuid, EntityTameable pet)
	{
		if (pet != null)
		{
			pet.setOwnerId(uuid);
		}
	}
	
	/** add player ship list data */
	public static void addPlayerColledShip(int classID, EntityPlayer player)
	{
		CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
		
		if (capa != null)
		{
			capa.setColleShip(classID);
		}
	}
	
	/** add player ship list data */
	public static void addPlayerColledEquip(int equipID, EntityPlayer player)
	{
		CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
		
		if (capa != null)
		{
			capa.setColleEquip(equipID);
		}
	}
	
	/** update ship path navigator */
	public static void updateShipNavigator(IShipAttackBase entity)
	{
		//null check
		if (entity == null) return;
		
		EntityLiving entity2 = (EntityLiving) entity;
		ShipPathNavigate pathNavi = entity.getShipNavigate();
		ShipMoveHelper moveHelper = entity.getShipMoveHelper();
		
		//若有ship path, 則更新ship navigator
        if (pathNavi != null && moveHelper != null && !pathNavi.noPath())
        {
        	//若同時有官方ai的路徑, 則清除官方ai路徑
        	if (!entity2.getNavigator().noPath())
        	{
        		entity2.getNavigator().clearPathEntity();
        	}

        	//若坐下或綁住, 則清除路徑
        	if (entity.getIsSitting() || entity.getIsLeashed())
        	{
        		entity.getShipNavigate().clearPathEntity();
        	}
        	else
        	{
        		//用particle顯示path point
    			if (ConfigHandler.debugMode && entity2.ticksExisted % 20 == 0)
    			{
    				sendPathParticlePacket(entity.getShipNavigate().getPath(), new TargetPoint(entity2.dimension, entity2.posX, entity2.posY, entity2.posZ, 48D));
    			}
        	}

        	//update movement
        	pathNavi.onUpdateNavigation();
	        moveHelper.onUpdateMoveHelper();
	        
	        //apply movement
	        entity2.moveEntityWithHeading(entity2.moveStrafing, entity2.moveForward);
		}//end ship path

        //若有vanilla path, 則用特效顯示出path
        if (!entity2.getNavigator().noPath())
        {
			//用particle顯示path point
        	if (ConfigHandler.debugMode && entity2.ticksExisted % 16 == 0)
        	{
        		sendPathParticlePacket(entity2.getNavigator().getPath(), new TargetPoint(entity2.dimension, entity2.posX, entity2.posY, entity2.posZ, 64D));
			}
        }//end vanilla path
	}
	
	//send  path indicator particle for vanilla path or ship path
	private static <T> void sendPathParticlePacket(T path, TargetPoint target)
	{
		int parType;
		int len = 0;
		int[] points = null;
		
		if (path instanceof ShipPath)
		{
			ShipPath p = (ShipPath) path;
			ShipPathPoint temp;
			
			parType = 0;
			len = p.getCurrentPathLength();
			points = new int[len * 3 + 1];
			
			//add current path index (target point)
			points[0] = p.getCurrentPathIndex();
			
			//add path points
			for (int i = 0; i < len; i++)
			{
				temp = p.getPathPointFromIndex(i);
				points[i * 3 + 1] = temp.xCoord;
				points[i * 3 + 2] = temp.yCoord;
				points[i * 3 + 3] = temp.zCoord;
			}
		}
		else if (path instanceof Path)
		{
			Path p = (Path) path;
			PathPoint temp;
			
			parType = 1;
			len = p.getCurrentPathLength();
			points = new int[len * 3 + 1];
			
			//add current path index (target point)
			points[0] = p.getCurrentPathIndex();
			
			//add path points
			for (int i = 0; i < len; i++)
			{
				temp = p.getPathPointFromIndex(i);
				points[i * 3 + 1] = temp.xCoord;
				points[i * 3 + 2] = temp.yCoord;
				points[i * 3 + 3] = temp.zCoord;
			}
		}
		else
		{
			return;
		}
		
		if (points != null)
		{
			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(parType, points), target);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static RayTraceResult getPlayerMouseOverEntity(double dist, float duringTicks)
	{
		return getMouseOverEntity(ClientProxy.getMineraft().getRenderViewEntity(), dist, duringTicks);
	}

	/**
	 * ray trace for entity: 
	 * 1. get the farest block
	 * 2. create collision box (rectangle, diagonal vertex = player and farest block)
	 * 3. get entity in this box
	 * 4. check entity on player~block sight line
	 * 5. return nearest entity
	 * 
	 * 修改自: EntityRenderer.getMouseOver
	 */
	@SideOnly(Side.CLIENT)
	public static RayTraceResult getMouseOverEntity(Entity viewer, double dist, float parTick)
	{
		RayTraceResult lookBlock = null;
		
        if (viewer != null && viewer.world != null)
        {
            lookBlock = viewer.rayTrace(dist, parTick);
            Vec3d vec3 = viewer.getPositionEyes(parTick);

            //若有抓到方塊, 則d1改為抓到方塊的距離
            double d1 = dist;
            if (lookBlock != null)
            {
                d1 = lookBlock.hitVec.distanceTo(vec3);
            }

            //計算entity視線的方向向量 * 距離
            Vec3d vec31 = viewer.getLook(parTick);
            double vec3x = vec31.xCoord * dist;
            double vec3y = vec31.yCoord * dist;
            double vec3z = vec31.zCoord * dist;
            Vec3d vec32 = vec3.addVector(vec3x, vec3y, vec3z);
            Vec3d vec33 = null;
            RayTraceResult lookEntity = null;
            Entity pointedEntity = null;
            
            //從玩家到目標方塊之間, 做出擴展1格的方形collision box, 抓出其中碰到的entity
            List<Entity> list = viewer.world.getEntitiesWithinAABBExcludingEntity(viewer, viewer.getEntityBoundingBox().addCoord(vec3x, vec3y, vec3z).expand(1D, 1D, 1D));
            double d2 = d1;

            //檢查抓到的entity, 是否在玩家~目標方塊的視線上
            for (Entity entity : list)
            {
                if (entity.canBeCollidedWith())
                {
                	//檢查entity大小是否在視線上碰撞到
                    double f2 = entity.getCollisionBorderSize();
                    AxisAlignedBB targetBox = entity.getEntityBoundingBox().expand(f2, f2, f2);
                    RayTraceResult getObj = targetBox.calculateIntercept(vec3, vec32);

                    //若viewer完全塞在目標的box裡面
                    if (targetBox.isVecInside(vec3))
                    {
                        if (d2 >= 0D)
                        {
                        	pointedEntity = entity;
                        	//抓到位置玩家位置或者目標box位置
                            vec33 = (getObj == null ? vec3 : getObj.hitVec);
                            //抓到距離改為0
                            d2 = 0D;
                        }
                    }
                    //其他有抓到目標的情況
                    else if (getObj != null)
                    {
                        double d3 = vec3.distanceTo(getObj.hitVec);	//抓到距離

                        //若抓到距離在dist之內, 則判定為抓到目標
                        if (d3 < d2 || d2 == 0D)
                        {
                        	//若抓到的是玩家自己的座騎, 且屬於不能互動的座騎
                            if (entity.getLowestRidingEntity() == viewer.getLowestRidingEntity() && !entity.canRiderInteract())
                            {
                                //若dist設為0D, 才會抓到自己的座騎, 否則都無視座騎
                            	if (d2 == 0D)
                            	{
                                    pointedEntity = entity;
                                    vec33 = getObj.hitVec;
                                }
                            }
                            //其他非座騎entity
                            else
                            {
                                pointedEntity = entity;
                                vec33 = getObj.hitVec;
                                d2 = d3;
                            }
                        }
                    }
                }
            }

            //若有抓到entity, 且抓到距離在視線碰到的最遠方塊之內, 才算有抓到entity
            if (pointedEntity != null && (d2 < d1 || lookBlock == null))
            {
            	lookBlock = new RayTraceResult(pointedEntity, vec33);
            }
        }
        
        return lookBlock;
    }
	
	/** get mouseover target
	 * 
	 *  client/server both side
	 *  calc the look vector by eye height and pitch, less accuracy
	 *  
	 *  par1: check liquid block
	 *  par2: ignore collide check
	 *  par3: always return the last target hit
	 *  
	 *  修改自Item.rayTrace
	 */
	public static RayTraceResult getMouseoverTarget(World world, EntityPlayer player, double dist, boolean onLiquid, boolean ignoreNoAABB, boolean alwaysLastHit)
	{
		float f = 1.0F;
        float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double)f;
        double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double)f + (double)(world.isRemote ? player.getEyeHeight() - player.getDefaultEyeHeight() : player.getEyeHeight()); // isRemote check to revert changes to ray trace position due to adding the eye height clientside and player yOffset differences
        double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)f;
        float f3 = MathHelper.cos(-f2 * 0.017453292F - (float)Math.PI);
        float f4 = MathHelper.sin(-f2 * 0.017453292F - (float)Math.PI);
        float f5 = -MathHelper.cos(-f1 * 0.017453292F);
        float f6 = MathHelper.sin(-f1 * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        
        Vec3d vec3 = new Vec3d(d0, d1, d2);
        Vec3d vec31 = vec3.addVector((double)f7 * dist, (double)f6 * dist, (double)f8 * dist);
        
        return world.rayTraceBlocks(vec3, vec31, onLiquid, ignoreNoAABB, alwaysLastHit);
	}
	
	/** set emotes to all nearby hostile ships */
	public static void applyShipEmotesAOEHostile(World world, double x, double y, double z, double range, int emotesType)
	{
		//server side only
		if (!world.isRemote)
		{
			//get ship entity
            AxisAlignedBB box = new AxisAlignedBB(x-range, y-range, z-range, x+range, y+range, z+range);
            List<BasicEntityShipHostile> slist = world.getEntitiesWithinAABB(BasicEntityShipHostile.class, box);
            
            if (slist != null)
            {
                for (BasicEntityShipHostile s : slist)
                {
                	if (s.isEntityAlive()) s.applyEmotesReaction(emotesType);
                }
            }
		}
	}
	
	/** set emotes to all nearby ships
	 * 
	 *  emotes type:
	 *  0: caress head (owner)
  	 *  1: caress head (other)
  	 *  2: damaged
  	 *  3: attack
  	 *  4: idle
  	 *  5: command
  	 *  6: shock
	 */
	public static void applyShipEmotesAOE(World world, double x, double y, double z, double range, int emotesType)
	{
		//server side only
		if (!world.isRemote)
		{
			//get ship entity
            AxisAlignedBB box = new AxisAlignedBB(x-range, y-range, z-range, x+range, y+range, z+range);
            List<BasicEntityShip> slist = world.getEntitiesWithinAABB(BasicEntityShip.class, box);
            
            if (slist != null)
            {
                for (BasicEntityShip s : slist)
                {
                	if (s.isEntityAlive()) s.applyEmotesReaction(emotesType);
                }
            }
		}
	}
	
	/** set emotes to all nearby ships with owner checking
	 * 
	 *  emotes type:
	 *  0: caress head (owner)
  	 *  1: caress head (other)
  	 *  2: damaged
  	 *  3: attack
  	 *  4: idle
  	 *  5: command
  	 *  6: shock
	 */
	public static void applyShipEmotesAOECheckOwner(World world, double x, double y, double z, double range, int emotesType, int ownerUID) {
		//server side only
		if (!world.isRemote)
		{
			//get ship entity
            AxisAlignedBB box = new AxisAlignedBB(x-range, y-range, z-range, x+range, y+range, z+range);
            List<BasicEntityShip> slist = world.getEntitiesWithinAABB(BasicEntityShip.class, box);
            
            if (slist != null)
            {
                for (BasicEntityShip s : slist)
                {
                	if (s.isEntityAlive() && s.getPlayerUID() == ownerUID)
                	{
                		s.applyEmotesReaction(emotesType);
                	}
                }
            }
		}
	}
	
	/** apply emotes to all entity in list */
	public static void applyEmotesAOE(List entlist, int emotes)
	{
		if (entlist != null && !entlist.isEmpty())
		{
			Entity s = (Entity) entlist.get(0);
			TargetPoint point = new TargetPoint(s.dimension, s.posX, s.posY, s.posZ, 48D);
			
			for (Object o : entlist)
			{
				if (s instanceof Entity)
				{
					CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle((Entity) o, 36, ((Entity) o).height * 0.6F, 0, emotes), point);
				}
			}
		}
	}
	
	/** apply waypoint moving
  	 * 
  	 *  1. if guard position = waypoint block, get next waypoint
  	 *  2. if next waypoint = ship's last waypoint, get block's last waypoint (backwards mode)
  	 *  3. if no next/last waypoint, stop
  	 */
	public static boolean updateWaypointMove(IShipGuardian entity)
	{
		boolean updatePos = false;

  		//in guard block mode
  		if (!entity.getStateFlag(ID.F.CanFollow) && entity.getGuardedPos(1) > 0 && !entity.getIsSitting() && !entity.getIsLeashed() && !entity.getIsRiding())
  		{
  			//check distance < 3 blocks
  			float dx = (float) (entity.getGuardedPos(0) + 0.5D - ((Entity)entity).posX);
  			float dy = (float) (entity.getGuardedPos(1) - ((Entity)entity).posY);
			float dz = (float) (entity.getGuardedPos(2) + 0.5D - ((Entity)entity).posZ);
			dx = dx * dx;
			dy = dy * dy;
			dz = dz * dz;
  			double distsq = dx + dy + dz;
  			
  			//get target block
  			BlockPos pos = new BlockPos(entity.getGuardedPos(0), entity.getGuardedPos(1), entity.getGuardedPos(2));
	  		TileEntity tile = ((Entity) entity).world.getTileEntity(pos);
	  		
  			//is waypoint block
  			if (tile instanceof TileEntityCrane)
  			{
  				//ship wait for craning (xz < 2 blocks, y < 5 blocks)
  				if (distsq < 25D)
				{
  					if (entity.getStateMinor(ID.M.CraneState) == 0)
  					{
  						entity.setStateMinor(ID.M.CraneState, 1);
  						
  						//若座騎為BasicEntityMount, 騎乘者為BasicEntityShip, 則解除騎乘並開始裝卸
  						if (!((Entity)entity).getPassengers().isEmpty())
  						{
  							Entity rider = ((Entity)entity).getPassengers().get(0);
  							
  							if (entity instanceof BasicEntityMount && rider instanceof BasicEntityShip)
  							{
  								((BasicEntityShip) rider).setStateMinor(ID.M.CraneState, 1);
  								rider.dismountRidingEntity();
  							}
  						}
  					}
				}
  				else
  				{
  					//go to the crane below
  	  				entity.getShipNavigate().tryMoveToXYZ(entity.getGuardedPos(0) + 0.5D, entity.getGuardedPos(1) - 2D, entity.getGuardedPos(2) + 0.5D, 1D);
  				}
  			}
  			else
  			{
  				//cancel craning
  				entity.setStateMinor(ID.M.CraneState, 0);
  				
  				//cancel rider craning
				if (!((Entity)entity).getPassengers().isEmpty())
				{
					Entity rider = ((Entity)entity).getPassengers().get(0);
					
					if (rider instanceof BasicEntityShip)
					{
						((BasicEntityShip) rider).setStateMinor(ID.M.CraneState, 0);
					}
				}
  			}
  			
  			//is waypoint block
  			if (tile instanceof TileEntityWaypoint)
  			{
  	  			if (distsq < 9D)
  	  			{
	  	  			try
  	  				{
  	  					updatePos = applyNextWaypoint((TileEntityWaypoint) tile, entity, true, 16);
	  	  				
	  	  				//set follow dist
	  	  				if (updatePos)
	  	  				{
	  	  					entity.setStateMinor(ID.M.FollowMin, 2);
	  	  					entity.getShipNavigate().tryMoveToXYZ(entity.getGuardedPos(0) + 0.5D, entity.getGuardedPos(1), entity.getGuardedPos(2) + 0.5D, 1D);
	  	  				}
	  	  				
	  	  				return updatePos;
  	  				}
  	  				catch (Exception e)
  	  				{
  	  					e.printStackTrace();
  	  				}
  	  			}
  	  			else
  	  			{
  	  				if (entity.getTickExisted() % 128 == 0)
  	  				{
  	  					entity.getShipNavigate().tryMoveToXYZ(entity.getGuardedPos(0) + 0.5D, entity.getGuardedPos(1), entity.getGuardedPos(2) + 0.5D, 1D);
  	  				}
  	  			}
  				
  			}
  		}//end in guard mode
  		
  		return updatePos;
  	}
	
	/** set next waypoint by checking last and next waypoint, return true if changed */
	public static boolean applyNextWaypoint(ITileWaypoint tile, IShipGuardian entity, boolean checkWpStay, int checkDelay)
	{
		boolean changed = false;
		
		boolean timeout = !checkWpStay;
		BlockPos next = tile.getNextWaypoint();
		BlockPos last = tile.getLastWaypoint();
		BlockPos shiplast = entity.getLastWaypoint();
		
		//check waypoint stay time
		int wpstay = entity.getWpStayTime();
		
		//stay until timeout
		if (checkWpStay)
		{
			//check wp tile stay time and ship wp stay time, get the longer one
			int staytimemax = Math.max(entity.getWpStayTimeMax(), tile.getWpStayTime());
			
			if(wpstay < staytimemax)
			{
				entity.setWpStayTime(wpstay + checkDelay);
			}
			else
			{
				timeout = true;
			}
		}
		
		//timeout, go to next wp
		if (timeout)
		{
			entity.setWpStayTime(0);
			
			//if next == last, inverse moving
			if (next.getY() > 0 && next.getX() == shiplast.getX() && next.getY() == shiplast.getY() && next.getZ() == shiplast.getZ())
			{
				//if no last waypoint, go to next waypoint
				if (last.getY() <= 0)
				{
					//go to next waypoint
					if (next.getY() > 0)
					{
						setGuardedPos((Entity) entity, next.getX(), next.getY(), next.getZ());
						changed = true;
					}
				}
				//get last waypoint, go to last waypoint
				else
				{
					//go to last waypoint (backwards mode)
					setGuardedPos((Entity) entity, last.getX(), last.getY(), last.getZ());
					changed = true;
				}
			}
			//go to next waypoint
			else if (next.getY() > 0)
			{
				setGuardedPos((Entity) entity, next.getX(), next.getY(), next.getZ());
				changed = true;
			}
			
			//set last waypoint
			entity.setLastWaypoint(((TileEntity)tile).getPos());
			
			if (!((Entity)entity).getPassengers().isEmpty())
			{
				Entity rider = ((Entity)entity).getPassengers().get(0);
				
				if (rider instanceof BasicEntityShip)
				{
					((BasicEntityShip) rider).setLastWaypoint(((TileEntity)tile).getPos());
				}
			}
		}
		
		return changed;
	}
	
	private static void setGuardedPos(Entity host, int x, int y, int z)
	{
		((IShipGuardian) host).setGuardedPos(x, y, z, host.dimension, 1);
		
		if (!host.getPassengers().isEmpty())
		{
			Entity rider = host.getPassengers().get(0);
			
			if (rider instanceof BasicEntityShip)
			{
				((BasicEntityShip) rider).setGuardedPos(x, y, z, host.dimension, 1);
			}
		}
	}
	
	public static boolean canDodge(IShipEquipAttrs ent, float dist)
	{
		if (ent != null && !((Entity)ent).world.isRemote)
		{
			int dodge = (int) ent.getEffectEquip(ID.EF_DODGE);
			Entity ent2 = (Entity) ent;
			
			if (ent instanceof IShipInvisible && dist > 36F)
			{	//dist > 6 blocks
				dodge += (int) ((IShipInvisible)ent).getInvisibleLevel();
				//check limit
				if(dodge > (int) ConfigHandler.limitShipEffect[6]) dodge = (int) ConfigHandler.limitShipEffect[6];
			}
			
			//roll dodge
			if (rand.nextInt(101) <= dodge)
			{
				//spawn miss particle
				TargetPoint point = new TargetPoint(ent2.dimension, ent2.posX, ent2.posY, ent2.posZ, 32D);
				CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(ent2, 34, false), point);
				return true;
			}
		}
		
		return false;
	}
	
	/** count entity number in the world
	 *  type: 0:boss ship, 1:mob ship, 2:all hostile ship
	 */
	public static int getEntityNumber(int type, World world)
	{
		int count = 0;
		
		if (world != null)
		{
			for (Entity ent: world.loadedEntityList)
			{
				switch(type)
				{
				case 1:   //boss ship 
					if (!ent.isNonBoss())
					{
						count++;
					}
					break;
				case 2:   //all hostile ship
					if (ent instanceof BasicEntityShipHostile)
					{
						count++;
					}
					break;
				default:  //small or large mob ship
					if (ent instanceof BasicEntityShipHostile && ent.isNonBoss())
					{
						count++;
					}
					break;
				}
			}
		}//end world not null
		
		return count;
	}
	
	//clear seat2
  	public static void clearMountSeat(EntityLiving host)
  	{
  		//清空座騎, 若騎乘ship mount, 則順便清空ship mount
  		if (host.getRidingEntity() != null)
  		{
  			if (host.getRidingEntity() instanceof BasicEntityMount)
  			{
	  			((BasicEntityMount) host.getRidingEntity()).clearRider();
  			}
  			
  			host.dismountRidingEntity();
  		}
  		
  		//清空乘客
  		for (Entity p : host.getPassengers())
  		{
  			p.dismountRidingEntity();
  		}
  	}
  	
  	/** get entity with specific class/interface
  	 */
  	public static <T> ArrayList getEntitiesWithinAABB(World world, Class <? extends T > cls, AxisAlignedBB aabb, @Nullable Predicate filter)
    {
  		//get all entity within AABB
  		ArrayList<Entity> list = (ArrayList<Entity>) world.getEntitiesWithinAABB(Entity.class, aabb);
  		ArrayList<T> list2 = new ArrayList<T>();
  		
  		//check entity class
        for (Entity entity : list)
        {
            if (cls.isAssignableFrom(entity.getClass()) && filter.apply((T) entity))
            {
            	list2.add((T) entity);
            }
        }

        return list2;
    }
  	
  	/**
  	 * custom entity moving method
  	 * 1. floating on water/lava/liquid block
  	 * 2. change movSpeed in water
  	 */
  	public static <T extends EntityLivingBase & IShipNavigator> void moveEntityWithHeading(T host, float strafe, float forward)
	{
        double d0;
        
        //液體中移動
        if (EntityHelper.checkEntityIsInLiquid(host))
        {
            d0 = host.posY;
            host.moveRelative(strafe, forward, host.getMoveSpeed() * 0.4F); //水中的速度計算(含漂移效果)
            host.move(host.motionX, host.motionY, host.motionZ);
            
            //水中阻力
            host.motionX *= 0.8D;
            host.motionY *= 0.8D;
            host.motionZ *= 0.8D;
            
            //水中撞到東西會上升
            if (host.isCollidedHorizontally &&
            	host.isOffsetPositionInLiquid(host.motionX, host.motionY + 0.6D - host.posY + d0, host.motionZ))
            {
                host.motionY = 0.3D;
            }
        }
        //非液體中移動
        else
        {
            float f6 = 0.91F;
            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain(host.posX, host.getEntityBoundingBox().minY - 1D, host.posZ);

            //取得地面磨擦係數
            if (host.onGround)
            {
                f6 = host.world.getBlockState(blockpos$pooledmutableblockpos).getBlock().slipperiness * 0.91F;
            }

            //計算地面磨擦力效果
            float f7 = 0.16277136F / (f6 * f6 * f6);
            float f8;

            //move speed on ground
            if (host.onGround)
            {
                f8 = host.getMoveSpeed() * f7;
            }
            //move speed in air
            else
            {
                f8 = host.jumpMovementFactor;
            }
            
            //計算跳躍Y速度值
            if (host.isJumping())
            {
        		host.motionY += host.getMoveSpeed() * host.getJumpSpeed() * 0.1D;
            }

            //計算實際XZ速度值
            host.moveRelative(strafe, forward, f8);
            
            //再次判定entity是否還站在地面, 重取地面摩擦係數
            f6 = 0.91F;

            if (host.onGround)
            {
                f6 = host.world.getBlockState(blockpos$pooledmutableblockpos.setPos(host.posX, host.getEntityBoundingBox().minY - 1D, host.posZ)).getBlock().slipperiness * 0.91F;
            }

            //判定是否在樓梯移動狀態
            if (host.isOnLadder())
            {
                float f9 = 0.15F;
                host.motionX = MathHelper.clamp(host.motionX, (double)(-f9), (double)f9);
                host.motionZ = MathHelper.clamp(host.motionZ, (double)(-f9), (double)f9);
                host.fallDistance = 0F;

                if (host.motionY < -0.15D)
                {
                    host.motionY = -0.15D;
                }
            }

            //實際移動entity
            host.move(host.motionX, host.motionY, host.motionZ);

            //若移動方向為衝撞樓梯, 則給予上升值
            if (host.isCollidedHorizontally && host.isOnLadder())
            {
                host.motionY = 0.4D;
            }

            //漂浮藥水效果 or 獲得飛行能力時, 新增往上漂浮移動值
            if (host.isPotionActive(MobEffects.LEVITATION))
            {
                host.motionY += (0.05D * (double)(host.getActivePotionEffect(MobEffects.LEVITATION).getAmplifier() + 1) - host.motionY) * 0.2D;
            }
            //無漂浮效果, 計算自然掉落
            else
            {
                blockpos$pooledmutableblockpos.setPos(host.posX, 0.0D, host.posZ);

                if (!host.world.isRemote || host.world.isBlockLoaded(blockpos$pooledmutableblockpos) && host.world.getChunkFromBlockCoords(blockpos$pooledmutableblockpos).isLoaded())
                {
                    if (!host.hasNoGravity())
                    {
                    	host.motionY -= 0.08D;
                    }
                }
                else if (host.posY > 0.0D)
                {
                	host.motionY = -0.1D;
                }
                else
                {
                	host.motionY = 0.0D;
                }
            }

            //計算三方向阻力效果
            host.motionY *= 0.98D;
            host.motionX *= f6;
            host.motionZ *= f6;
            blockpos$pooledmutableblockpos.release();
        }
        
        //計算四肢擺動值
        host.prevLimbSwingAmount = host.limbSwingAmount;
        double d5 = host.posX - host.prevPosX;
        double d7 = host.posZ - host.prevPosZ;
        float f10 = MathHelper.sqrt(d5 * d5 + d7 * d7) * 4F;

        if (f10 > 1F)
        {
            f10 = 1F;
        }

        host.limbSwingAmount += (f10 - host.limbSwingAmount) * 0.4F;
        host.limbSwing += host.limbSwingAmount;
    }
  	
  	/**
  	 * check passenger is specific class
  	 * parms: mount, target class, id
  	 * id: -1:none, 0~N: check only at Nth passenger
  	 */
  	public static boolean checkPassengerClass(Entity mount, Class cls)
  	{
  		return checkPassengerClass(mount, cls, -1);
  	}
  	
  	public static boolean checkPassengerClass(Entity mount, Class cls, int id)
  	{
  		if (mount != null && cls != null)
  		{
  			List<Entity> list = mount.getPassengers();
  			
  			if (list.size() > 0 && list.size() > id)
  			{
  				//no id checking
  				if (id < 0)
  				{
  	  				for (int i = 0; i < list.size(); i++)
  	  				{
  	  					if (cls.isAssignableFrom(list.get(i).getClass())) return true;
  	  				}
  				}
  				//apply id checking
  				else
  				{
  					if (cls.isAssignableFrom(list.get(id).getClass())) return true;
  				}
  			}//end list not empty
  		}//end null check
  		
  		return false;
  	}
  	
  	/**
  	 * get selected or offhand PointerItem
  	 */
  	public static ItemStack getPointerInUse(EntityPlayer player)
  	{
  		ItemStack pointer = null;
  		
  		if (player != null)
  		{
  			ItemStack itemMain = player.inventory.getCurrentItem();
  			ItemStack[] itemOff = player.inventory.offHandInventory;

  			//check main hand
  			if (itemMain != null && itemMain.getItem() == ModItems.PointerItem)
  			{
  				pointer = itemMain;
  			}
  			//check off hand
  			else
  			{
  	  			for (ItemStack i : itemOff)
  	  			{
  	  	  			if (i != null && i.getItem() == ModItems.PointerItem)
  	  	  			{
  	  	  				pointer = i;
  	  	  				break;
  	  	  			}
  	  			}
  			}
  		}
  		
  		return pointer;
  	}
  	
    //check entity is not host or launcher
    public static boolean isNotHost(IShipOwner host, Entity target)
    {
    	//null check
    	if (host == null || target == null) return false;
    	
    	//not self
    	if (target.equals(host)) return false;
    	
		if (host.getHostEntity() != null)
		{
			Entity host2 = host.getHostEntity();
			
			//not launcher
			if (target.equals(host2)) return false;
			
			//not host's mounts
			if (target.equals(host2.getRidingEntity())) return false;
			
			//not riders
			for (Entity rider : host2.getPassengers())
			{
				if (target.equals(rider)) return false;
			}
		}
		
		return true;
	}
  	
  	
}
