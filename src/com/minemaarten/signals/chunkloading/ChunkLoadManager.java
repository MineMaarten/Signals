package com.minemaarten.signals.chunkloading;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.PlayerOrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.ListMultimap;
import com.minemaarten.signals.Signals;
import com.minemaarten.signals.lib.Log;

public class ChunkLoadManager{
    public static ChunkLoadManager INSTANCE = new ChunkLoadManager();

    public Map<Entity, ChunkLoadEntry> entries = new HashMap<>();

    public void init(){
        //Re-register chunks
        ForgeChunkManager.setForcedChunkLoadingCallback(Signals.instance, new PlayerOrderedLoadingCallback(){
            /**
             * Load all forced chunks
             */
            @Override
            public void ticketsLoaded(List<Ticket> tickets, World world){
                tickets.forEach(ticket -> add(ticket));
            }

            /**
             * Re-register all previous tickets
             * @param tickets
             * @param world
             * @return
             */
            @Override
            public ListMultimap<String, Ticket> playerTicketsLoaded(ListMultimap<String, Ticket> tickets, World world){
                return tickets;
            }
        });
    }

    private void add(Ticket ticket){
        Validate.notNull(ticket);
        ChunkLoadEntry oldEntry = entries.put(ticket.getEntity(), new ChunkLoadEntry(ticket));
        if(oldEntry != null) {
            oldEntry.dispose();
        }
    }

    public void update(){
        entries.values().removeIf(ChunkLoadEntry::update);
    }

    public boolean markAsChunkLoader(EntityPlayer associatedPlayer, EntityMinecart cart){
        return markAsChunkLoader(associatedPlayer.getGameProfile().getId().toString(), cart);
    }

    public boolean markAsChunkLoader(String chunkloadingPlayer, EntityMinecart cart){
        Ticket ticket = ForgeChunkManager.requestPlayerTicket(Signals.instance, chunkloadingPlayer, cart.world, Type.ENTITY);
        if(ticket != null) {
            ticket.bindEntity(cart);
            add(ticket);
            return true;
        } else {
            return false;
        }
    }

    public void unmarkAsChunkLoader(EntityMinecart cart){
        ChunkLoadEntry entry = entries.remove(cart);
        if(entry != null) {
            entry.dispose();
        }
    }

    public static class ChunkLoadEntry{
        private final Ticket ticket;
        private ChunkPos curPos;
        private Set<ChunkPos> curForced = new HashSet<>();

        public ChunkLoadEntry(Ticket ticket){
            this.ticket = ticket;
            if(update()) {
                Log.warning("Chunkloading cart dead on init!");
            } else {
                Log.debug("Loaded chunkloader for cart at " + (ticket.getEntity() == null ? "[No entity]" : ticket.getEntity().getPosition().toString()));
            }
        }

        public boolean update(){
            if(ticket.getEntity() == null || ticket.getEntity().isDead) {
                Log.warning("No or dead cart for chunkloading ticket found! Disposing...");
                dispose();
                return true;
            }

            ChunkPos newPos = new ChunkPos(ticket.getEntity().chunkCoordX, ticket.getEntity().chunkCoordZ);
            if(!newPos.equals(curPos)) { // When the cart changed from chunk

                //Select a 3x3 area centered around the cart.
                Set<ChunkPos> newForced = new HashSet<>();
                for(int x = newPos.x - 1; x <= newPos.x + 1; x++) {
                    for(int z = newPos.z - 1; z <= newPos.z + 1; z++) {
                        newForced.add(new ChunkPos(x, z));
                    }
                }

                //Unforce chunks we don't need anymore
                curForced.stream().filter(pos -> !newForced.contains(pos)).forEach(pos -> {
                    ForgeChunkManager.unforceChunk(ticket, pos);
                });

                //Force new chunks
                newForced.stream().filter(pos -> !curForced.contains(pos)).forEach(pos -> {
                    ForgeChunkManager.forceChunk(ticket, pos);
                });

                curForced = newForced;
            }
            return false;
        }

        public void dispose(){
            ForgeChunkManager.releaseTicket(ticket);
        }
    }
}
