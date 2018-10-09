package com.minemaarten.signals.commands;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class CommandSignals extends CommandBase{

    @Override
    public String getName(){
        return "signals";
    }

    @Override
    public String getUsage(ICommandSender sender){
        return "signals <action> <arguments>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException{
        if(args.length == 0) throw new WrongUsageException("command.signals.noArgs");
        String subCommand = args[0];
        if(subCommand.equals("rebuildNetwork")) {
            RailNetworkManager.getServerInstance().rebuildNetwork();
            sender.sendMessage(new TextComponentTranslation("command.signals.networkCleared"));
        } else if(subCommand.equals("debug") && sender.getName().startsWith("Player" /* Playerx */)) {
            if(debug(server, sender, args)) {
                sender.sendMessage(new TextComponentString("DEBUG EXECUTED"));
            } else {
                sender.sendMessage(new TextComponentString("Could not execute debug!"));
            }
        } else {
            throw new WrongUsageException("command.signals.invalidSubCommand", subCommand);
        }
    }

    private boolean debug(MinecraftServer server, ICommandSender sender, String[] args){
        World overworld = server.getWorld(0);
        for(int i = 0; i < 10000; i++) {
            EntityMinecartEmpty cart = new EntityMinecartEmpty(overworld, i, 64, Integer.parseInt(args[1]));
            cart.forceSpawn = true;
            boolean success = overworld.spawnEntity(cart);
            if(i % 1000 == 0) {
                sender.sendMessage(new TextComponentString("Spawned " + i + " / 100.000"));
            }
        }
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos){
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "rebuildNetwork") : super.getTabCompletions(server, sender, args, targetPos);
    }
}
