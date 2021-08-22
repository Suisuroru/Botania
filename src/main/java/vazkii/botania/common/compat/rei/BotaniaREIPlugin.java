/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.compat.rei;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.item.IAncientWillContainer;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.crafting.*;
import vazkii.botania.common.item.ItemAncientWill;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraPick;
import vazkii.botania.common.item.lens.ItemLens;
import vazkii.botania.common.lib.LibMisc;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static vazkii.botania.common.compat.rei.CategoryUtils.doesOreExist;
import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomDisplay;

@Environment(EnvType.CLIENT)
public class BotaniaREIPlugin implements REIClientPlugin {
	@Override
	public void registerCategories(CategoryRegistry helper) {
		helper.add(
				new BreweryREICategory(),
				new PureDaisyREICategory(),
				new RunicAltarREICategory(),
				new PetalApothecaryREICategory(),
				new ElvenTradeREICategory(),
				new ManaPoolREICategory(),
				new OrechidREICategory(BotaniaREICategoryIdentifiers.ORECHID, ModSubtiles.orechid),
				new OrechidREICategory(BotaniaREICategoryIdentifiers.ORECHID_IGNEM, ModSubtiles.orechidIgnem)
		);
		Set<ItemLike> apothecaries = ImmutableSet.of(
				ModBlocks.defaultAltar,
				ModBlocks.desertAltar,
				ModBlocks.forestAltar,
				ModBlocks.fungalAltar,
				ModBlocks.mesaAltar,
				ModBlocks.mossyAltar,
				ModBlocks.mountainAltar,
				ModBlocks.plainsAltar,
				ModBlocks.swampAltar,
				ModBlocks.taigaAltar);
		for (ItemLike altar : apothecaries) {
			helper.addWorkstations(BotaniaREICategoryIdentifiers.PETAL_APOTHECARY, EntryStacks.of(altar));
		}
		helper.addWorkstations(BotaniaREICategoryIdentifiers.BREWERY, EntryStacks.of(ModBlocks.brewery));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.ELVEN_TRADE, EntryStacks.of(ModBlocks.alfPortal));
		Set<ItemLike> manaPools = ImmutableSet.of(
				ModBlocks.manaPool,
				ModBlocks.dilutedPool,
				ModBlocks.fabulousPool
		);
		for (ItemLike pool : manaPools) {
			helper.addWorkstations(BotaniaREICategoryIdentifiers.MANA_INFUSION, EntryStacks.of(pool));
		}
		helper.addWorkstations(BotaniaREICategoryIdentifiers.ORECHID, EntryStacks.of(ModSubtiles.orechid), EntryStacks.of(ModSubtiles.orechidFloating));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.ORECHID_IGNEM, EntryStacks.of(ModSubtiles.orechidIgnem), EntryStacks.of(ModSubtiles.orechidIgnemFloating));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.PURE_DAISY, EntryStacks.of(ModSubtiles.pureDaisy), EntryStacks.of(ModSubtiles.pureDaisyFloating));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.RUNE_ALTAR, EntryStacks.of(ModBlocks.runeAltar));

		helper.removePlusButton(BotaniaREICategoryIdentifiers.PETAL_APOTHECARY);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.BREWERY);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.ELVEN_TRADE);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.MANA_INFUSION);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.ORECHID);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.ORECHID_IGNEM);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.PURE_DAISY);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.RUNE_ALTAR);
	}

	@Override
	public void registerDisplays(DisplayRegistry helper) {
		registerAncientWillRecipeWrapper(helper);
		registerCompositeLensRecipeWrapper(helper);
		registerTerraPickTippingRecipeWrapper(helper);

		helper.registerFiller(RecipePetals.class, PetalApothecaryREIDisplay::new);
		helper.registerFiller(RecipeBrew.class, BreweryREIDisplay::new);
		// rawtyped predicate due to rei oversight?
		Predicate<?> pred = (Recipe<?> recipe) -> recipe instanceof RecipeElvenTrade && recipe.getId().getNamespace().equals(LibMisc.MOD_ID) && !recipe.getId().getPath().contains("return");
		helper.registerFiller(pred, x -> new ElvenTradeREIDisplay((RecipeElvenTrade) x));
		helper.registerFiller(RecipeElvenTrade.class, ElvenTradeREIDisplay::new);
		helper.registerFiller(RecipeManaInfusion.class, ManaPoolREIDisplay::new);
		registerOrechidRecipes(helper, false);
		registerOrechidRecipes(helper, true);
		helper.registerFiller(RecipePureDaisy.class, PureDaisyREIDisplay::new);
		helper.registerFiller(RecipeRuneAltar.class, RunicAltarREIDisplay::new);
	}

	void registerAncientWillRecipeWrapper(DisplayRegistry helper) {
		ImmutableList.Builder<EntryIngredient> input = ImmutableList.builder();
		ImmutableList.Builder<EntryStack<ItemStack>> output = ImmutableList.builder();
		Set<ItemStack> wills = ImmutableSet.of(new ItemStack(ModItems.ancientWillAhrim), new ItemStack(ModItems.ancientWillDharok), new ItemStack(ModItems.ancientWillGuthan), new ItemStack(ModItems.ancientWillKaril), new ItemStack(ModItems.ancientWillTorag), new ItemStack(ModItems.ancientWillVerac));
		IAncientWillContainer container = (IAncientWillContainer) ModItems.terrasteelHelm;

		ItemStack helmet = new ItemStack(ModItems.terrasteelHelm);
		input.add(EntryIngredients.of(helmet));
		input.add(EntryIngredients.ofItemStacks(wills));
		for (ItemStack will : wills) {
			ItemStack copy = helmet.copy();
			container.addAncientWill(copy, ((ItemAncientWill) will.getItem()).type);
			output.add(EntryStacks.of(copy));
		}
		helper.add(new DefaultCustomDisplay(null, input.build(), Collections.singletonList(EntryIngredient.of(output.build()))));
	}

	void registerCompositeLensRecipeWrapper(DisplayRegistry helper) {
		List<ItemStack> lensStacks = ItemTags.getAllTags().getTagOrEmpty(prefix("lens"))
				.getValues().stream()
				.map(ItemStack::new)
				.filter(s -> !((ItemLens) s.getItem()).isControlLens(s))
				.filter(s -> ((ItemLens) s.getItem()).isCombinable(s))
				.collect(Collectors.toList());
		List<Item> lenses = lensStacks.stream().map(ItemStack::getItem).collect(Collectors.toList());
		List<EntryIngredient> inputs = Arrays.asList(EntryIngredients.ofItemStacks(lensStacks), EntryIngredients.of(new ItemStack(Items.SLIME_BALL)), EntryIngredients.ofItemStacks(lensStacks));
		int end = lenses.size() - 1;

		List<ItemStack> firstInput = new ArrayList<>();
		List<ItemStack> secondInput = new ArrayList<>();
		List<ItemStack> outputs = new ArrayList<>();
		for (int i = 1; i <= end; i++) {
			ItemStack firstLens = new ItemStack(lenses.get(i));
			for (Item secondLens : lenses) {
				if (secondLens == firstLens.getItem()) {
					continue;
				}

				ItemStack secondLensStack = new ItemStack(secondLens);
				if (((ItemLens) firstLens.getItem()).canCombineLenses(firstLens, secondLensStack)) {
					firstInput.add(firstLens);
					secondInput.add(secondLensStack);
					outputs.add(((ItemLens) firstLens.getItem()).setCompositeLens(firstLens.copy(), secondLensStack));
				}
			}
		}
		inputs.set(0, EntryIngredients.ofItemStacks(firstInput));
		inputs.set(2, EntryIngredients.ofItemStacks(secondInput));

		helper.add(new DefaultCustomDisplay(null, inputs, Collections.singletonList(EntryIngredients.ofItemStacks(outputs))));
	}

	void registerTerraPickTippingRecipeWrapper(DisplayRegistry helper) {
		List<EntryIngredient> inputs = ImmutableList.of(EntryIngredients.of(ModItems.terraPick), EntryIngredients.of(ModItems.elementiumPick));
		ItemStack output = new ItemStack(ModItems.terraPick);
		ItemTerraPick.setTipped(output);

		helper.add(new DefaultCustomDisplay(null, inputs, Collections.singletonList(EntryIngredients.of(output))));
	}

	void registerOrechidRecipes(DisplayRegistry helper, boolean isIgnem) {
		Map<ResourceLocation, Integer> oreWeights = isIgnem ? BotaniaAPI.instance().getNetherOreWeights() : BotaniaAPI.instance().getOreWeights();
		List<OrechidRecipeWrapper> orechidRecipes = oreWeights.entrySet().stream()
				.filter(e -> doesOreExist(e.getKey()))
				.map(OrechidRecipeWrapper::new)
				.sorted()
				.collect(Collectors.toList());
		for (OrechidRecipeWrapper recipe : orechidRecipes) {
			if (isIgnem) {
				helper.add(new OrechidIgnemREIDisplay(recipe));
			} else {
				helper.add(new OrechidREIDisplay(recipe));
			}
		}
	}
}