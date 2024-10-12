
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.scpadditions.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import net.mcreator.scpadditions.item.YogurtItem;
import net.mcreator.scpadditions.item.VodkaItem;
import net.mcreator.scpadditions.item.TeaItem;
import net.mcreator.scpadditions.item.SprayItem;
import net.mcreator.scpadditions.item.SpiritItem;
import net.mcreator.scpadditions.item.SecurityCredentialsItem;
import net.mcreator.scpadditions.item.Scp330YellowCandyItem;
import net.mcreator.scpadditions.item.Scp330RedCandyItem;
import net.mcreator.scpadditions.item.Scp330GreenCandyItem;
import net.mcreator.scpadditions.item.Scp330BlueCandyItem;
import net.mcreator.scpadditions.item.Scp1176honeyItem;
import net.mcreator.scpadditions.item.SCP572Item;
import net.mcreator.scpadditions.item.QuantumItem;
import net.mcreator.scpadditions.item.PlayingCardItem;
import net.mcreator.scpadditions.item.PiecesOfPaperItem;
import net.mcreator.scpadditions.item.PearCiderItem;
import net.mcreator.scpadditions.item.NeutroniumItem;
import net.mcreator.scpadditions.item.MorphineItem;
import net.mcreator.scpadditions.item.Level6KeycardItem;
import net.mcreator.scpadditions.item.Level5KeycardItem;
import net.mcreator.scpadditions.item.Level4KeycardItem;
import net.mcreator.scpadditions.item.Level3KeycardItem;
import net.mcreator.scpadditions.item.Level2KeycardItem;
import net.mcreator.scpadditions.item.Level1KeycardItem;
import net.mcreator.scpadditions.item.LagerItem;
import net.mcreator.scpadditions.item.IronCItem;
import net.mcreator.scpadditions.item.IpecacItem;
import net.mcreator.scpadditions.item.InsulinItem;
import net.mcreator.scpadditions.item.InkItem;
import net.mcreator.scpadditions.item.IceCreamItem;
import net.mcreator.scpadditions.item.HotItem;
import net.mcreator.scpadditions.item.HoneyItem;
import net.mcreator.scpadditions.item.HeroinItem;
import net.mcreator.scpadditions.item.HazmatSuitItem;
import net.mcreator.scpadditions.item.HappinessItem;
import net.mcreator.scpadditions.item.GrogItem;
import net.mcreator.scpadditions.item.GrimaceShakeItem;
import net.mcreator.scpadditions.item.GoldCItem;
import net.mcreator.scpadditions.item.GlassItem;
import net.mcreator.scpadditions.item.GinItem;
import net.mcreator.scpadditions.item.Geiger3Item;
import net.mcreator.scpadditions.item.Geiger2Item;
import net.mcreator.scpadditions.item.Geiger1Item;
import net.mcreator.scpadditions.item.FrozenYogurtItem;
import net.mcreator.scpadditions.item.FecesItem;
import net.mcreator.scpadditions.item.FecesAndBloodItem;
import net.mcreator.scpadditions.item.FearItem;
import net.mcreator.scpadditions.item.EthanolItem;
import net.mcreator.scpadditions.item.EstusItem;
import net.mcreator.scpadditions.item.EspressoItem;
import net.mcreator.scpadditions.item.EnergyDrinkItem;
import net.mcreator.scpadditions.item.EmptyCupItem;
import net.mcreator.scpadditions.item.EggsItem;
import net.mcreator.scpadditions.item.Drink1Item;
import net.mcreator.scpadditions.item.DeathItem;
import net.mcreator.scpadditions.item.CurryItem;
import net.mcreator.scpadditions.item.CreditCardItem;
import net.mcreator.scpadditions.item.CourageItem;
import net.mcreator.scpadditions.item.CosmopolitanItem;
import net.mcreator.scpadditions.item.CorrosiveBlackItem;
import net.mcreator.scpadditions.item.CorrosiveAcidItem;
import net.mcreator.scpadditions.item.ColdItem;
import net.mcreator.scpadditions.item.ColaItem;
import net.mcreator.scpadditions.item.CoinItem;
import net.mcreator.scpadditions.item.CoffeeItem;
import net.mcreator.scpadditions.item.CoconutItem;
import net.mcreator.scpadditions.item.CocaineItem;
import net.mcreator.scpadditions.item.CiderItem;
import net.mcreator.scpadditions.item.ChocolateItem;
import net.mcreator.scpadditions.item.ChimItem;
import net.mcreator.scpadditions.item.ChampionItem;
import net.mcreator.scpadditions.item.ChampagneItem;
import net.mcreator.scpadditions.item.CassisFantaItem;
import net.mcreator.scpadditions.item.CarrotItem;
import net.mcreator.scpadditions.item.CarbonItem;
import net.mcreator.scpadditions.item.CactusItem;
import net.mcreator.scpadditions.item.BloodOfChristItem;
import net.mcreator.scpadditions.item.BloodItem;
import net.mcreator.scpadditions.item.BleachItem;
import net.mcreator.scpadditions.item.BeerItem;
import net.mcreator.scpadditions.item.AquaRegiaItem;
import net.mcreator.scpadditions.item.AppleCiderItem;
import net.mcreator.scpadditions.item.AntiEnergyItem;
import net.mcreator.scpadditions.item.AmnesiaItem;
import net.mcreator.scpadditions.item.AloeItem;
import net.mcreator.scpadditions.ScpAdditionsMod;

