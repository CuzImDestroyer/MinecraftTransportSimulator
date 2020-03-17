package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInit;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartAddition;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartRemoval;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.wrappers.WrapperItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**Base vehicle class.  All vehicle entities should extend this class.
 * It is primarily responsible for the adding and removal of parts,
 * as well as dealing with what happens when this part is killed.
 * It is NOT responsible for custom data sets, sounds, or movement.
 * That should be done in sub-classes to keep methods segregated.
 * 
 * @author don_bruce
 */
abstract class EntityVehicleA_Base extends Entity{
	/**The pack definition for this vehicle.  This is set upon NBT load on the server, but needs a packet
	 * to be present on the client.  Do NOT assume this will be valid simply because
	 * the vehicle has been loaded!
	 */
	public JSONVehicle definition;
	
	/**This list contains all parts this vehicle has.  Do NOT use it in loops or you will get CMEs all over!
	 * Use the getVehicleParts() method instead to return a loop-safe array.*/
	private final List<APart<? extends EntityVehicleE_Powered>> parts = new ArrayList<APart<? extends EntityVehicleE_Powered>>();

	/**Cooldown byte to prevent packet spam requests during client-side loading of part packs.**/
	private byte clientPackPacketCooldown = 0;
	
	public EntityVehicleA_Base(World world){
		super(world);
	}
	
	public EntityVehicleA_Base(World world, JSONVehicle definition){
		this(world);
		this.definition = definition;
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		//We need to get pack data manually if we are on the client-side.
		///Although we could call this in the constructor, Minecraft changes the
		//entity IDs after spawning and that fouls things up.
		if(definition == null){
			if(world.isRemote){
				if(clientPackPacketCooldown == 0){
					clientPackPacketCooldown = 40;
					MTS.MTSNet.sendToServer(new PacketVehicleClientInit((EntityVehicleE_Powered) this));
				}else{
					--clientPackPacketCooldown;
				}
			}
		}
	}
	
