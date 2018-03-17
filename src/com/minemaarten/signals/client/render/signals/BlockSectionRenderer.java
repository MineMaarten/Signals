package com.minemaarten.signals.client.render.signals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemDye;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.client.RectRenderer;
import com.minemaarten.signals.lib.HeadingUtils;
import com.minemaarten.signals.lib.Vec3iUtils;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailObjectHolder;
import com.minemaarten.signals.rail.network.RailSection;
import com.minemaarten.signals.rail.network.mc.MCNetworkRail;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class BlockSectionRenderer{

    private Map<RailSection<MCPos>, SectionRenderer> sectionsToRenderer = new HashMap<>();

    private SectionRenderer getSectionRenderer(RailSection<MCPos> section){
        SectionRenderer renderer = sectionsToRenderer.get(section);
        if(renderer == null) {
            renderer = new SectionRenderer(section);

            Set<Integer> invalidColors = getAdjacentBlockSections(section).map(x -> x.colorIndex).collect(Collectors.toSet());
            int availableColors = 16 - invalidColors.size();
            if(availableColors > 0) { //If there are colors left (it would be very exceptional if there weren't.
                int usedIndex = Math.abs(section.iterator().next().hashCode()) % availableColors; //Use a deterministic way to generate a color index.
                for(int i = 0; i < 16; i++) {
                    if(!invalidColors.contains(i)) {
                        if(usedIndex-- <= 0) {
                            renderer.colorIndex = i;
                            break;
                        }
                    }
                }
            }

            renderer.compileRender();

            sectionsToRenderer.put(section, renderer);
        }
        return renderer;
    }

    private Stream<SectionRenderer> getAdjacentBlockSections(RailSection<MCPos> section){
        return sectionsToRenderer.entrySet().stream().filter(e -> e.getKey().isAdjacent(section)).map(e -> e.getValue());
    }

    public void updateSectionRenderers(){
        Iterable<RailSection<MCPos>> allSections = RailNetworkManager.getInstance().getNetwork().getAllSections();
        sectionsToRenderer.clear();

        for(RailSection<MCPos> section : allSections) {
            getSectionRenderer(section);
        }
    }

    public void render(BufferBuilder b){

        /*b.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for(TileEntity te : tes) {
            if(te instanceof TileEntitySignalBase) {
                TileEntitySignalBase teSignal = (TileEntitySignalBase)te;
             TODO   renderSignalDirection(b, teSignal);
            }
        }
        Tessellator.getInstance().draw();*/

        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for(SectionRenderer blockSection : sectionsToRenderer.values()) {
            blockSection.rectRenderer.render(b);
        }
        Tessellator.getInstance().draw();
    }

    private static int getRailHeightOffset(NetworkRail<MCPos> rail, EnumFacing dir){
        switch(((MCNetworkRail)rail).getCurDir()){
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

    /*private void renderSignalDirection(BufferBuilder buffer, TileEntitySignalBase signal){
        EnumFacing signalFacing = signal.getFacing().getOpposite();
        SignalBlockNode rootNode = null;//TODO signal.getSignalBlockInfo();
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
    }*/

    private static class SectionRenderer{

        public int colorIndex;
        public RectRenderer rectRenderer;
        private final RailSection<MCPos> section;

        public SectionRenderer(RailSection<MCPos> section){
            this.section = section;
        }

        public void compileRender(){
            rectRenderer = new RectRenderer();

            int color = ItemDye.DYE_COLORS[colorIndex];
            float r = (color >> 16) / 256F;
            float g = (color >> 8 & 255) / 256F;
            float b = (color & 255) / 256F;
            rectRenderer.setColor(r, g, b);

            NetworkRail<MCPos> rootNode = section.iterator().next();
            Set<NetworkRail<MCPos>> traversed = new HashSet<>();
            traversed.add(rootNode);

            Stack<NetworkRail<MCPos>> toTraverse = new Stack<>();
            RailObjectHolder<MCPos> networkObjs = RailNetworkManager.getInstance().getNetwork().railObjects;

            toTraverse.push(rootNode);

            while(!toTraverse.isEmpty()) {
                NetworkRail<MCPos> node = toTraverse.pop();
                EnumRailDirection railDir = ((MCNetworkRail)node).getCurDir();

                List<NetworkRail<MCPos>> neighbors = node.getSectionNeighborRails(networkObjs).collect(Collectors.toList());
                for(NetworkRail<MCPos> neighbor : neighbors) {
                    rectRenderer.pos(node.pos.getX() + 0.5, node.pos.getY() + (railDir.isAscending() ? 0.6 : 0.1), node.pos.getZ() + 0.5);

                    EnumFacing dir = HeadingUtils.toFacing(neighbor.pos.getRelativeHeading(node.pos));
                    int offset = getRailHeightOffset(node, dir);
                    //                    boolean isHigherNeighbor = neighbor.pos.getY() > node.pos.getY();
                    Vec3d interpolated = Vec3iUtils.interpolate(node.pos.getPos(), neighbor.pos.getPos());
                    rectRenderer.pos(interpolated.x + 0.5, node.pos.getY() + (offset == 1 ? 1.1 : 0.1), interpolated.z + 0.5);

                    /* Vec3d interpolated = Vec3iUtils.interpolate(node.pos.getPos(), neighbor.pos.getPos());
                     EnumFacing dir = HeadingUtils.toFacing(neighbor.pos.getRelativeHeading(node.pos));

                     if(dir != null) { //When adjacent
                         boolean neighborsNeighborIsHigher = getRailHeightOffset(neighbor, dir) == 1;

                         rectRenderer.pos(node.pos.getX() + 0.5, node.pos.getY() + (isHigherNeighbor || goingDown ? 0.6 : 0.1), node.pos.getZ() + 0.5);
                         rectRenderer.pos(interpolated.x + 0.5, node.pos.getY() + (isHigherNeighbor ? 1.1 : 0.1), interpolated.z + 0.5);
                         rectRenderer.pos(interpolated.x + 0.5, node.pos.getY() + (isHigherNeighbor ? 1.1 : 0.1), interpolated.z + 0.5);
                         rectRenderer.pos(neighbor.pos.getX() + 0.5, neighbor.pos.getY() + (isLowerNeighbor || neighborsNeighborIsHigher ? 0.6 : 0.1), neighbor.pos.getZ() + 0.5);
                     } else { //When not adjacent (Rail Link, for example)
                         isLowerNeighbor = false;
                     }*/
                    if(section.containsRail(neighbor.pos) && traversed.add(neighbor)) {
                        toTraverse.push(neighbor);
                    }
                }
            }
        }
    }
}
