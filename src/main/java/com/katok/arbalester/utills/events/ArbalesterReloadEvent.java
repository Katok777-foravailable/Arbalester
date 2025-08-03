package com.katok.arbalester.utills.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.katok.arbalester.Arbalester;

public class ArbalesterReloadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Arbalester instance;

    public ArbalesterReloadEvent(Arbalester instance) {
        this.instance = instance;
    }

    public Arbalester getInstance() {
        return instance;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
