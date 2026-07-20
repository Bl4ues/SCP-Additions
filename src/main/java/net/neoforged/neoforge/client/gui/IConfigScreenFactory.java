package net.neoforged.neoforge.client.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
@FunctionalInterface
public interface IConfigScreenFactory { Screen create(Minecraft minecraft, Screen parent); }
