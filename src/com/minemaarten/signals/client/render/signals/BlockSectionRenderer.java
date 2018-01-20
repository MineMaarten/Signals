package com.minemaarten.signals.client.render.signals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemDye;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.client.RectRenderer;
import com.minemaarten.signals.lib.SignalBlockNode;
import com.minemaarten.signals.lib.SignalBlockSection;
import com.minemaarten.signals.lib.SignalsUtils;
import com.minemaarten.signals.lib.Vec3iUtils;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class BlockSectionRenderer{

    private Map<TileEntitySignalBase, SignalBlockSectionColored> signalsToColors = new HashMap<>();
    private int refreshCounter = 0;

    private SignalBlockSectionColored getBlockSection(TileEntitySignalBase te){
        SignalBlockSectionColored blockSection = signalsToColors.get(te);
        if(blockSection == null) {
            SignalBlockSectionColored newSection = new SignalBlockSectionColored(te.getSignalBlockInfo());
            Optional<SignalBlockSectionColored> matchingSection = signalsToColors.values().stream().filter(x -> x.equals(newSection)).findFirst();
            if(matchingSection.isPresent()) {
                blockSection = matchingSection.get();
            } else {
                blockSection = newSection;

                Set<Integer> invalidColors = getAdjacentBlockSections(blockSection).map(x -> x.colorIndex).collect(Collectors.toSet());
                int availableColors = 16 - invalidColors.size();
                if(availableColors > 0) { //If there are colors left (it would be very exceptional if there weren't.
                    int usedIndex = blockSection.getRootNode().hashCode() % availableColors; //Use a deterministic way to generate a color index.
                    for(int i = 0; i < 16; i++) {
                        if(!invalidColors.contains(i)) {
                            if(usedIndex-- <= 0) {
                                blockSection.colorIndex = i;
                                break;
                            }
                        }
                    }
                }

                blockSection.compileRender();
            }

            signalsToColors.put(te, blockSection);
        }
        return blockSection;
    }

    private Stream<SignalBlockSectionColored> getAdjacentBlockSections(SignalBlockSectionColored blockSection){
        return signalsToColors.values().stream().filter(otherSection -> otherSection.isAdjacent(blockSection));
    }

    public void render(BufferBuilder b, List<TileEntity> tes){
        if(refreshCounter++ >= 100) {
            refreshCounter = 0;
            signalsToColors.clear();

            for(TileEntity te : tes) {
                if(te instanceof TileEntitySignalBase) {
                    TileEntitySignalBase teSignal = (TileEntitySignalBase)te;
                    getBlockSection(teSignal);
                }
            }
        }

        b.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for(TileEntity te : tes) {
            if(te instanceof TileEntitySignalBase) {
                TileEntitySignalBase teSignal = (TileEntitySignalBase)te;
                renderSignalDirection(b, teSignal);
            }
        }
        Tessellator.getInstance().draw();

        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for(SignalBlockSectionColored blockSection : signalsToColors.values()) {
            blockSection.rectRenderer.render(b);
        }
        Tessellator.getInstance().draw();
    }

    private int getRailHeightOffset(EnumRailDirection railDir, EnumFacing dir){
        switch(railDir){
            case ASCENDING_EAST:
                return dir == EnumFacing.EAST ? 1 : (dir == EnumFacing.WEST ? -1 : 0);
            case ASCENDING_NORTH:
                return dir == EnumFacing.NORTH ? 1 : (dir == EnumFacing.SOUTH ? -1 : 0);
            case ASCENDING_SOUTH:
                return dir == EnumFacing.SOUTH ? 1 : (dir == EnumFacing.NORTH ? -1 : 0);
            case ASCENDING_WEST:
                return dir == EnumFacing.WEST ? 1 : (dir == EnumFacing.EAST ? -1 : 0);
            default:
                return 0;
        }
    }

    private void renderSignalDirection(BufferBuilder buffer, TileEntitySignalBase signal){
        EnumFacing signalFacing = signal.getFacing().getOpposite();
        SignalBlockNode rootNode = signal.getSignalBlockInfo();
        int heightOffset = getRailHeightOffset(rootNode.railDir, signalFacing);

        EnumFacing rotatedFacing = signalFacing.rotateY();
        EnumFacing rotatedFacing2 = signalFacing.rotateYCCW();

        int colorIndex = getBlockSection(signal).colorIndex;
        int color = ItemDye.DYE_COLORS[colorIndex];
        float r = (color >> 16) / 256F;
        float g = (color >> 8 & 255) / 256F;
        float b = (color & 255) / 256F;

        BlockPos pos = signal.getPos().offset(rotatedFacing);
        Vec3d posVec = new Vec3d(pos.getX() + 0.5, pos.getY() + (heightOffset != 0 ? 0.6 : 0.1), pos.getZ() + 0.5);

        double yOffset = heightOffset * 1;

        double arrowSize = 0.2;
        double spacing = 0.2;
        for(int i = -2; i < 0; i++) {
            Vec3d shiftedPosVec = posVec.addVector(signalFacing.getFrontOffsetX() * spacing * i, spacing * i * yOffset, signalFacing.getFrontOffsetZ() * spacing * i);
            Vec3d vecBack = shiftedPosVec.addVector(signalFacing.getFrontOffsetX() * arrowSize, arrowSize * yOffset, signalFacing.getFrontOffsetZ() * arrowSize);
            Vec3d c1 = vecBack.addVector(rotatedFacing.getFrontOffsetX() * arrowSize, 0, rotatedFacing.getFrontOffsetZ() * arrowSize);
            Vec3d c2 = vecBack.addVector(rotatedFacing2.getFrontOffsetX() * arrowSize, 0, rotatedFacing2.getFrontOffsetZ() * arrowSize);

            buffer.pos(shiftedPosVec.x, shiftedPosVec.y, shiftedPosVec.z).color(r, g, b, 1).endVertex();
            buffer.pos(c1.x, c1.y, c1.z).color(r, g, b, 1).endVertex();
            //buffer.pos(shiftedPosVec.x, shiftedPosVec.y, shiftedPosVec.z).color(r, g, b, 1).endVertex();
            buffer.pos(c2.x, c2.y, c2.z).color(r, g, b, 1).endVertex();
        }
    }

    private void renderSignalBlocks(RectRenderer rectRenderer, SignalBlockNode node, boolean goingDown){
        for(SignalBlockNode neighbor : node.nextNeighbors) {
            boolean isLowerNeighbor = neighbor.railPos.getY() < node.railPos.getY();
            boolean isHigherNeighbor = neighbor.railPos.getY() > node.railPos.getY();

            Vec3d interpolated = Vec3iUtils.interpolate(node.railPos, neighbor.railPos);
            EnumFacing dir = SignalsUtils.getRelativeHorizonal(node.railPos, neighbor.railPos);

            if(dir != null) { //When adjacent
                boolean neighborsNeighborIsHigher = getRailHeightOffset(neighbor.railDir, dir) == 1;

                rectRenderer.pos(node.railPos.getX() + 0.5, node.railPos.getY() + (isHigherNeighbor || goingDown ? 0.6 : 0.1), node.railPos.getZ() + 0.5);
                rectRenderer.pos(interpolated.x + 0.5, node.railPos.getY() + (isHigherNeighbor ? 1.1 : 0.1), interpolated.z + 0.5);
                rectRenderer.pos(interpolated.x + 0.5, node.railPos.getY() + (isHigherNeighbor ? 1.1 : 0.1), interpolated.z + 0.5);
                rectRenderer.pos(neighbor.railPos.getX() + 0.5, neighbor.railPos.getY() + (isLowerNeighbor || neighborsNeighborIsHigher ? 0.6 : 0.1), neighbor.railPos.getZ() + 0.5);
            } else { //When not adjacent (Rail Link, for example)
                isLowerNeighbor = false;
            }

            renderSignalBlocks(rectRenderer, neighbor, isLowerNeighbor);
        }
    }

    private class SignalBlockSectionColored extends SignalBlockSection{

        public int colorIndex;
        public RectRenderer rectRenderer;

        public SignalBlockSectionColored(SignalBlockNode rootNode){
            super(rootNode);
        }

        public void compileRender(){
            rectRenderer = new RectRenderer();

            int color = ItemDye.DYE_COLORS[colorIndex];
            float r = (color >> 16) / 256F;
            float g = (color >> 8 & 255) / 256F;
            float b = (color & 255) / 256F;
            rectRenderer.setColor(r, g, b);
            SignalBlockNode rootNode = getRootNode();
            boolean goingDown = false;

            //Determine going down for the first node
            if(!rootNode.nextNeighbors.isEmpty()) {
                SignalBlockNode neighbor = rootNode.nextNeighbors.get(0); //The first node can only have one neighbor.
                EnumFacing relDir = SignalsUtils.getRelativeHorizonal(rootNode.railPos, neighbor.railPos);
                goingDown = getRailHeightOffset(rootNode.railDir, relDir) == -1;
            }

            renderSignalBlocks(rectRenderer, rootNode, goingDown);
        }
    }
}
