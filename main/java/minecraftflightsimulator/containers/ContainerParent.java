package minecraftflightsimulator.containers;

import minecraftflightsimulator.entities.EntityParent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerParent extends Container{
	protected EntityParent parent;
	
	public ContainerParent(InventoryPlayer invPlayer, EntityParent parent){
		this.parent = parent;
		parent.openInventory();
        for(int j=0; j<3; ++j){
            for(int k=0; k<9; ++k){
                this.addSlotToContainer(new Slot(invPlayer, k + j * 9 + 9, 8 + k * 18, 103 + j * 18 + 18*2 + 1));
            }
        }
        for (int j=0; j<9; ++j){
            this.addSlotToContainer(new Slot(invPlayer, j, 8 + j * 18, 161 + 18*2 + 1));
        }
        parent.initParentContainerSlots(this);
	}
	
	@Override
    public Slot addSlotToContainer(Slot slot){
		return super.addSlotToContainer(slot);
    }
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return parent.isUseableByPlayer(player);
	}
	
	@Override
	public void onContainerClosed(EntityPlayer player){
		super.onContainerClosed(player);
		parent.closeInventory();
	}
	
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int sourceSlotIndex){
		ItemStack item = getSlot(sourceSlotIndex).getStack();
		if(item != null){
			for(int i=0; i<inventorySlots.size(); ++i){
				Slot slot = (Slot) inventorySlots.get(i);
				if(slot.inventory.equals(parent)){
					if(slot.isItemValid(item) && (slot.getHasStack() ? slot.getStack().stackSize < slot.getSlotStackLimit() : true)){
						if(item.stackSize != 0){
							slot.putStack(item.splitStack(1));
						}
						
					}
				}
			}
			if(item.stackSize == 0){
				((Slot) inventorySlots.get(sourceSlotIndex)).putStack(null);
			}
		}
		return null;		
	}
}