    @Override
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport){
    	//Overridden due to stupid tracker behavior.
    	//Client-side render changes calls put in its place.
    	setRenderDistanceWeight(100);
    	this.ignoreFrustumCheck = true;
    }
    
    /**
	 * Adds the passed-part to this vehicle, but in this case the part is in item form
	 * with associated data rather than a fully-constructed form.  This method will check
	 * if the item-based part can go to this vehicle.  If so, it is constructed and added,
	 * and a packet is sent to all clients to inform them of this change.  Returns true
	 * if all operations completed, false if the part was not able to be added.
	 * Note that the passed-in data MAY be null if the item didn't have any.
	 */
    public boolean addPartFromItem(WrapperItemStack stack, double xPos, double yPos, double zPos){
    	for(Entry<Vec3d, VehiclePart> packPartEntry : getAllPossiblePackParts().entrySet()){
    		//Check to see if this is the part we want to add.
    		if(packPartEntry.getKey().x == xPos && packPartEntry.getKey().y == yPos && packPartEntry.getKey().z == zPos){
	    		//Check to make sure the spot is free.
				if(getPartAtLocation(xPos, yPos, zPos) == null){
					//Check to make sure the part is valid.
					if(packPartEntry.getValue().types.contains(partItem.definition.general.type)){
						//Check to make sure the part is in parameter ranges.
						if(partItem.isPartValidForPackDef(packPartEntry.getValue())){
							//Part is valid.  Create it and add it.
							addPart(PackParserSystem.createPart((EntityVehicleE_Powered) this, packPartEntry.getValue(), partItem.definition, stack.stack.hasTagCompound() ? stack.stack.getTagCompound() : new NBTTagCompound()), false);
							MTS.MTSNet.sendToAll(new PacketVehicleClientPartAddition((EntityVehicleE_Powered) this, xPos, yPos, zPos, partItem, partTag));
							return true;
						}
					}
				}
    		}
		}
    	return false;
    }
	
	public void addPart(APart<? extends EntityVehicleE_Powered> part, boolean ignoreCollision){
		parts.add(part);
		if(!ignoreCollision){
			//Check for collision, and boost if needed.
			if(part.isPartCollidingWithBlocks(Vec3d.ZERO)){
				this.setPositionAndRotation(posX, posY + part.getHeight(), posZ, rotationYaw, rotationPitch);
			}
			
			//Sometimes we need to do this for parts that are deeper into the ground.
			if(part.isPartCollidingWithBlocks(new Vec3d(0, Math.max(0, -part.offset.y) + part.getHeight(), 0))){
				this.setPositionAndRotation(posX, posY +  part.getHeight(), posZ, rotationYaw, rotationPitch);
			}
		}
	}
	
	public void removePart(APart<? extends EntityVehicleE_Powered> part, boolean playBreakSound){
		if(parts.contains(part)){
			parts.remove(part);
			if(part.isValid()){
				part.removePart();
				if(!world.isRemote){
					MTS.MTSNet.sendToAll(new PacketVehicleClientPartRemoval((EntityVehicleE_Powered) this, part.offset.x, part.offset.y, part.offset.z));
				}
			}
			if(!world.isRemote){
				if(playBreakSound){
					this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 2.0F, 1.0F);
				}
			}
		}
	}
	
	/**
	 * Returns a loop-safe array for iterating over parts.
	 * Use this for everything that needs to look at parts.
	 */
	public List<APart<? extends EntityVehicleE_Powered>> getVehicleParts(){
		return ImmutableList.copyOf(parts);
	}
	
	/**
	 * Gets the part at the specified location.
	 */
	public APart<? extends EntityVehicleE_Powered> getPartAtLocation(double offsetX, double offsetY, double offsetZ){
		for(APart<? extends EntityVehicleE_Powered> part : this.parts){
			if(part.offset.x == offsetX && part.offset.y == offsetY && part.offset.z == offsetZ){
				return part;
			}
		}
		return null;
	}
	
	/**
	 * Gets all possible pack parts.  This includes additional parts on the vehicle
	 * and extra parts of parts on other parts.  Map returned is the position of the
	 * part positions and the part pack information at those positions.
	 * Note that additional parts will not be added if no part is present
	 * in the primary location.
	 */
	public Map<Vec3d, VehiclePart> getAllPossiblePackParts(){
		Map<Vec3d, VehiclePart> packParts = new HashMap<Vec3d, VehiclePart>();
		//First get all the regular part spots.
		for(VehiclePart packPart : definition.parts){
			Vec3d partPos = new Vec3d(packPart.pos[0], packPart.pos[1], packPart.pos[2]);
			packParts.put(partPos, packPart);
			
			//Check to see if we can put an additional part in this location.
			//If a part is present at a location that can have an additional part, we allow it to be placed.
			while(packPart.additionalPart != null){
				boolean foundPart = false;
				for(APart<? extends EntityVehicleE_Powered> part : this.parts){
					if(part.offset.equals(partPos)){
						partPos = new Vec3d(packPart.additionalPart.pos[0], packPart.additionalPart.pos[1], packPart.additionalPart.pos[2]);
						packPart = packPart.additionalPart;
						packParts.put(partPos, packPart);
						foundPart = true;
						break;
					}
				}
				if(!foundPart){
					break;
				}
			}
		}
		
		//Next get any sub parts on parts that are present.
		for(APart<? extends EntityVehicleE_Powered> part : this.parts){
			if(part.definition.subParts != null){
				VehiclePart parentPack = getPackDefForLocation(part.offset.x, part.offset.y, part.offset.z);
				for(VehiclePart extraPackPart : part.definition.subParts){
					VehiclePart correctedPack = getPackForSubPart(parentPack, extraPackPart);
					packParts.put(new Vec3d(correctedPack.pos[0], correctedPack.pos[1], correctedPack.pos[2]), correctedPack);
				}
			}
			
		}
		return packParts;
	}
	
	/**
	 * Gets the pack definition at the specified location.
	 */
	public VehiclePart getPackDefForLocation(double offsetX, double offsetY, double offsetZ){
		//Check to see if this is a main part.
		for(VehiclePart packPart : definition.parts){
			if(packPart.pos[0] == offsetX && packPart.pos[1] == offsetY && packPart.pos[2] == offsetZ){
				return packPart;
			}
			
			//Not a main part.  Check if this is an additional part.
			while(packPart.additionalPart != null){
				if(packPart.additionalPart.pos[0] == offsetX && packPart.additionalPart.pos[1] == offsetY && packPart.additionalPart.pos[2] == offsetZ){
					return packPart.additionalPart;
				}else{
					packPart = packPart.additionalPart;
				}
			}
		}
		
		//If this is not a main part or an additional part, check the sub-parts.
		for(APart<? extends EntityVehicleE_Powered> part : this.parts){
			if(part.definition.subParts.size() > 0){
				VehiclePart parentPack = getPackDefForLocation(part.offset.x, part.offset.y, part.offset.z);
				for(VehiclePart extraPackPart : part.definition.subParts){
					VehiclePart correctedPack = getPackForSubPart(parentPack, extraPackPart);
					if(correctedPack.pos[0] == offsetX && correctedPack.pos[1] == offsetY && correctedPack.pos[2] == offsetZ){
						return correctedPack;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns a PackPart with the correct properties for a SubPart.  This is because
	 * subParts inherit some properties from their parent parts. 
	 */
	private VehiclePart getPackForSubPart(VehiclePart parentPack, VehiclePart subPack){
		VehiclePart correctPack = this.definition.new VehiclePart();
		correctPack.pos = new float[3];
		//If we will be mirrored, make sure to invert the x-coords of any sub-parts.
		correctPack.pos[0] = parentPack.pos[0] < 0 ^ parentPack.inverseMirroring ? parentPack.pos[0] - subPack.pos[0] : parentPack.pos[0] + subPack.pos[0];
		correctPack.pos[1] = parentPack.pos[1] + subPack.pos[1];
		correctPack.pos[2] = parentPack.pos[2] + subPack.pos[2];
		
		if(parentPack.rot != null || subPack.rot != null){
			correctPack.rot = new float[3];
		}
		if(parentPack.rot != null){
			correctPack.rot[0] += parentPack.rot[0];
			correctPack.rot[1] += parentPack.rot[1];
			correctPack.rot[2] += parentPack.rot[2];
		}
		if(subPack.rot != null){
			correctPack.rot[0] += subPack.rot[0];
			correctPack.rot[1] += subPack.rot[1];
			correctPack.rot[2] += subPack.rot[2];
		}
		
		correctPack.turnsWithSteer = parentPack.turnsWithSteer;
		correctPack.isController = subPack.isController;
		correctPack.inverseMirroring = subPack.inverseMirroring;
		correctPack.types = subPack.types;
		correctPack.customTypes = subPack.customTypes;
		correctPack.minValue = subPack.minValue;
		correctPack.maxValue = subPack.maxValue;
		return correctPack;
	}
			
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		//Check to see if we were an old or new vehicle.  If we are old, load using the old naming convention.
		if(tagCompound.hasKey("vehicleName")){
			String oldVehicleName = tagCompound.getString("vehicleName");
			String parsedPackID = oldVehicleName.substring(0, oldVehicleName.indexOf(':'));
			String parsedSystemName =  oldVehicleName.substring(oldVehicleName.indexOf(':') + 1);
			this.definition = (JSONVehicle) MTSRegistry.packItemMap.get(parsedPackID).get(parsedSystemName).definition;
		}else{
			this.definition = (JSONVehicle) MTSRegistry.packItemMap.get(tagCompound.getString("packID")).get(tagCompound.getString("systemName")).definition;
		}
		
		if(this.parts.size() == 0){
			NBTTagList partTagList = tagCompound.getTagList("Parts", 10);
			for(byte i=0; i<partTagList.tagCount(); ++i){
				//Use a try-catch for parts in case they've changed since this vehicle was last placed.
				//Don't want crashes due to pack updates.
				try{
					NBTTagCompound partTag = partTagList.getCompoundTagAt(i);
					VehiclePart packPart = getPackDefForLocation(partTag.getDouble("offsetX"), partTag.getDouble("offsetY"), partTag.getDouble("offsetZ"));
					//If we are using the old naming system for this vehicle, use it to load parts too.
					if(tagCompound.hasKey("vehicleName")){
						String oldPartName = partTag.getString("partName");
						String parsedPackID = oldPartName.substring(0, oldPartName.indexOf(':'));
						String parsedSystemName =  oldPartName.substring(oldPartName.indexOf(':') + 1);
						JSONPart partDefinition = (JSONPart) MTSRegistry.packItemMap.get(parsedPackID).get(parsedSystemName).definition;
						addPart(PackParserSystem.createPart((EntityVehicleE_Powered) this, packPart, partDefinition, partTag), true);
					}else{
						JSONPart partDefinition = (JSONPart) MTSRegistry.packItemMap.get(partTag.getString("packID")).get(partTag.getString("systemName")).definition;
						addPart(PackParserSystem.createPart((EntityVehicleE_Powered) this, packPart, partDefinition, partTag), true);
					}
					
				}catch(Exception e){
					MTS.MTSLog.error("ERROR IN LOADING PART FROM NBT!");
					e.printStackTrace();
				}
			}
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setString("packID", definition.packID);
		tagCompound.setString("systemName", definition.systemName);
		
		NBTTagList partTagList = new NBTTagList();
		for(APart<? extends EntityVehicleE_Powered> part : this.getVehicleParts()){
			//Don't save the part if it's not valid.
			if(part.isValid()){
				NBTTagCompound partTag = part.getPartNBTTag();
				//We need to set some extra data here for the part to allow this vehicle to know where it went.
				//This only gets set here during saving/loading, and is NOT returned in the item that comes from the part.
				partTag.setString("packID", part.definition.packID);
				partTag.setString("systemName", part.definition.systemName);
				partTag.setDouble("offsetX", part.offset.x);
				partTag.setDouble("offsetY", part.offset.y);
				partTag.setDouble("offsetZ", part.offset.z);
				partTagList.appendTag(partTag);
			}
		}
		tagCompound.setTag("Parts", partTagList);
		return tagCompound;
	}
	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
}
