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
import net.minecraft.item.ItemDye;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

import com.minemaarten.signals.client.RectRenderer;
import com.minemaarten.signals.lib.HeadingUtils;
import com.minemaarten.signals.lib.Vec3iUtils;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailObjectHolder;
import com.minemaarten.signals.rail.network.mc.MCNetworkRail;
import com.minemaarten.signals.rail.network.mc.MCPos;

public abstract class AbstractRailRenderer<TSection> {

    protected final Map<TSection, SectionRenderer> sectionsToRenderer = new HashMap<>();

    private void addSectionRenderer(TSection section){
        SectionRenderer renderer = new SectionRenderer(section);

        Set<Integer> invalidColors = getAdjacentSections(section).map(x -> x.colorIndex).collect(Collectors.toSet());
        int availableColors = 16 - invalidColors.size();
        if(availableColors > 0) { //If there are colors left (it would be very exceptional if there weren't.
            int usedIndex = Math.abs(section.hashCode()) % availableColors; //Use a deterministic way to generate a color index.
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

    private Stream<SectionRenderer> getAdjacentSections(TSection edge){
        return sectionsToRenderer.entrySet().stream().filter(e -> isAdjacent(e.getKey(), edge)).map(e -> e.getValue());
    }

    public void updateSectionRenderers(){
        sectionsToRenderer.clear();

        for(TSection edge : getRenderableSections()) {
            addSectionRenderer(edge);
        }
    }

    public void updateSpecificSection(TSection section){
        sectionsToRenderer.remove(section);
        if(canRender(section)) addSectionRenderer(section);
    }

    protected abstract boolean isAdjacent(TSection s1, TSection s2);

    protected abstract Iterable<TSection> getRenderableSections();

    protected boolean canRender(TSection section){
        return true;
    }

    protected abstract NetworkRail<MCPos> getRootNode(TSection section);

    protected abstract RailObjectHolder<MCPos> getNeighborProvider(TSection section);

    protected abstract boolean shouldTraverse(TSection section, NetworkRail<MCPos> rail);

    public void render(BufferBuilder b){
        for(SectionRenderer edgeRenderer : sectionsToRenderer.values()) {
            edgeRenderer.rectRenderer.render(b);
        }
    }

    public static int getRailHeightOffset(NetworkRail<MCPos> rail, EnumFacing dir){
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

    public double getLineWidth(){
        return 0.075;
    }

    public double getHeightOffset(){
        return 0;
    }

    private class SectionRenderer{

        public int colorIndex;
        public RectRenderer rectRenderer;
        private final TSection section;

        public SectionRenderer(TSection section){
            this.section = section;
        }

        public void compileRender(){
            rectRenderer = new RectRenderer();
            rectRenderer.width = getLineWidth();

            int color = ItemDye.DYE_COLORS[colorIndex];
            float r = (color >> 16) / 256F;
            float g = (color >> 8 & 255) / 256F;
            float b = (color & 255) / 256F;
            rectRenderer.setColor(r, g, b);

            NetworkRail<MCPos> rootNode = getRootNode(section);
            Set<NetworkRail<MCPos>> traversed = new HashSet<>();
            traversed.add(rootNode);

            Stack<NetworkRail<MCPos>> toTraverse = new Stack<>();

            toTraverse.push(rootNode);

            RailObjectHolder<MCPos> neighborProvider = getNeighborProvider(section);

            while(!toTraverse.isEmpty()) {
                NetworkRail<MCPos> node = toTraverse.pop();
                EnumRailDirection railDir = ((MCNetworkRail)node).getCurDir();

                List<NetworkRail<MCPos>> neighbors = node.getSectionNeighborRails(neighborProvider).collect(Collectors.toList());
                for(NetworkRail<MCPos> neighbor : neighbors) {
                    rectRenderer.pos(node.pos.getX() + 0.5, node.pos.getY() + (railDir.isAscending() ? 0.6 : 0.1) + getHeightOffset(), node.pos.getZ() + 0.5);

                    EnumFacing dir = HeadingUtils.toFacing(neighbor.pos.getRelativeHeading(node.pos));
                    int offset = getRailHeightOffset(node, dir);
                    Vec3d interpolated = Vec3iUtils.interpolate(node.pos.getPos(), neighbor.pos.getPos());
                    rectRenderer.pos(interpolated.x + 0.5, node.pos.getY() + (offset == 1 ? 1.1 : 0.1) + getHeightOffset(), interpolated.z + 0.5);

                    if(shouldTraverse(section, neighbor) && traversed.add(neighbor)) {
                        toTraverse.push(neighbor);
                    }
                }
            }
        }
    }
}
