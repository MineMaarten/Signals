package com.minemaarten.signals.rail.network.mc;

import io.netty.buffer.ByteBuf;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import com.google.common.collect.ImmutableList;
import com.minemaarten.signals.lib.EnumSetUtils;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.mc.NetworkSerializer.EnumNetworkObject;

public class MCNetworkRail extends NetworkRail<MCPos> implements ISerializableNetworkObject{

    private static final Object NORMAL_RAIL_TYPE = new Object();
    private static final String NORMAL_RAIL_STRING = "r";
    private static final EnumSet<EnumHeading> STANDARD_NEIGHBOR_HEADINGS = EnumSet.allOf(EnumHeading.class);
    private static final EnumRailDirection[] ALL_RAIL_DIRECTIONS_ARRAY = EnumRailDirection.values();
    private static final EnumSet<EnumRailDirection> ALL_RAIL_DIRECTIONS = EnumSet.allOf(EnumRailDirection.class);

    private final String railType; //The type of rail, usually the block registry name, but null for Blocks.RAIL to save memory for serialization
    private final EnumRailDirection curDir; //Used client-side for rendering rail sections.
    private final ImmutableList<MCPos> potentialRailNeighbors, potentialObjectNeighbors;
    private final EnumSet<EnumRailDirection> validRailDirs;
    private final EnumSet<EnumHeading> validNeighborHeadings;
    private final EnumMap<EnumHeading, Set<MCPos>> entryDirToPotentialRailNeighbors;

    public MCNetworkRail(MCPos pos, Block railBlock, EnumRailDirection curDir, EnumSet<EnumRailDirection> validRailDirs){
        this(pos, railBlock == Blocks.RAIL ? null : railBlock.getRegistryName().toString(), curDir, validRailDirs);
    }

    public MCNetworkRail(MCPos pos, String railType, EnumRailDirection curDir, EnumSet<EnumRailDirection> validRailDirs){
        super(pos);
        this.railType = railType;
        this.curDir = curDir;

        //If standard rail, use mem cache
        if(validRailDirs.size() == ALL_RAIL_DIRECTIONS.size()) {
            this.validRailDirs = ALL_RAIL_DIRECTIONS;
            this.validNeighborHeadings = STANDARD_NEIGHBOR_HEADINGS;

        } else { //Else, compute
            this.validRailDirs = validRailDirs;
            this.validNeighborHeadings = getValidHeadings(validRailDirs);

        }
        potentialObjectNeighbors = computePotentialObjectNeighbors();
        potentialRailNeighbors = ImmutableList.copyOf(potentialObjectNeighbors.stream().flatMap(this::plusOneMinusOneHeight).collect(Collectors.toList()));
        entryDirToPotentialRailNeighbors = computeExitsForEntries(validRailDirs);
    }

    public static MCNetworkRail fromTag(NBTTagCompound tag){
        EnumRailDirection curDir = ALL_RAIL_DIRECTIONS_ARRAY[tag.getByte("c")];
        if(tag.hasKey("t")) {
            EnumSet<EnumRailDirection> validRailDirs = EnumSetUtils.toEnumSet(EnumRailDirection.class, ALL_RAIL_DIRECTIONS_ARRAY, tag.getShort("r"));
            return new MCNetworkRail(new MCPos(tag), tag.getString("t"), curDir, validRailDirs);
        } else {
            return new MCNetworkRail(new MCPos(tag), (String)null, curDir, ALL_RAIL_DIRECTIONS);
        }
    }

