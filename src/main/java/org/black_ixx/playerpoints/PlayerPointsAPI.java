package org.black_ixx.playerpoints;

import com.avaje.ebean.EbeanServer;
import com.mengcraft.economy.$;
import com.mengcraft.economy.Main;
import com.mengcraft.economy.entity.Log;
import lombok.val;
import org.black_ixx.playerpoints.event.PlayerPointsChangeEvent;
import org.black_ixx.playerpoints.event.PlayerPointsResetEvent;
import org.bukkit.Bukkit;

import java.util.UUID;

import static com.mengcraft.economy.$.nil;

/**
 * API hook.
 */
public class PlayerPointsAPI {

    private final EbeanServer database;
    private final Main main;

    public PlayerPointsAPI(Main main, EbeanServer database) {
        this.main = main;
        this.database = database;
    }

    public boolean give(UUID who, int value) {
        $.thr(nil(who), "nil");
        val event = new PlayerPointsChangeEvent(who, value);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        val update = database.createUpdate(PP.class, "update " + PP.TABLE_NAME + " set value = value + :value where who = :who")
                .set("who", who)
                .set("value", event.getChange());
        int result = update.execute();
        if (result == 0) {
            PP p = new PP();
            p.setWho(who);
            p.setValue(event.getChange());
            database.insert(p);
            // Thr exception here if op not okay
        }

        main.log(Bukkit.getOfflinePlayer(who), event.getChange(), "point", Log.Op.ADD);

        return true;
    }

    @Deprecated
    public boolean give(String name, int amount) {
        val p = Bukkit.getPlayerExact(name);
        $.thr(nil(p), "offline");
        return give(p.getUniqueId(), amount);
    }

    public boolean take(UUID who, int amount) {
        $.thr(nil(who), "nil");
        int look = look(who);
        return look - amount > -1 && give(who, -amount);
    }

    @Deprecated
    public boolean take(String name, int amount) {
        val p = Bukkit.getPlayerExact(name);
        $.thr(nil(p), "offline");
        return take(p.getUniqueId(), amount);
    }

    public int look(UUID who) {
        $.thr(nil(who), "nil");
        val find = database.find(PP.class, who);
        if (!nil(find)) {
            return find.getValue();
        }
        return 0;
    }

    @Deprecated
    public int look(String name) {
        val p = Bukkit.getPlayerExact(name);
        $.thr(nil(p), "offline");
        return look(p.getUniqueId());
    }

    public boolean pay(UUID who, UUID to, int amount) {
        return take(who, amount) && give(to, amount);
    }

    @Deprecated
    public boolean pay(String name, String to, int amount) {
        val p = Bukkit.getPlayerExact(name);
        val i = Bukkit.getPlayerExact(to);
        $.thr(nil(p) || nil(i), "offline");
        return pay(p.getUniqueId(), i.getUniqueId(), amount);
    }

    public boolean set(UUID who, int amount) {
        $.thr(nil(who), "nil");
        val event = new PlayerPointsResetEvent(who);
        event.setChange(amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        val update = database.createUpdate(PP.class, "update " + PP.TABLE_NAME + " set value = :value where who = :who")
                .set("who", who)
                .set("value", event.getChange());
        int result = update.execute();
        if (result == 0) {
            PP p = new PP();
            p.setWho(who);
            p.setValue(event.getChange());
            database.insert(p);
        }

        main.log(Bukkit.getOfflinePlayer(who), event.getChange(), "point", Log.Op.SET);

        return true;
    }

    @Deprecated
    public boolean set(String name, int amount) {
        val p = Bukkit.getPlayerExact(name);
        $.thr(nil(p), "offline");
        return set(p.getUniqueId(), amount);
    }

    public boolean reset(UUID who) {
        return set(who, 0);
    }

    @Deprecated
    public boolean reset(String name, int amount) {
        val p = Bukkit.getPlayerExact(name);
        $.thr(nil(p), "offline");
        return set(p.getUniqueId(), amount);
    }

}
