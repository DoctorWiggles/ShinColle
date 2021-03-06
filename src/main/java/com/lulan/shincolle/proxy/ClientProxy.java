package com.lulan.shincolle.proxy;

import com.lulan.shincolle.client.render.RenderMiscEntity;
import com.lulan.shincolle.client.render.RenderMountsEntity;
import com.lulan.shincolle.client.render.RenderShipEntity;
import com.lulan.shincolle.client.render.RenderSummonEntity;
import com.lulan.shincolle.client.render.item.RenderBasicEntityItem;
import com.lulan.shincolle.client.render.item.RenderTileEntityItem;
import com.lulan.shincolle.entity.battleship.EntityBattleshipNGT;
import com.lulan.shincolle.entity.battleship.EntityBattleshipNGTMob;
import com.lulan.shincolle.entity.battleship.EntityBattleshipRe;
import com.lulan.shincolle.entity.battleship.EntityBattleshipTa;
import com.lulan.shincolle.entity.battleship.EntityBattleshipYMT;
import com.lulan.shincolle.entity.battleship.EntityBattleshipYMTMob;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerAkatsuki;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerAkatsukiMob;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerHa;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerHibiki;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerHibikiMob;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerI;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerIkazuchi;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerIkazuchiMob;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerInazuma;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerInazumaMob;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerNi;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerRo;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerShimakaze;
import com.lulan.shincolle.entity.destroyer.EntityDestroyerShimakazeMob;
import com.lulan.shincolle.entity.hime.EntityBattleshipHime;
import com.lulan.shincolle.entity.mounts.EntityMountAfH;
import com.lulan.shincolle.entity.mounts.EntityMountBaH;
import com.lulan.shincolle.entity.mounts.EntityMountCaH;
import com.lulan.shincolle.entity.mounts.EntityMountCaWD;
import com.lulan.shincolle.entity.mounts.EntityMountHbH;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityAirplane;
import com.lulan.shincolle.entity.other.EntityAirplaneT;
import com.lulan.shincolle.entity.other.EntityAirplaneTHostile;
import com.lulan.shincolle.entity.other.EntityAirplaneTakoyaki;
import com.lulan.shincolle.entity.other.EntityAirplaneZero;
import com.lulan.shincolle.entity.other.EntityAirplaneZeroHostile;
import com.lulan.shincolle.entity.other.EntityFloatingFort;
import com.lulan.shincolle.entity.other.EntityProjectileBeam;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.entity.other.EntityRensouhouMob;
import com.lulan.shincolle.entity.other.EntityRensouhouS;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.BasicEntityItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy
{

	//client world會隨玩家所在位置持續改變, 但是dim id永遠都是0不變, 無法反推dim id?
	public static World getClientWorld()
	{
		return Minecraft.getMinecraft().world;
	}
	
	//client player
	public static EntityPlayer getClientPlayer()
	{
		return Minecraft.getMinecraft().player;
	}
	
	//client minecraft
	public static GameSettings getGameSetting()
	{
		return Minecraft.getMinecraft().gameSettings;
	}
	
	//client minecraft
	public static Minecraft getMineraft()
	{
		return Minecraft.getMinecraft();
	}

	//登錄偵測按鍵
	@Override
	public void registerKeyBindings() {}
	
	//init entity, item, block models
	@Override
	public void registerRender() throws Exception
	{
		//init item models
		ModItems.initModels();
		
		//init block models
		ModBlocks.initModels();
		
		//取代mc原本的tesr item renderer, 改為自訂的renderer
		TileEntityItemStackRenderer.instance = new RenderTileEntityItem();
		
		//custom item entity render
		RenderingRegistry.registerEntityRenderingHandler(BasicEntityItem.class, RenderBasicEntityItem.FACTORY);

		//entity render (model class, shadow size)
//		RenderingRegistry.registerEntityRenderingHandler(EntityAirfieldHime.class, new BasicShipRenderer(new ModelAirfieldHime(), 0.7F, ID.Ship.AirfieldHime));
		RenderingRegistry.registerEntityRenderingHandler(EntityBattleshipHime.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityBattleshipNGT.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityBattleshipNGTMob.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityBattleshipYMT.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityBattleshipYMTMob.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityBattleshipTa.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityBattleshipRe.class, RenderShipEntity.FACTORY_SHIP);
//		RenderingRegistry.registerEntityRenderingHandler(EntityCarrierAkagi.class, new BasicShipRenderer(new ModelCarrierAkagi(0), 0.7F, ID.Ship.CarrierAkagi));
//		RenderingRegistry.registerEntityRenderingHandler(EntityCarrierAkagiBoss.class, new BossShipRenderer(new ModelCarrierAkagi(1), 1.2F, ID.Ship.CarrierAkagi));
//		RenderingRegistry.registerEntityRenderingHandler(EntityCarrierKaga.class, new BasicShipRenderer(new ModelCarrierKaga(0), 0.7F, ID.Ship.CarrierKaga));
//		RenderingRegistry.registerEntityRenderingHandler(EntityCarrierKagaBoss.class, new BossShipRenderer(new ModelCarrierKaga(1), 1.2F, ID.Ship.CarrierKaga));
//		RenderingRegistry.registerEntityRenderingHandler(EntityCarrierHime.class, new BasicShipRenderer(new ModelCarrierHime(), 1F, ID.Ship.CarrierHime));
//		RenderingRegistry.registerEntityRenderingHandler(EntityCarrierWD.class, new BasicShipRenderer(new ModelCarrierWDemon(), 1F, ID.Ship.CarrierWD));
//		RenderingRegistry.registerEntityRenderingHandler(EntityCarrierWo.class, new BasicShipRenderer(new ModelCarrierWo(), 1F, ID.Ship.CarrierWO));
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerI.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerRo.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerHa.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerNi.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerAkatsuki.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerAkatsukiMob.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerHibiki.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerHibikiMob.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerIkazuchi.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerIkazuchiMob.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerInazuma.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerInazumaMob.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerShimakaze.class, RenderShipEntity.FACTORY_SHIP);
		RenderingRegistry.registerEntityRenderingHandler(EntityDestroyerShimakazeMob.class, RenderShipEntity.FACTORY_SHIP);
//		RenderingRegistry.registerEntityRenderingHandler(EntityHarbourHime.class, new BasicShipRenderer(new ModelHarbourHime(), 0.8F, ID.Ship.HarbourHime));
//		RenderingRegistry.registerEntityRenderingHandler(EntityHeavyCruiserRi.class, new BasicShipRenderer(new ModelHeavyCruiserRi(), 0.7F, ID.Ship.HeavyCruiserRI));
//		RenderingRegistry.registerEntityRenderingHandler(EntityHeavyCruiserNe.class, new BasicShipRenderer(new ModelHeavyCruiserNe(), 0.7F, ID.Ship.HeavyCruiserNE));
//		RenderingRegistry.registerEntityRenderingHandler(EntityNorthernHime.class, new RenderNorthernHime(new ModelNorthernHime(), 0.5F, ID.Ship.NorthernHime));
		RenderingRegistry.registerEntityRenderingHandler(EntityRensouhou.class, RenderSummonEntity.FACTORY_SUMMON);
		RenderingRegistry.registerEntityRenderingHandler(EntityRensouhouMob.class, RenderSummonEntity.FACTORY_SUMMON);
		RenderingRegistry.registerEntityRenderingHandler(EntityRensouhouS.class, RenderSummonEntity.FACTORY_SUMMON);
//		RenderingRegistry.registerEntityRenderingHandler(EntitySubmKa.class, new BasicShipRenderer(new ModelSubmKa(), 0.5F, ID.Ship.SubmarineKA));
//		RenderingRegistry.registerEntityRenderingHandler(EntitySubmYo.class, new BasicShipRenderer(new ModelSubmYo(), 0.5F, ID.Ship.SubmarineYO));
//		RenderingRegistry.registerEntityRenderingHandler(EntitySubmSo.class, new BasicShipRenderer(new ModelSubmSo(), 0.5F, ID.Ship.SubmarineSO));
//		RenderingRegistry.registerEntityRenderingHandler(EntitySubmRo500.class, new BasicShipRenderer(new ModelSubmRo500(), 0.5F, ID.Ship.SubmarineRo500));
//		RenderingRegistry.registerEntityRenderingHandler(EntitySubmRo500Mob.class, new BasicShipRenderer(new ModelSubmRo500(), 0.5F, ID.Ship.SubmarineRo500));
//		RenderingRegistry.registerEntityRenderingHandler(EntitySubmU511.class, new BasicShipRenderer(new ModelSubmU511(), 0.5F, ID.Ship.SubmarineU511));
//		RenderingRegistry.registerEntityRenderingHandler(EntitySubmU511Mob.class, new BasicShipRenderer(new ModelSubmU511(), 0.5F, ID.Ship.SubmarineU511));
//		RenderingRegistry.registerEntityRenderingHandler(EntityTransportWa.class, new BasicShipRenderer(new ModelTransportWa(), 0.7F, ID.Ship.TransportWA));
		
		//mount render
		RenderingRegistry.registerEntityRenderingHandler(EntityMountAfH.class, RenderMountsEntity.FACTORY_MOUNT);
		RenderingRegistry.registerEntityRenderingHandler(EntityMountBaH.class, RenderMountsEntity.FACTORY_MOUNT);
		RenderingRegistry.registerEntityRenderingHandler(EntityMountCaWD.class, RenderMountsEntity.FACTORY_MOUNT);
		RenderingRegistry.registerEntityRenderingHandler(EntityMountHbH.class, RenderMountsEntity.FACTORY_MOUNT);
		RenderingRegistry.registerEntityRenderingHandler(EntityMountCaH.class, RenderMountsEntity.FACTORY_MOUNT);
		
		//misc render
		RenderingRegistry.registerEntityRenderingHandler(EntityAbyssMissile.class, RenderMiscEntity.FACTORY_MISC);
		RenderingRegistry.registerEntityRenderingHandler(EntityProjectileBeam.class, RenderMiscEntity.FACTORY_MISC);
		
		//summons render
		RenderingRegistry.registerEntityRenderingHandler(EntityAirplane.class, RenderSummonEntity.FACTORY_SUMMON);
		RenderingRegistry.registerEntityRenderingHandler(EntityAirplaneTakoyaki.class, RenderSummonEntity.FACTORY_SUMMON);
		RenderingRegistry.registerEntityRenderingHandler(EntityAirplaneT.class, RenderSummonEntity.FACTORY_SUMMON);
		RenderingRegistry.registerEntityRenderingHandler(EntityAirplaneZero.class, RenderSummonEntity.FACTORY_SUMMON);
		RenderingRegistry.registerEntityRenderingHandler(EntityAirplaneTHostile.class, RenderSummonEntity.FACTORY_SUMMON);
		RenderingRegistry.registerEntityRenderingHandler(EntityAirplaneZeroHostile.class, RenderSummonEntity.FACTORY_SUMMON);
		RenderingRegistry.registerEntityRenderingHandler(EntityFloatingFort.class, RenderSummonEntity.FACTORY_SUMMON);
	
	}
	
	
}