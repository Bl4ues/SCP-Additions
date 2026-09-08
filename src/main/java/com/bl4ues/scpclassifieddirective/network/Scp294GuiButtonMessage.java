package com.bl4ues.scpclassifieddirective.network;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import com.bl4ues.scpclassifieddirective.procedures.Scp294drinkGiveProcedure;
import com.bl4ues.scpclassifieddirective.world.inventory.Scp294GuiMenu;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Scp294GuiButtonMessage {
	private final int buttonID, x, y, z;
	private final String input;

	public Scp294GuiButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
		this.input = buffer.readUtf(80);
	}

	public Scp294GuiButtonMessage(int buttonID, int x, int y, int z, String input) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.input = input;
	}

	public static void buffer(Scp294GuiButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		String safe = message.input == null ? "" : message.input;
		if (safe.length() > 80) safe = safe.substring(0, 80);
		buffer.writeUtf(safe, 80);
	}

	public static void handler(Scp294GuiButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			if (entity != null) {
				handleButtonAction(entity, message.buttonID, message.x, message.y, message.z, message.input);
			}
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, String input) {
		Level world = entity.level();
		BlockPos pos = new BlockPos(x, y, z);
		if (!world.hasChunkAt(pos)) return;

		// A physical click is only an intention. The server accepts it while the
		// sender really owns the SCP-294 menu for this exact machine.
		if (!(entity.containerMenu instanceof Scp294GuiMenu menu)
				|| menu.x != x || menu.y != y || menu.z != z) {
			return;
		}

		if (buttonID == 0) {
			String safe = input == null ? "" : input;
			if (safe.length() > 80) safe = safe.substring(0, 80);
			Scp294drinkGiveProcedure.execute(world, x, y, z, entity, safe);
		} else if (buttonID == 1) {
			Scp294drinkGiveProcedure.insertCoinFromInventory(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		ScpClassifiedDirectiveMod.addNetworkMessage(Scp294GuiButtonMessage.class, Scp294GuiButtonMessage::buffer, Scp294GuiButtonMessage::new, Scp294GuiButtonMessage::handler);
	}
}
