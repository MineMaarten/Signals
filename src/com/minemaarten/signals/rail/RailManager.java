package com.minemaarten.signals.rail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.api.IRailMapper;
import com.minemaarten.signals.api.SignalsRail;
import com.minemaarten.signals.lib.Log;

public class RailManager{
    private static final RailManager INSTANCE = new RailManager();
    private final List<IRailMapper> railMappers = new ArrayList<IRailMapper>();
    private final Map<Block, IRail> blockToRails = new HashMap<Block, IRail>();

    public static RailManager getInstance(){
        return INSTANCE;
    }
    
    public void initializeAPIImplementors(ASMDataTable asmData){
    	Set<ASMData> allAnnotatedClasses = asmData.getAll(SignalsRail.class.getName());
    	for(ASMData annotatedClass : allAnnotatedClasses){
    		try {
				Class<?> clazz = Class.forName(annotatedClass.getClassName());
				Log.info("Found class annotating @SignalRail : " + annotatedClass.getClassName());
				if(IRail.class.isAssignableFrom(clazz)){
					IRail rail = (IRail)clazz.newInstance();
					for(Block applicableBlock : rail.getApplicableBlocks()){
						if(applicableBlock != null){
							Log.warning("IRail \"" + annotatedClass.getClassName() + "\" returned a null block as applicable!");
						}else{
							registerRail(applicableBlock, rail);
						}
					}
					Log.info("Successfully registered the IRail for \"" + annotatedClass.getClassName() + "\".");
				}else if(IRailMapper.class.isAssignableFrom(clazz)){
					IRailMapper railMapper = (IRailMapper)clazz.newInstance();
					registerCustomRailMapper(railMapper);
					Log.info("Successfully registered the IRailMapper for \"" + annotatedClass.getClassName() + "\".");
				}else{
					Log.error("Annotated class \"" + annotatedClass.getClassName() + "\" is not implementing IRail nor IRailMapper!");
				}
			}catch (ClassNotFoundException e) {
				e.printStackTrace();
			}catch(IllegalAccessException e){
				Log.error("Annotated class \"" + annotatedClass.getClassName() + "\" could not be instantiated, probably because it is not marked public!");
				e.printStackTrace();
			}catch(InstantiationException e){
				Log.error("Annotated class \"" + annotatedClass.getClassName() + "\" could not be instantiated, probably because it either does not have a constructor without arguments, or because the class is abstract!");
				e.printStackTrace();
			}
    	}
    }

    private void registerRail(Block railBlock, IRail rail){
        if(railBlock == null) throw new NullPointerException("Block is null!");
        if(rail == null) throw new NullPointerException("Rail is null!");
        blockToRails.put(railBlock, rail);
    }

    private void registerCustomRailMapper(IRailMapper rail){
        if(rail == null) throw new NullPointerException("Rail Mapper is null!");
        railMappers.add(rail);
    }

    public IRail getRail(World world, BlockPos pos, IBlockState state){
        IRail rail = blockToRails.get(state.getBlock());
        if(rail != null) return rail;
        for(IRailMapper mapper : railMappers) {
            rail = mapper.getRail(world, pos, state);
            if(rail != null) return rail;
        }
        return null;
    }
}
