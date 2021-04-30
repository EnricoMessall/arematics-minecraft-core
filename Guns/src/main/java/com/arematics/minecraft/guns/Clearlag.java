package com.arematics.minecraft.guns;

import com.arematics.minecraft.core.server.Server;
import com.arematics.minecraft.core.utils.ArematicsExecutor;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Data
@Component
public class Clearlag {
    LocalDateTime nextExecute;
    private final Server server;

    @Autowired
    public Clearlag(Server server){
        this.server = server;
        start();
    }

    public void start(){
        Duration duration = getDuration();
        nextExecute = LocalDateTime.now().plus(duration);


        if (duration.toMillis() > 120_000) {
            long timeTillFirstExecute = duration.toMillis() - 59_999;
            ArematicsExecutor.asyncDelayed(this::mentionClearLag, timeTillFirstExecute, TimeUnit.MILLISECONDS);
        }

        ArematicsExecutor.asyncDelayed(this::executeClearLag, duration.toMillis() - 5_000, TimeUnit.MILLISECONDS);
    }

    private void mentionClearLag(){
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
        server.getOnline().forEach(player -> player.info("clear_lag_time")
                .DEFAULT()
                .replace("time", dateTimeFormatter.format(player.parseTime(nextExecute)))
                .handle());
    }

    private void executeClearLag(){
        ArematicsExecutor.syncRepeat(this::clearLag, 0, 1, TimeUnit.SECONDS, 5);
    }

    private void clearLag(int time){
        float value = ((float) time) / ((float) 5);
        if (time == 0) {
            server.getOnline().forEach(player -> {
                String msg = player.info("clear_lag_now").DEFAULT().toObjectForFirst();
                player.bossBar().set(msg, 1);
                ArematicsExecutor.syncDelayed(() -> player.bossBar().hide(), 2, TimeUnit.SECONDS);
            });
            clear();
            ArematicsExecutor.asyncDelayed(this::start, 5, TimeUnit.SECONDS);
        } else {
            server.getOnline().forEach(player -> {
                String msg = player.info("clear_lag_in")
                        .DEFAULT()
                        .replace("seconds", String.valueOf(time))
                        .toObjectForFirst();
                player.bossBar().set(msg, value);
            });
        }
    }

    private void clear(){
        Bukkit.getWorlds().stream().map(World::getEntities).forEach(entities -> entities.forEach(this::clearEntity));
    }

    private void clearEntity(Entity entity){
        if(entity instanceof Item || entity instanceof Minecart){
            entity.remove();
        }
    }

    private Duration getDuration(){
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime nextClear = now.truncatedTo(ChronoUnit.HOURS)
                .plusMinutes(15 * (now.getMinute() / 15))
                .plusMinutes(15);

        return Duration.between(now, nextClear);
    }
}
