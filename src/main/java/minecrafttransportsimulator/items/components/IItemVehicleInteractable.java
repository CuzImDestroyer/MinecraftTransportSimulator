package minecrafttransportsimulator.items.components;

import minecrafttransportsimulator.packets.vehicles.PacketVehicleInteract;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperPlayer;

/**Interface that performs an action on vehicles.  The methods in here will be called 
 * from {@link PacketVehicleInteract} on the server when a player clicks a vehicle on a client.
 * 
 * @author don_bruce
 */
public interface IItemVehicleInteractable{
	
	/**
	 *  Performs interaction on the vehicle.  Dependent on the state of the passed-in vehicle.
	 *  Also passes-in a flag that determines if the player is allowed to edit the vehicle's state.
	 *  Is a combination of owner name and if the player is OP.  This is only called on the SERVER.
	 *  Note that the part passed in MAY be null if the box clicked was a collision box or part slot.
	 */
	public void doVehicleInteraction(EntityVehicleE_Powered vehicle, APart<? extends EntityVehicleE_Powered> part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick, WrapperNBT data);
	
	public static enum PlayerOwnerState{
		USER,
		OWNER,
		ADMIN;
	}
}