    public static MCNetworkRail fromByteBuf(ByteBuf b){
        MCPos pos = new MCPos(b);
        EnumRailDirection curDir = ALL_RAIL_DIRECTIONS_ARRAY[b.readByte()];
        String type = ByteBufUtils.readUTF8String(b);
        if(type.equals(NORMAL_RAIL_STRING)) {
            return new MCNetworkRail(pos, (String)null, curDir, ALL_RAIL_DIRECTIONS);
        } else {
            EnumSet<EnumRailDirection> validRailDirs = EnumSetUtils.toEnumSet(EnumRailDirection.class, ALL_RAIL_DIRECTIONS_ARRAY, b.readShort());
            return new MCNetworkRail(pos, type, curDir, validRailDirs);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        pos.writeToNBT(tag);
        tag.setByte("c", (byte)curDir.ordinal());
        if(railType != null) {
            tag.setString("t", railType);
            tag.setShort("r", EnumSetUtils.toShort(validRailDirs));
        }
    }

    @Override
    public void writeToBuf(ByteBuf b){
        pos.writeToBuf(b);
        b.writeByte(curDir.ordinal());
        ByteBufUtils.writeUTF8String(b, railType == null ? NORMAL_RAIL_STRING : railType);
        if(railType != null) {
            b.writeShort(EnumSetUtils.toShort(validRailDirs));
        }
    }

    private Stream<MCPos> plusOneMinusOneHeight(MCPos pos){
        return Stream.of(pos.offset(EnumFacing.UP), pos, pos.offset(EnumFacing.DOWN));
    }

    private ImmutableList<MCPos> computePotentialObjectNeighbors(){
        return ImmutableList.copyOf(validNeighborHeadings.stream().map(pos::offset).collect(Collectors.toList()));
    }

    @Override
    public Object getRailType(){
        return railType == null ? NORMAL_RAIL_TYPE : railType;
    }

    public EnumRailDirection getCurDir(){
        return curDir;
    }

    @Override
    public List<MCPos> getPotentialNeighborRailLocations(){
        return potentialRailNeighbors;
    }

    @Override
    public EnumSet<EnumHeading> getPotentialNeighborRailHeadings(){
        return validNeighborHeadings;
    }

    @Override
    public List<MCPos> getPotentialNeighborObjectLocations(){
        return potentialObjectNeighbors;
    }

    @Override
    public Collection<MCPos> getPotentialPathfindNeighbors(EnumHeading entryDir){
        return entryDirToPotentialRailNeighbors.get(entryDir);
    }

    private EnumMap<EnumHeading, Set<MCPos>> computeExitsForEntries(EnumSet<EnumRailDirection> validRailDirs){
        EnumMap<EnumHeading, Set<MCPos>> exitsForEntries = new EnumMap<>(EnumHeading.class);
        for(EnumHeading heading : EnumHeading.VALUES) {
            exitsForEntries.put(heading, new HashSet<>(12));
        }

        for(EnumRailDirection railDir : validRailDirs) {
            EnumSet<EnumHeading> railDirDirs = getDirections(railDir);
            Set<MCPos> dirPositions = railDirDirs.stream().map(pos::offset).flatMap(this::plusOneMinusOneHeight).collect(Collectors.toSet());
            for(EnumHeading heading : railDirDirs) {
                exitsForEntries.get(heading.getOpposite()).addAll(dirPositions);
            }
        }

        return exitsForEntries;
    }

    private static EnumSet<EnumHeading> getValidHeadings(EnumSet<EnumRailDirection> validRailDirs){
        EnumSet<EnumHeading> headings = EnumSet.noneOf(EnumHeading.class);
        for(EnumRailDirection dir : validRailDirs) {
            headings.addAll(getDirections(dir));
        }
        return headings;
    }

    private static EnumSet<EnumHeading> getDirections(EnumRailDirection railDir){
        switch(railDir){
            case NORTH_SOUTH:
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
                return EnumSet.of(EnumHeading.NORTH, EnumHeading.SOUTH);
            case EAST_WEST:
            case ASCENDING_EAST:
            case ASCENDING_WEST:
                return EnumSet.of(EnumHeading.EAST, EnumHeading.WEST);
            case SOUTH_EAST:
                return EnumSet.of(EnumHeading.SOUTH, EnumHeading.EAST);
            case SOUTH_WEST:
                return EnumSet.of(EnumHeading.SOUTH, EnumHeading.WEST);
            case NORTH_WEST:
                return EnumSet.of(EnumHeading.NORTH, EnumHeading.WEST);
            case NORTH_EAST:
                return EnumSet.of(EnumHeading.NORTH, EnumHeading.EAST);
            default:
                return EnumSet.noneOf(EnumHeading.class);
        }
    }

    @Override
    public EnumNetworkObject getType(){
        return EnumNetworkObject.RAIL;
    }
}
