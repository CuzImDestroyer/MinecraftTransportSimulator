package minecrafttransportsimulator.rendering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**Main render class for all multipart entities.
 * Renders the parent model, and all child models that have been registered by
 * {@link registerChildRender}.  Ensures all parts are rendered in the exact
 * location they should be in as all rendering is done in the same operation.
 * Entities don't render above 255 well due to the new chunk visibility system.
 * This code is present to be called manually from
 * {@link ClientEventSystem#on(RenderWorldLastEvent)}.
 *
 * @author don_bruce
 */
public final class RenderMultipart{
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	private static final Map<String, ResourceLocation> textureArray = new HashMap<String, ResourceLocation>();
	private static final Map<String, Map<String, Float[][]>> modelArray = new HashMap<String, Map<String, Float[][]>>();
    
	public static void render(EntityMultipartMoving mover, EntityPlayer playerRendering, float partialTicks){
		if(mover.pack == null){
			return;
		}
		
		//Bind texture.  Adds new element to cache if needed.
		if(!textureArray.containsKey(mover.pack.general.name)){
			ResourceLocation textureLocation;
			if(mover.pack.rendering.useCustomModelTexture){
				textureLocation = new ResourceLocation(mover.pack.rendering.modelTexture);
			}else{
				textureLocation = new ResourceLocation(MTS.MODID, mover.pack.rendering.modelTexture);
			}
			textureArray.put(mover.pack.general.name, textureLocation);
		}
		minecraft.getTextureManager().bindTexture(textureArray.get(mover.pack.general.name));
		
		//Get model
		if(!modelArray.containsKey(mover.pack.general.name)){
			modelArray.put(mover.pack.general.name, OBJParserSystem.parseOBJModel(mover.pack.general.name + ".obj"));
		}
		
		//Translate model
		GL11.glPushMatrix();		
        Entity renderViewEntity = minecraft.getRenderViewEntity();
        double playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
        double playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
        double playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
        double thisX = mover.lastTickPosX + (mover.posX - mover.lastTickPosX) * (double)partialTicks;
        double thisY = mover.lastTickPosY + (mover.posY - mover.lastTickPosY) * (double)partialTicks;
        double thisZ = mover.lastTickPosZ + (mover.posZ - mover.lastTickPosZ) * (double)partialTicks;
        GL11.glTranslated(thisX - playerX, thisY - playerY, thisZ - playerZ);
        
        //Set up lighting
        int i = mover.getBrightnessForRender(partialTicks);
        minecraft.entityRenderer.enableLightmap();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, i % 65536, i / 65536);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.depthFunc(515);
        //TODO why does this affect lighting of the model?
        RenderHelper.enableStandardItemLighting();
		
        //Render model
		GL11.glPushMatrix();
		if(mover.pack.rendering.flipModel){
			//GL11.glRotatef(180, 1, 0, 0);
			GL11.glRotatef(45 - 180, 1, 0, 0);
			//GL11.glRotatef(mover.ticksExisted%360, 1, 0, 0);
		}
		GL11.glTranslatef(mover.pack.rendering.modelOffset[0], mover.pack.rendering.modelOffset[1], mover.pack.rendering.modelOffset[2]);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Entry<String, Float[][]> entry : modelArray.get(mover.pack.general.name).entrySet()){
			//TODO add actuation for control surfaces.
			for(Float[] vertex : entry.getValue()){
				//TODO put this into OpenGLHelper.
				/*		//Calculate normals.
				List<Float[]> normalList = new ArrayList<Float[]>();
				for(int i=0; i<faceValues.size() - 3; i += 3){
					Float[] faceVertex1 = vertexList.get(faceValues.get(i)[0] - vertexOffset);
					Float[] faceVertex2 = vertexList.get(faceValues.get(i + 1)[0] - vertexOffset);
					Float[] faceVertex3 = vertexList.get(faceValues.get(i + 2)[0] - vertexOffset);
					Vector3f v1 = new Vector3f(faceVertex1[0], faceVertex1[1], faceVertex1[2]);
					Vector3f v2 = new Vector3f(faceVertex2[0], faceVertex2[1], faceVertex2[2]);
					Vector3f v3 = new Vector3f(faceVertex3[0], faceVertex3[1], faceVertex3[2]);
					Vector3f norm = Vector3f.cross(Vector3f.sub(v2, v1, null), Vector3f.sub(v3, v1, null), null);
					//Add 3 times.  Once for each vertex in this shape.
					System.out.format("x:%f, y:%f, z:%f\n", norm.x, norm.y, norm.z);
					normalList.add(new Float[]{norm.x, norm.y, norm.z});
					normalList.add(new Float[]{norm.x, norm.y, norm.z});
					normalList.add(new Float[]{norm.x, norm.y, norm.z});
				}*/
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				///GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
			}
		}
		GL11.glEnd();
		GL11.glPopMatrix();
		
		//Render children
		for(EntityMultipartChild child : mover.getChildren()){
        	if(MTSRegistryClient.childRenderMap.get(child.getClass()) != null){
        		//Render child model.
        		MTSVector childOffset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, mover.rotationPitch, mover.rotationYaw, mover.rotationRoll);
				MTSRegistryClient.childRenderMap.get(child.getClass()).render(child, childOffset.xCoord, childOffset.yCoord, childOffset.zCoord, partialTicks);
			}
        }
	
		//Render bounding boxes if applicable.
		if(minecraft.gameSettings.showDebugInfo){
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glColor3f(0.0F, 0.0F, 0.0F);
			GL11.glLineWidth(3.0F);
			for(Entity entity : minecraft.theWorld.loadedEntityList){
				if(entity instanceof EntityMultipartChild){
					EntityMultipartChild child = (EntityMultipartChild) entity;
					if(child.parent != null){
						if(child.parent.UUID.equals(mover.UUID)){
							GL11.glPushMatrix();
							MTSVector childOffset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, mover.rotationPitch, mover.rotationYaw, mover.rotationRoll);
							GL11.glTranslated(childOffset.xCoord, childOffset.yCoord, childOffset.zCoord);
							renderBoundingBox(child.getEntityBoundingBox().offset(-child.posX, -child.posY, -child.posZ));
							GL11.glPopMatrix();
						}
					}
				}
			}
			GL11.glLineWidth(1.0F);
			GL11.glColor3f(1.0F, 1.0F, 1.0F);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glPopMatrix();
		}
		
        RenderHelper.disableStandardItemLighting();
        minecraft.entityRenderer.disableLightmap();
        GL11.glPopMatrix();
	}
	
	private static void renderBoundingBox(AxisAlignedBB box){
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(box.minX, box.minY, box.minZ);
		GL11.glVertex3d(box.maxX, box.minY, box.minZ);
		GL11.glVertex3d(box.minX, box.minY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
		GL11.glVertex3d(box.minX, box.maxY, box.minZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
		GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
		
		GL11.glVertex3d(box.minX, box.minY, box.minZ);
		GL11.glVertex3d(box.minX, box.minY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.minY, box.minZ);
		GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
		GL11.glVertex3d(box.minX, box.maxY, box.minZ);
		GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
		
		GL11.glVertex3d(box.minX, box.minY, box.minZ);
		GL11.glVertex3d(box.minX, box.maxY, box.minZ);
		GL11.glVertex3d(box.maxX, box.minY, box.minZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
		GL11.glVertex3d(box.minX, box.minY, box.maxZ);
		GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
		GL11.glEnd();
	}
	
	/*
	private void renderLightBeams(EntityPlane plane){
    	GL11.glPushMatrix();
    	Minecraft.getMinecraft().entityRenderer.disableLightmap();
    	if(MinecraftForgeClient.getRenderPass() == -1){
    		GL11.glTranslated(-renderOffset[0], -renderOffset[1], -renderOffset[2]);
        }
    	if((plane.lightStatus & 4) == 4){
        	renderTaxiBeam(plane);
        }
    	if((plane.lightStatus & 8) == 8){
    		renderLandingBeam(plane);
    	}
    	Minecraft.getMinecraft().entityRenderer.enableLightmap();
    	GL11.glPopMatrix();
	}
	
	protected abstract float[] getRenderOffset();
	protected abstract void renderPlane(EntityPlane plane);
	protected abstract void renderWindows(EntityPlane plane);
	protected abstract void renderConsole(EntityPlane plane);
	protected abstract void renderMarkings(EntityPlane plane);
	protected abstract void renderNavigationLights(EntityPlane plane, float brightness);
	protected abstract void renderStrobeLights(EntityPlane plane, float brightness);
	protected abstract void renderTaxiLights(EntityPlane plane, float brightness);
	protected abstract void renderLandingLights(EntityPlane plane, float brightness);
	protected abstract void renderTaxiBeam(EntityPlane plane);
	protected abstract void renderLandingBeam(EntityPlane plane);
		*/
	
	private void renderDebugVectors(EntityPlane plane){
		double[] debugForces = plane.getDebugForces();
    	GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glLineWidth(1);
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.headingVec.xCoord*debugForces[0], 2 + plane.headingVec.yCoord*debugForces[0],  plane.headingVec.zCoord*debugForces[0]);
		
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)), 2, Math.sin(Math.toRadians(plane.rotationYaw)));
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);

		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)), 2, -Math.sin(Math.toRadians(plane.rotationYaw)));
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glEnd();
		
		GL11.glLineWidth(5);
		GL11.glColor4f(1, 0, 0, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(plane.headingVec.xCoord*debugForces[0], 2 + plane.headingVec.yCoord*debugForces[0],  plane.headingVec.zCoord*debugForces[0]);
		GL11.glVertex3d(plane.headingVec.xCoord*(debugForces[0] - debugForces[1]), 2 + plane.headingVec.yCoord*(debugForces[0] - debugForces[1]),  plane.headingVec.zCoord*(debugForces[0] - debugForces[1]));
		
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*(debugForces[2] - debugForces[3])/10, 2 + plane.verticalVec.yCoord*(debugForces[2] - debugForces[3])/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*(debugForces[2] - debugForces[3])/10);
		
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*(debugForces[2] - debugForces[3])/10, 2 + plane.verticalVec.yCoord*(debugForces[2] - debugForces[3])/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*(debugForces[2] - debugForces[3])/10);
		GL11.glEnd();
		
		GL11.glColor4f(0, 0, 1, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.velocityVec.xCoord*plane.velocity, 2 + plane.velocityVec.yCoord*plane.velocity,  plane.velocityVec.zCoord*plane.velocity);
		GL11.glEnd();
		
		GL11.glColor4f(0, 1, 0, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.headingVec.xCoord, 2 + plane.headingVec.yCoord,  plane.headingVec.zCoord);
		GL11.glEnd();
				
		GL11.glLineWidth(1);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
	}
}