
package net.mcreator.scpadditions.network;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraftforge.network.NetworkEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.mcreator.scpadditions.world.inventory.TeslaTerminalMenu;
import net.mcreator.scpadditions.procedures.TeslaTerminalController;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Supplier;
import java.util.HashMap;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class TeslaTerminalButtonMessage {
	private final int buttonID, x, y, z;

	public TeslaTerminalButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public TeslaTerminalButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(TeslaTerminalButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(TeslaTerminalButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			handleButtonAction(entity, buttonID, x, y, z);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		if (entity == null) {
			return;
		}
		Level world = entity.level();
		HashMap guistate = TeslaTerminalMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID != 2 && !TeslaTerminalController.hasSecurityCredentials(entity)) {
			return;
		}
		if (buttonID == 0) {
			TeslaTerminalController.enableTeslaGates(world, x, y, z, entity);
		}
		if (buttonID == 1) {
			TeslaTerminalController.disableTeslaGates(world, x, y, z, entity);
		}
		if (buttonID == 2) {
			TeslaTerminalController.logout(entity);
		}
		if (buttonID == 3) {
			TeslaTerminalController.setManualOverride(world, x, y, z, entity, true);
		}
		if (buttonID == 4) {
			TeslaTerminalController.setManualOverride(world, x, y, z, entity, false);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		ScpAdditionsMod.addNetworkMessage(TeslaTerminalButtonMessage.class, TeslaTerminalButtonMessage::buffer, TeslaTerminalButtonMessage::new, TeslaTerminalButtonMessage::handler);
	}
}
