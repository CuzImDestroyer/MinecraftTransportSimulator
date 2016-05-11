package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityPlane;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class AileronPacket implements IMessage{
	private int id;
	private byte packetType;
	private short aileronData;

	public AileronPacket() { }
	
	public AileronPacket(int id, boolean increment, short aileronCooldown){
		this.id=id;
		this.aileronData=aileronCooldown;
		this.packetType = (byte) (increment ? 1 : -1);
	}
	
	public AileronPacket(int id, short aileronAngle){
		this.id=id;
		this.aileronData=aileronAngle;
		this.packetType = 0;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.packetType=buf.readByte();
		this.aileronData=buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.packetType);
		buf.writeShort(this.aileronData);
	}

	public static class AileronPacketHandler implements IMessageHandler<AileronPacket, IMessage> {
		public IMessage onMessage(AileronPacket message, MessageContext ctx) {
			EntityPlane thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(message.packetType == 1){
					if(thisEntity.aileronAngle + thisEntity.aileronIncrement <= 250){
						thisEntity.aileronAngle += thisEntity.aileronIncrement;
						thisEntity.aileronCooldown = message.aileronData;
					}else{
						return null;
					}
				}else if(message.packetType == -1){
					if(thisEntity.aileronAngle - thisEntity.aileronIncrement >= -250){
						thisEntity.aileronAngle -= thisEntity.aileronIncrement;
						thisEntity.aileronCooldown = message.aileronData;
					}else{
						return null;
					}
				}else{
					thisEntity.aileronAngle = message.aileronData;
					thisEntity.aileronCooldown = Short.MAX_VALUE;
				}
				if(ctx.side==Side.SERVER){
					MFS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}