package me.d4rk.fracassadobot;

import me.d4rk.fracassadobot.handlers.DataHandler;
import me.d4rk.fracassadobot.handlers.economy.EconomyThread;
import me.d4rk.fracassadobot.listeners.GuildMessageListener;
import me.d4rk.fracassadobot.listeners.GuildReactionListener;
import me.d4rk.fracassadobot.utils.Config;
import me.d4rk.fracassadobot.utils.command.CommandRegistry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;


public class Bot {

    public static JDA jda;

    public static void main(String[] args) {

        Config.loadConfig();
        DataHandler.connect("localhost", 28015);
        CommandRegistry.registerCmds();

        try {
            jda = new JDABuilder()
                    .setToken(Config.token)
                    .addEventListeners(new GuildMessageListener(), new GuildReactionListener())
                    .build();
        }catch (Exception e){
            e.printStackTrace();
        }

        EconomyThread.start();

        if(Config.isStreaming) {
            jda.getPresence().setPresence(Activity.streaming(Config.defaultPlaying, "https://twitch.tv/"), false);
        }else {
            jda.getPresence().setPresence(Activity.playing(Config.defaultPlaying), false);
        }


    }

}
