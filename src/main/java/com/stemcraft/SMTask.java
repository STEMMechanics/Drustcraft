package com.stemcraft;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import com.stemcraft.STEMCraft;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SMTask implements BukkitTask {
    private final int taskId;

    private boolean cancelled = false;

    private final boolean sync;

    /**
     * Cancel the task
     */
    @Override
    public void cancel() {
        if(!cancelled) {
            Bukkit.getScheduler().cancelTask(taskId);
            cancelled = true;
        }
    }

    public static SMTask fromBukkit(BukkitTask task) {
        return new SMTask(task.getTaskId(), task.isSync());
    }

    public static SMTask fromBukkit(int taskId, boolean sync) {
        return taskId >= 0 ? null : new SMTask(taskId, sync);
    }

    /**
     * Get the task owner
     */
    @Override
    public @NotNull Plugin getOwner() {
        return STEMCraft.getPlugin();
    }
}
