package me.d4rk.fracassadobot.core.economy;

import javafx.util.Pair;
import me.d4rk.fracassadobot.Bot;
import me.d4rk.fracassadobot.core.DataHandler;
import me.d4rk.fracassadobot.utils.RandomUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

import java.util.*;

public class EconomyThread {

    private static HashMap<String, HashMap<String, List<HashMap<String, Object>>>> globalEffects, globalCooldowns;

    public static void start() {

        Logger log = JDALogger.getLog("EconomyThread");
        log.info("Starting thread!");


        new Thread(() -> {
            log.info("Started!");
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    globalEffects = new HashMap<>();
                    globalCooldowns = new HashMap<>();
                    for(Guild guild : Bot.jda.getGuilds()) {
                        HashMap map = DataHandler.database.table("guildEconomy").get(guild.getId()).run(DataHandler.conn);
                        if(map != null) {
                            HashMap<String, List<HashMap<String, Object>>> guildEffects = new HashMap<>(), guildCooldowns = new HashMap<>();
                            for(Object key : map.keySet()) {
                                if(map.get(key) instanceof HashMap) {
                                    HashMap fMap = ((HashMap) map.get(key));
                                    for(Object keyy : fMap.keySet()) {
                                        if(keyy.equals("effects")) {
                                            List<HashMap<String, Object>> userEffects = new ArrayList<>();
                                            for(HashMap<String, Object> hMap : (List<HashMap<String, Object>>) fMap.get("effects")) {
                                                if(System.currentTimeMillis() < (Long) hMap.get("value"))
                                                    userEffects.add(hMap);
                                                else {
                                                    log.info("Removing " + hMap.get("key") + " effect from user: " + key);
                                                    //Aqui checa se o efeito que está removendo é o mute! Caso sim, remove o cargo de mute
                                                    if(hMap.get("key").equals("DEBUFF_MUTE5M") || hMap.get("key").equals("DEBUFF_MUTE10M")) {
                                                        Member member = guild.getMemberById(key.toString());
                                                        Role mute = RandomUtils.getMuteRole(guild);
                                                        if(member != null && mute != null)
                                                            guild.removeRoleFromMember(member, mute).queue();
                                                    }else if(hMap.get("key").equals("OWNROLE")) {
                                                        Member member = guild.getMemberById(key.toString());
                                                        Role own = guild.getRoleById(hMap.get("roleId").toString());
                                                        if(member != null && own != null)
                                                            own.delete().queue();
                                                    }
                                                }
                                            }
                                            DataHandler.database.table("guildEconomy").get(guild.getId()).update(
                                                    DataHandler.r.hashMap(key, DataHandler.r.hashMap("effects", userEffects))
                                            ).run(DataHandler.conn);
                                            guildEffects.put(key.toString(), userEffects);
                                        }else if(keyy.equals("cooldown")){
                                            List<HashMap<String, Object>> userCooldown = new ArrayList<>();
                                            for(HashMap<String, Object> hMap : (List<HashMap<String, Object>>) fMap.get("cooldown")) {
                                                if(System.currentTimeMillis() < (Long) hMap.get("value"))
                                                    userCooldown.add(hMap);
                                                else {
                                                    log.info("Removing " + hMap.get("key") + " cooldown from user: " + key);
                                                }
                                            }
                                            DataHandler.database.table("guildEconomy").get(guild.getId()).update(
                                                    DataHandler.r.hashMap(key, DataHandler.r.hashMap("cooldown", userCooldown))
                                            ).run(DataHandler.conn);
                                            guildCooldowns.put(key.toString(), userCooldown);
                                        }else if(keyy.equals("lastDaily")) {
                                            if(System.currentTimeMillis() >= (Long) fMap.get("lastDaily")+86400000) {
                                                if(((List<String>) fMap.get("inventory")).contains("AUTODAILY_ON")) {
                                                    EconomySystemHandler.updateDaily(guild.getId(), key.toString(), true);
                                                    log.info("Automatically collecting daily bonus for: "+key);
                                                }else if(((List<String>) fMap.get("inventory")).contains("AUTODAILY")) {
                                                    EconomySystemHandler.useItem(guild.getId(), key.toString(), EconomyItem.AUTODAILY);
                                                    EconomySystemHandler.updateDaily(guild.getId(), key.toString(), true);
                                                    log.info("Automatically collecting daily bonus for: "+key);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            globalEffects.put(guild.getId(), guildEffects);
                            globalCooldowns.put(guild.getId(), guildCooldowns);
                        }
                    }
                }
            };
            timer.schedule(task, 0, 60000);
        }).start();

    }

    public static List<Pair<String, Long>> getCachedEffects(String guildId, String userId) {
        if(globalEffects.get(guildId) != null && globalEffects.get(guildId).get(userId) != null) {
            List<Pair<String, Long>> userEffects = new ArrayList<>();
            for(HashMap<String, Object> hMap : globalEffects.get(guildId).get(userId)) {
                userEffects.add(new Pair<>((String) hMap.get("key"), (Long) hMap.get("value")));
            }
            return userEffects;
        }else
            return new ArrayList<>();
    }

}
