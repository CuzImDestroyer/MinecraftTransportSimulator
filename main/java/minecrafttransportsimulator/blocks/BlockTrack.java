package minecrafttransportsimulator.blocks;

import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.baseclasses.MTSTileEntityRotateable;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockTrack extends MTSTileEntityRotateable{
	private static final AxisAlignedBB blockBox = new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 0.25F, 1.0F);
	
	public BlockTrack(){
		super(Material.IRON);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
	
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state){
		TileEntityTrack track = (TileEntityTrack) world.getTileEntity(pos);
		if(track != null){
			if(track.curve != null){
				if(!world.isRemote){
					TileEntityTrack otherEnd = (TileEntityTrack) world.getTileEntity(track.curve.blockEndPos);
					if(otherEnd != null){
						int numberTracks = (int) track.curve.pathLength;
						while(numberTracks > 0){
							int tracksInItem = Math.min(numberTracks, 64);
							world.spawnEntityInWorld(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(MTSRegistry.track, tracksInItem, this.getMetaFromState(state))));
							numberTracks -= tracksInItem;
						}
						track.removeFakeTracks();
						super.breakBlock(world, pos, state);
						world.setBlockToAir(otherEnd.getPos());
						return;
					}
				}
			}
		}
		super.breakBlock(world, pos, state);
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
		return blockBox;
	}

	@Override
	public MTSTileEntity getTileEntity(){
		return new TileEntityTrack();
	}
}
