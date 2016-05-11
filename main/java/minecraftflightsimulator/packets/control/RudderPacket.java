package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityPlane;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class RudderPacket implements IMessage{
	private int id;
	private byte packetType;
	private short rudderData;

	public RudderPacket() { }
	
	public RudderPacket(int id, boolean increment, short rudderCooldown){
		this.id=id;
		this.rudderData=rudderCooldown;
		this.packetType = (byte) (increment ? 1 : -1);
	}
	
	public RudderPacket(int id, short rudderAngle){
		this.id=id;
		this.rudderData=rudderAngle;
		this.packetType = 0;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.packetType=buf.readByte();
		this.rudderData=buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.packetType);
		buf.writeShort(this.rudderData);
	}

	public static class RudderPacketHandler implements IMessageHandler<RudderPacket, IMessage> {
		public IMessage onMessage(RudderPacket message, MessageContext ctx) {
			EntityPlane thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(message.packetType == 1){
					if(thisEntity.rudderAngle + thisEntity.rudderIncrement <= 250){
						thisEntity.rudderAngle += thisEntity.rudderIncrement;
						thisEntity.rudderCooldown = message.rudderData;
					}else{
						return null;
					}
				}else if(message.packetType == -1){
					if(thisEntity.rudderAngle - thisEntity.rudderIncrement >= -250){
						thisEntity.rudderAngle -= thisEntity.rudderIncrement;
						thisEntity.rudderCooldown = message.rudderData;
					}else{
						return null;
					}
				}else{
					thisEntity.rudderAngle = message.rudderData;
					thisEntity.rudderCooldown = Short.MAX_VALUE;
				}
				if(ctx.side==Side.SERVER){
					MFS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}