public class ScpAdditionsModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, ScpAdditionsMod.MODID);
	public static final RegistryObject<Item> BUTTON_ROFF = block(ScpAdditionsModBlocks.BUTTON_ROFF);
	public static final RegistryObject<Item> BUTTON_LOFF = block(ScpAdditionsModBlocks.BUTTON_LOFF);
	public static final RegistryObject<Item> TESLA_GATE = block(ScpAdditionsModBlocks.TESLA_GATE);
	public static final RegistryObject<Item> TESLA_TERMINAL_OFF = block(ScpAdditionsModBlocks.TESLA_TERMINAL_OFF);
	public static final RegistryObject<Item> SECURITY_CREDENTIALS = REGISTRY.register("security_credentials", () -> new SecurityCredentialsItem());
	public static final RegistryObject<Item> DECON_OPEN = block(ScpAdditionsModBlocks.DECON_OPEN);
	public static final RegistryObject<Item> HAZMAT_SUIT_HELMET = REGISTRY.register("hazmat_suit_helmet", () -> new HazmatSuitItem.Helmet());
	public static final RegistryObject<Item> HAZMAT_SUIT_CHESTPLATE = REGISTRY.register("hazmat_suit_chestplate", () -> new HazmatSuitItem.Chestplate());
	public static final RegistryObject<Item> HAZMAT_SUIT_LEGGINGS = REGISTRY.register("hazmat_suit_leggings", () -> new HazmatSuitItem.Leggings());
	public static final RegistryObject<Item> HAZMAT_SUIT_BOOTS = REGISTRY.register("hazmat_suit_boots", () -> new HazmatSuitItem.Boots());
	public static final RegistryObject<Item> GEIGER_1 = REGISTRY.register("geiger_1", () -> new Geiger1Item());
	public static final RegistryObject<Item> SPRAY = REGISTRY.register("spray", () -> new SprayItem());
	public static final RegistryObject<Item> SCP_079_SYSTEM_CONTROL = block(ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL);
	public static final RegistryObject<Item> SCP_079CONTROLOFF = block(ScpAdditionsModBlocks.SCP_079CONTROLOFF);
	public static final RegistryObject<Item> SCP_059 = block(ScpAdditionsModBlocks.SCP_059);
	public static final RegistryObject<Item> SCP_059_CONTAINED = block(ScpAdditionsModBlocks.SCP_059_CONTAINED);
	public static final RegistryObject<Item> SCP_059_1 = block(ScpAdditionsModBlocks.SCP_059_1);
	public static final RegistryObject<Item> SCP_079ON = block(ScpAdditionsModBlocks.SCP_079ON);
	public static final RegistryObject<Item> SCP_294 = block(ScpAdditionsModBlocks.SCP_294);
	public static final RegistryObject<Item> SCP_330 = block(ScpAdditionsModBlocks.SCP_330);
	public static final RegistryObject<Item> SCP_426 = block(ScpAdditionsModBlocks.SCP_426);
	public static final RegistryObject<Item> SCP_572 = REGISTRY.register("scp_572", () -> new SCP572Item());
	public static final RegistryObject<Item> SCP_902_CLOSED = block(ScpAdditionsModBlocks.SCP_902_CLOSED);
	public static final RegistryObject<Item> SCP_914BLOCK = block(ScpAdditionsModBlocks.SCP_914BLOCK);
	public static final RegistryObject<Item> SCP_914CLOCKWORKS = block(ScpAdditionsModBlocks.SCP_914CLOCKWORKS);
	public static final RegistryObject<Item> SCP_914BODY = block(ScpAdditionsModBlocks.SCP_914BODY);
	public static final RegistryObject<Item> SCP_914DIAL_1TO_1 = block(ScpAdditionsModBlocks.SCP_914DIAL_1TO_1);
	public static final RegistryObject<Item> SCP_914_KEY_WIND = block(ScpAdditionsModBlocks.SCP_914_KEY_WIND);
	public static final RegistryObject<Item> SCP_914_INTAKE = block(ScpAdditionsModBlocks.SCP_914_INTAKE);
	public static final RegistryObject<Item> SCP_914_OUTPUT = block(ScpAdditionsModBlocks.SCP_914_OUTPUT);
	public static final RegistryObject<Item> SCP_914_INTAKE_DOOR = block(ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR);
	public static final RegistryObject<Item> SCP_914_OUTPUT_DOOR = block(ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR);
	public static final RegistryObject<Item> SCP_1176 = block(ScpAdditionsModBlocks.SCP_1176);
	public static final RegistryObject<Item> SCP_330_RED_CANDY = REGISTRY.register("scp_330_red_candy", () -> new Scp330RedCandyItem());
	public static final RegistryObject<Item> SCP_330_GREEN_CANDY = REGISTRY.register("scp_330_green_candy", () -> new Scp330GreenCandyItem());
	public static final RegistryObject<Item> SCP_330_YELLOW_CANDY = REGISTRY.register("scp_330_yellow_candy", () -> new Scp330YellowCandyItem());
	public static final RegistryObject<Item> SCP_330_BLUE_CANDY = REGISTRY.register("scp_330_blue_candy", () -> new Scp330BlueCandyItem());
	public static final RegistryObject<Item> SCP_1176HONEY = REGISTRY.register("scp_1176honey", () -> new Scp1176honeyItem());
	public static final RegistryObject<Item> PLAYING_CARD = REGISTRY.register("playing_card", () -> new PlayingCardItem());
	public static final RegistryObject<Item> CREDIT_CARD = REGISTRY.register("credit_card", () -> new CreditCardItem());
	public static final RegistryObject<Item> PIECES_OF_PAPER = REGISTRY.register("pieces_of_paper", () -> new PiecesOfPaperItem());
	public static final RegistryObject<Item> COIN = REGISTRY.register("coin", () -> new CoinItem());
	public static final RegistryObject<Item> EMPTY_CUP = REGISTRY.register("empty_cup", () -> new EmptyCupItem());
	public static final RegistryObject<Item> LEVEL_1_KEYCARD = REGISTRY.register("level_1_keycard", () -> new Level1KeycardItem());
	public static final RegistryObject<Item> LEVEL_2_KEYCARD = REGISTRY.register("level_2_keycard", () -> new Level2KeycardItem());
	public static final RegistryObject<Item> LEVEL_3_KEYCARD = REGISTRY.register("level_3_keycard", () -> new Level3KeycardItem());
	public static final RegistryObject<Item> LEVEL_4_KEYCARD = REGISTRY.register("level_4_keycard", () -> new Level4KeycardItem());
	public static final RegistryObject<Item> LEVEL_5_KEYCARD = REGISTRY.register("level_5_keycard", () -> new Level5KeycardItem());
	public static final RegistryObject<Item> LEVEL_6_KEYCARD = REGISTRY.register("level_6_keycard", () -> new Level6KeycardItem());
	public static final RegistryObject<Item> RIGHT_READER = block(ScpAdditionsModBlocks.RIGHT_READER);
	public static final RegistryObject<Item> LEFT_READER = block(ScpAdditionsModBlocks.LEFT_READER);
	public static final RegistryObject<Item> LV_2_RIGHT_READER = block(ScpAdditionsModBlocks.LV_2_RIGHT_READER);
	public static final RegistryObject<Item> LV_2_LEFT_READER = block(ScpAdditionsModBlocks.LV_2_LEFT_READER);
	public static final RegistryObject<Item> LV_3_RIGHT_READER = block(ScpAdditionsModBlocks.LV_3_RIGHT_READER);
	public static final RegistryObject<Item> LV_3_LEFT_READER = block(ScpAdditionsModBlocks.LV_3_LEFT_READER);
	public static final RegistryObject<Item> LV_4_RIGHT_READER = block(ScpAdditionsModBlocks.LV_4_RIGHT_READER);
	public static final RegistryObject<Item> LV_4_LEFT_READER = block(ScpAdditionsModBlocks.LV_4_LEFT_READER);
	public static final RegistryObject<Item> LV_5_RIGHT_READER = block(ScpAdditionsModBlocks.LV_5_RIGHT_READER);
	public static final RegistryObject<Item> LV_5_LEFT_READER = block(ScpAdditionsModBlocks.LV_5_LEFT_READER);
	public static final RegistryObject<Item> LV_6_RIGHT_READER = block(ScpAdditionsModBlocks.LV_6_RIGHT_READER);
	public static final RegistryObject<Item> LV_6_LEFT_READER = block(ScpAdditionsModBlocks.LV_6_LEFT_READER);
	public static final RegistryObject<Item> TESLA_ACTIVE = block(ScpAdditionsModBlocks.TESLA_ACTIVE);
	public static final RegistryObject<Item> TESLA_RECHARGE = block(ScpAdditionsModBlocks.TESLA_RECHARGE);
	public static final RegistryObject<Item> TESLA_ACTIVE_2 = block(ScpAdditionsModBlocks.TESLA_ACTIVE_2);
	public static final RegistryObject<Item> TESLA_ACTIVE_3 = block(ScpAdditionsModBlocks.TESLA_ACTIVE_3);
	public static final RegistryObject<Item> TESLA_ACTIVE_4 = block(ScpAdditionsModBlocks.TESLA_ACTIVE_4);
	public static final RegistryObject<Item> TESLA_TERMINAL_BLOCK = block(ScpAdditionsModBlocks.TESLA_TERMINAL_BLOCK);
	public static final RegistryObject<Item> SCP_079OFF = block(ScpAdditionsModBlocks.SCP_079OFF);
	public static final RegistryObject<Item> SCP_079CONTROL = block(ScpAdditionsModBlocks.SCP_079CONTROL);
	public static final RegistryObject<Item> SCP_902_OPEN = block(ScpAdditionsModBlocks.SCP_902_OPEN);
	public static final RegistryObject<Item> BUTTON_RON = block(ScpAdditionsModBlocks.BUTTON_RON);
	public static final RegistryObject<Item> BUTTON_LON = block(ScpAdditionsModBlocks.BUTTON_LON);
	public static final RegistryObject<Item> SCP_914DIAL_ROUGH = block(ScpAdditionsModBlocks.SCP_914DIAL_ROUGH);
	public static final RegistryObject<Item> SCP_914DIAL_COARSE = block(ScpAdditionsModBlocks.SCP_914DIAL_COARSE);
	public static final RegistryObject<Item> SCP_914DIAL_FINE = block(ScpAdditionsModBlocks.SCP_914DIAL_FINE);
	public static final RegistryObject<Item> SCP_914DIAL_VERY_FINE = block(ScpAdditionsModBlocks.SCP_914DIAL_VERY_FINE);
	public static final RegistryObject<Item> SCP_914CLOCKWORKS_2 = block(ScpAdditionsModBlocks.SCP_914CLOCKWORKS_2);
	public static final RegistryObject<Item> SCP_914_OUTPUT_DOOR_CLOSED = block(ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR_CLOSED);
	public static final RegistryObject<Item> SCP_914_INTAKE_DOOR_CLOSED = block(ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR_CLOSED);
	public static final RegistryObject<Item> GEIGER_2 = REGISTRY.register("geiger_2", () -> new Geiger2Item());
	public static final RegistryObject<Item> GEIGER_3 = REGISTRY.register("geiger_3", () -> new Geiger3Item());
	public static final RegistryObject<Item> SCP_0591INFECTED_3_SPAWN_EGG = REGISTRY.register("scp_0591infected_3_spawn_egg", () -> new ForgeSpawnEggItem(ScpAdditionsModEntities.SCP_0591INFECTED_3, -1, -1, new Item.Properties()));
	public static final RegistryObject<Item> DECON_CLOSED = block(ScpAdditionsModBlocks.DECON_CLOSED);
	public static final RegistryObject<Item> DECON_OPEN_RELOAD = block(ScpAdditionsModBlocks.DECON_OPEN_RELOAD);
	public static final RegistryObject<Item> RIGHT_READER_WRONG = block(ScpAdditionsModBlocks.RIGHT_READER_WRONG);
	public static final RegistryObject<Item> RIGHT_READER_ACCEPT = block(ScpAdditionsModBlocks.RIGHT_READER_ACCEPT);
	public static final RegistryObject<Item> LEFT_READER_WRONG = block(ScpAdditionsModBlocks.LEFT_READER_WRONG);
	public static final RegistryObject<Item> LEFT_READER_ACCEPT = block(ScpAdditionsModBlocks.LEFT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_2_RIGHT_READER_WRONG = block(ScpAdditionsModBlocks.LV_2_RIGHT_READER_WRONG);
	public static final RegistryObject<Item> LV_2_RIGHT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_2_RIGHT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_2_LEFT_READER_WRONG = block(ScpAdditionsModBlocks.LV_2_LEFT_READER_WRONG);
	public static final RegistryObject<Item> LV_2_LEFT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_2_LEFT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_3_RIGHT_READER_WRONG = block(ScpAdditionsModBlocks.LV_3_RIGHT_READER_WRONG);
	public static final RegistryObject<Item> LV_3_RIGHT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_3_RIGHT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_3_LEFT_READER_WRONG = block(ScpAdditionsModBlocks.LV_3_LEFT_READER_WRONG);
	public static final RegistryObject<Item> LV_3_LEFT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_3_LEFT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_4_RIGHT_READER_WRONG = block(ScpAdditionsModBlocks.LV_4_RIGHT_READER_WRONG);
	public static final RegistryObject<Item> LV_4_RIGHT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_4_RIGHT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_4_LEFT_READER_WRONG = block(ScpAdditionsModBlocks.LV_4_LEFT_READER_WRONG);
	public static final RegistryObject<Item> LV_4_LEFT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_4_LEFT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_5_RIGHT_READER_WRONG = block(ScpAdditionsModBlocks.LV_5_RIGHT_READER_WRONG);
	public static final RegistryObject<Item> LV_5_RIGHT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_5_RIGHT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_5_LEFT_READER_WRONG = block(ScpAdditionsModBlocks.LV_5_LEFT_READER_WRONG);
	public static final RegistryObject<Item> LV_5_LEFT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_5_LEFT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_6_RIGHT_READER_WRONG = block(ScpAdditionsModBlocks.LV_6_RIGHT_READER_WRONG);
	public static final RegistryObject<Item> LV_6_RIGHT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_6_RIGHT_READER_ACCEPT);
	public static final RegistryObject<Item> LV_6_LEFT_READER_WRONG = block(ScpAdditionsModBlocks.LV_6_LEFT_READER_WRONG);
	public static final RegistryObject<Item> LV_6_LEFT_READER_ACCEPT = block(ScpAdditionsModBlocks.LV_6_LEFT_READER_ACCEPT);
	public static final RegistryObject<Item> SCP_294_OUT_OF_RANGE = block(ScpAdditionsModBlocks.SCP_294_OUT_OF_RANGE);
	public static final RegistryObject<Item> SCP_294_STOCKING = block(ScpAdditionsModBlocks.SCP_294_STOCKING);
	public static final RegistryObject<Item> CUP_OF_COFFEE = REGISTRY.register("cup_of_coffee", () -> new CoffeeItem());
	public static final RegistryObject<Item> CUP_OF_ALCOHOL = REGISTRY.register("cup_of_alcohol", () -> new Drink1Item());
	public static final RegistryObject<Item> ETHANOL = REGISTRY.register("ethanol", () -> new EthanolItem());
	public static final RegistryObject<Item> SPIRIT = REGISTRY.register("spirit", () -> new SpiritItem());
	public static final RegistryObject<Item> VODKA = REGISTRY.register("vodka", () -> new VodkaItem());
	public static final RegistryObject<Item> ALOE = REGISTRY.register("aloe", () -> new AloeItem());
	public static final RegistryObject<Item> CACTUS = REGISTRY.register("cactus", () -> new CactusItem());
	public static final RegistryObject<Item> AMNESIA = REGISTRY.register("amnesia", () -> new AmnesiaItem());
	public static final RegistryObject<Item> ANTI_ENERGY = REGISTRY.register("anti_energy", () -> new AntiEnergyItem());
	public static final RegistryObject<Item> AQUA_REGIA = REGISTRY.register("aqua_regia", () -> new AquaRegiaItem());
	public static final RegistryObject<Item> BEER = REGISTRY.register("beer", () -> new BeerItem());
	public static final RegistryObject<Item> LAGER = REGISTRY.register("lager", () -> new LagerItem());
	public static final RegistryObject<Item> CORROSIVE_BLACK = REGISTRY.register("corrosive_black", () -> new CorrosiveBlackItem());
	public static final RegistryObject<Item> BLEACH = REGISTRY.register("bleach", () -> new BleachItem());
	public static final RegistryObject<Item> BLOOD = REGISTRY.register("blood", () -> new BloodItem());
	public static final RegistryObject<Item> BLOOD_OF_CHRIST = REGISTRY.register("blood_of_christ", () -> new BloodOfChristItem());
	public static final RegistryObject<Item> GRIMACE_SHAKE = REGISTRY.register("grimace_shake", () -> new GrimaceShakeItem());
	public static final RegistryObject<Item> QUANTUM = REGISTRY.register("quantum", () -> new QuantumItem());
	public static final RegistryObject<Item> CARBON = REGISTRY.register("carbon", () -> new CarbonItem());
	public static final RegistryObject<Item> CASSIS_FANTA = REGISTRY.register("cassis_fanta", () -> new CassisFantaItem());
	public static final RegistryObject<Item> CARROT = REGISTRY.register("carrot", () -> new CarrotItem());
	public static final RegistryObject<Item> CHAMPAGNE = REGISTRY.register("champagne", () -> new ChampagneItem());
	public static final RegistryObject<Item> CHIM = REGISTRY.register("chim", () -> new ChimItem());
	public static final RegistryObject<Item> CIDER = REGISTRY.register("cider", () -> new CiderItem());
	public static final RegistryObject<Item> APPLE_CIDER = REGISTRY.register("apple_cider", () -> new AppleCiderItem());
	public static final RegistryObject<Item> PEAR_CIDER = REGISTRY.register("pear_cider", () -> new PearCiderItem());
	public static final RegistryObject<Item> CHOCOLATE = REGISTRY.register("chocolate", () -> new ChocolateItem());
	public static final RegistryObject<Item> COCAINE = REGISTRY.register("cocaine", () -> new CocaineItem());
	public static final RegistryObject<Item> COCONUT = REGISTRY.register("coconut", () -> new CoconutItem());
	public static final RegistryObject<Item> COLA = REGISTRY.register("cola", () -> new ColaItem());
	public static final RegistryObject<Item> COLD = REGISTRY.register("cold", () -> new ColdItem());
	public static final RegistryObject<Item> COSMOPOLITAN = REGISTRY.register("cosmopolitan", () -> new CosmopolitanItem());
	public static final RegistryObject<Item> COURAGE = REGISTRY.register("courage", () -> new CourageItem());
	public static final RegistryObject<Item> CURRY = REGISTRY.register("curry", () -> new CurryItem());
	public static final RegistryObject<Item> DEATH = REGISTRY.register("death", () -> new DeathItem());
	public static final RegistryObject<Item> EGGS = REGISTRY.register("eggs", () -> new EggsItem());
	public static final RegistryObject<Item> NEUTRONIUM = REGISTRY.register("neutronium", () -> new NeutroniumItem());
	public static final RegistryObject<Item> ENERGY_DRINK = REGISTRY.register("energy_drink", () -> new EnergyDrinkItem());
	public static final RegistryObject<Item> ESPRESSO = REGISTRY.register("espresso", () -> new EspressoItem());
	public static final RegistryObject<Item> ESTUS = REGISTRY.register("estus", () -> new EstusItem());
	public static final RegistryObject<Item> CHAMPION = REGISTRY.register("champion", () -> new ChampionItem());
	public static final RegistryObject<Item> FEAR = REGISTRY.register("fear", () -> new FearItem());
	public static final RegistryObject<Item> FECES = REGISTRY.register("feces", () -> new FecesItem());
	public static final RegistryObject<Item> FECES_AND_BLOOD = REGISTRY.register("feces_and_blood", () -> new FecesAndBloodItem());
	public static final RegistryObject<Item> GIN = REGISTRY.register("gin", () -> new GinItem());
	public static final RegistryObject<Item> GLASS = REGISTRY.register("glass", () -> new GlassItem());
	public static final RegistryObject<Item> GOLD_C = REGISTRY.register("gold_c", () -> new GoldCItem());
	public static final RegistryObject<Item> GROG = REGISTRY.register("grog", () -> new GrogItem());
	public static final RegistryObject<Item> HAPPINESS = REGISTRY.register("happiness", () -> new HappinessItem());
	public static final RegistryObject<Item> HEROIN = REGISTRY.register("heroin", () -> new HeroinItem());
	public static final RegistryObject<Item> MORPHINE = REGISTRY.register("morphine", () -> new MorphineItem());
	public static final RegistryObject<Item> HONEY = REGISTRY.register("honey", () -> new HoneyItem());
	public static final RegistryObject<Item> HOT = REGISTRY.register("hot", () -> new HotItem());
	public static final RegistryObject<Item> TEA = REGISTRY.register("tea", () -> new TeaItem());
	public static final RegistryObject<Item> CORROSIVE_ACID = REGISTRY.register("corrosive_acid", () -> new CorrosiveAcidItem());
	public static final RegistryObject<Item> ICE_CREAM = REGISTRY.register("ice_cream", () -> new IceCreamItem());
	public static final RegistryObject<Item> FROZEN_YOGURT = REGISTRY.register("frozen_yogurt", () -> new FrozenYogurtItem());
	public static final RegistryObject<Item> YOGURT = REGISTRY.register("yogurt", () -> new YogurtItem());
	public static final RegistryObject<Item> INK = REGISTRY.register("ink", () -> new InkItem());
	public static final RegistryObject<Item> INSULIN = REGISTRY.register("insulin", () -> new InsulinItem());
	public static final RegistryObject<Item> IPECAC = REGISTRY.register("ipecac", () -> new IpecacItem());
	public static final RegistryObject<Item> IRON_C = REGISTRY.register("iron_c", () -> new IronCItem());

	private static RegistryObject<Item> block(RegistryObject<Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}
