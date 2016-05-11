package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.entities.EntityParent;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class ServerSyncPacket implements IMessage{
	private int id;
	private double posX;
	private double posY;
	private double posZ;
	private double motionX;
	private double motionY;
	private double motionZ;
	private float pitch;
	private float roll;
	private float yaw;

	public ServerSyncPacket() { }
	
	public ServerSyncPacket(int id, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float pitch, float roll, float yaw){
		this.id=id;
		this.posX=posX;
		this.posY=posY;
		this.posZ=posZ;
		this.motionX=motionX;
		this.motionY=motionY;
		this.motionZ=motionZ;
		this.pitch=pitch;
		this.roll=roll;
		this.yaw=yaw;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.posX=buf.readDouble();
		this.posY=buf.readDouble();
		this.posZ=buf.readDouble();
		this.motionX=buf.readDouble();
		this.motionY=buf.readDouble();
		this.motionZ=buf.readDouble();
		this.pitch=buf.readFloat();
		this.roll=buf.readFloat();
		this.yaw=buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeDouble(this.posX);
		buf.writeDouble(this.posY);
		buf.writeDouble(this.posZ);
		buf.writeDouble(this.motionX);
		buf.writeDouble(this.motionY);
		buf.writeDouble(this.motionZ);
		buf.writeFloat(this.pitch);
		buf.writeFloat(this.roll);
		buf.writeFloat(this.yaw);
	}

	public static class ServerSyncPacketHandler implements IMessageHandler<ServerSyncPacket, IMessage>{
		@Override
		public IMessage onMessage(ServerSyncPacket message, MessageContext ctx) {
			if(ctx.side==Side.CLIENT){
				EntityParent thisEntity = (EntityParent) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
				if(thisEntity != null){
					thisEntity.posX = rectifyValue(thisEntity.posX, message.posX, 0.01, 10);
					thisEntity.posY = rectifyValue(thisEntity.posY, message.posY, 0.01, 10);
					thisEntity.posZ = rectifyValue(thisEntity.posZ, message.posZ, 0.01, 10);
					thisEntity.motionX = rectifyValue(thisEntity.motionX, message.motionX, 0.01, 0.2);
					thisEntity.motionY = rectifyValue(thisEntity.motionY, message.motionY, 0.01, 0.2);
					thisEntity.motionZ = rectifyValue(thisEntity.motionZ, message.motionZ, 0.01, 0.2);
					thisEntity.rotationPitch = (float) rectifyValue(thisEntity.rotationPitch, message.pitch, 0.01, 5.0);
					thisEntity.rotationRoll = (float) rectifyValue(thisEntity.rotationRoll, message.roll, 0.01, 5.0);
					thisEntity.rotationYaw = (float) rectifyValue(thisEntity.rotationYaw, message.yaw, 0.01, 5.0);
					thisEntity.moveChildren();
				}
			}
			return null;
		}
	}
	
	private static double rectifyValue(double currentValue, double packetValue, double increment, double cutoff){
		if(currentValue > packetValue){
			if(currentValue - packetValue > cutoff){
				return packetValue;
			}else{
				return currentValue - Math.min(currentValue - packetValue, increment);
			}
		}else{
			if(packetValue - currentValue > cutoff){
				return packetValue;
			}else{
				return currentValue + Math.min(packetValue - currentValue, increment);
			}
		}
	}
}