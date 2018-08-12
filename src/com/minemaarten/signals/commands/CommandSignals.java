package com.minemaarten.signals.commands;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

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
        } else {
            throw new WrongUsageException("command.signals.invalidSubCommand", subCommand);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos){
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "rebuildNetwork") : super.getTabCompletions(server, sender, args, targetPos);
    }
}
