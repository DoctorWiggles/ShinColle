package com.lulan.shincolle.client.gui.inventory;

import com.lulan.shincolle.tileentity.TileEntityCrane;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class ContainerCrane extends Container
{
	
	//for shift item
	private static final int SLOT_LOAD = 0;
	private static final int SLOT_UNLOAD = 9;
	private static final int SLOT_PLAYERINV = 18;
	private static final int SLOT_HOTBAR = 45;
	private static final int SLOT_ALL = 54;
		
	private TileEntityCrane tile;
	private int lenTemp;
	private int[] valueTemp;
	
	
	public ContainerCrane(InventoryPlayer invPlayer, TileEntityCrane tile)
	{
		int i, j;
		this.tile = tile;
		this.lenTemp = tile.getFieldCount();
		this.valueTemp = new int[this.lenTemp];
		
		//tile inventory
		for (i = 0; i < 9; i++)
		{
			this.addSlotToContainer(new SlotCrane(tile, i, 8+i*18, 57));
		}
		
		for (i = 0; i < 9; i++)
		{
			this.addSlotToContainer(new SlotCrane(tile, i+9, 8+i*18, 88));
		}
		
		//player inventory
		for (i = 0; i < 3; i++)
		{
			for (j = 0; j < 9; j++)
			{
				this.addSlotToContainer(new Slot(invPlayer, j+i*9+9, 8+j*18, 111+i*18));
			}
		}
		
		//player hot bar
		for (i = 0; i < 9; i++)
		{
			this.addSlotToContainer(new Slot(invPlayer, i, 8+i*18, 169));
		}
	}

	//玩家是否可以觸發右鍵點方塊事件
	@Override
	public boolean canInteractWith(EntityPlayer player)
	{
		return true;
	}
	
	/** 禁用shift功能 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotid)
	{
		return null;
    }
	
	//將container數值跟tile entity內的數值比對, 如果不同則發送更新給client使gui呈現新數值
	@Override
	public void detectAndSendChanges()
	{
		super.detectAndSendChanges();
		
		//對所有開啟gui的人發送更新, 若數值有改變則發送更新封包
		for (int i = 0; i < this.listeners.size(); ++i)
        {
            IContainerListener listener = (IContainerListener) this.listeners.get(i);
            
            //檢查所有數值是否有改變
            int temp = 0;
            boolean update = false;
            
            for (int j = 0; j < this.lenTemp; j++)
            {
            	temp = this.tile.getField(j);
            	
            	//有部份數值需要用自訂封包來發送更新
            	if (this.valueTemp[j] != temp)
            	{
                   	switch (j)
                	{
                	case 0:		//使用自訂封包更新
                		this.tile.sendSyncPacket();
                	break;
                	case 1:
                		if (this.tile.getShip() != null) this.tile.getShip().sendSyncPacketTimer();
                	break;
            		default:	//使用官方方法更新
                    	listener.sendProgressBarUpdate(this, j, temp);
            		break;
                	}
            	}
            }//end for all value temp
        }//end for all listener
		
		//更新container內的數值
		for (int k = 0; k < this.lenTemp; k++)
		{
			this.valueTemp[k] = this.tile.getField(k);
		}
            
    }

	//client端container接收新值
	@Override
	@SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value)
	{
		this.tile.setField(id, value);
    }
	
	/** 複製游標上的itemstack到crane裝卸載列表中, 不影響玩家物品欄跟手上的物品 */
	@Override
	public ItemStack slotClick(int id, int key, ClickType type, EntityPlayer player)
	{
        ItemStack itemstack = player.inventory.getItemStack();
        
        if (id >= 0 && id < 18)
        {
        	Slot slot = (Slot) this.inventorySlots.get(id);
        	
        	//left click with item
        	if (itemstack != null && key == 0)
        	{
        		ItemStack itemstack2 = itemstack.copy();
        		itemstack2.stackSize = 1;
        		slot.putStack(itemstack2);
        		tile.setItemMode(id, false);
        	}
        	//other key with item
        	else if (itemstack != null && key > 0)
        	{
        		ItemStack itemstack2 = itemstack.copy();
        		itemstack2.stackSize = 1;
        		slot.putStack(itemstack2);
        		tile.setItemMode(id, true);
        	}
        	//any key without item
        	else
        	{
        		slot.putStack(null);
        		tile.setItemMode(id, false);
        	}
        	
        	detectAndSendChanges();
        	return null;
        }

        return super.slotClick(id, key, type, player);
    }
	
	
}