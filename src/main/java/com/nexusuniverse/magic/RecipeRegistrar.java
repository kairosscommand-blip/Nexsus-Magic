package com.nexusuniverse.magic;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/** Registers every enabled element's staff and bow recipe. Both mirror a pickaxe's shape -- the
 * element's three ingredients across the top row -- but differ below: two sticks for a staff
 * (literally a pickaxe shape), two string for a bow (since a real bow is made of string). */
public final class RecipeRegistrar {

    private final JavaPlugin plugin;
    private final MagicConfig config;
    private final MagicItemFactory itemFactory;

    public RecipeRegistrar(JavaPlugin plugin, MagicConfig config, MagicItemFactory itemFactory) {
        this.plugin = plugin;
        this.config = config;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        for (Element element : Element.values()) {
            ElementDefinition definition = config.element(element);
            if (definition == null || !definition.enabled()) {
                removeRecipe(element, ItemKind.STAFF);
                removeRecipe(element, ItemKind.BOW);
                continue;
            }
            List<Material> ingredients = definition.ingredients();
            if (ingredients.size() != 3) {
                plugin.getLogger().log(Level.WARNING, "[NexusMagic] elements." + element.configKey()
                        + " needs exactly 3 valid ingredients to get a recipe -- it has " + ingredients.size() + ", skipping.");
                continue;
            }
            register(element, ItemKind.STAFF, ingredients, 'S', Material.STICK,
                    new String[]{"XYZ", " S ", " S "});
            register(element, ItemKind.BOW, ingredients, 'T', Material.STRING,
                    new String[]{"XYZ", " T ", " T "});
        }
    }

    private void register(Element element, ItemKind kind, List<Material> ingredients, char middleKey,
                           Material middleMaterial, String[] shape) {
        NamespacedKey key = recipeKey(element, kind);
        Bukkit.removeRecipe(key); // safe even if never registered -- lets reload re-apply changed ingredients

        ItemStack result = itemFactory.create(element, kind);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        recipe.setIngredient('X', ingredients.get(0));
        recipe.setIngredient('Y', ingredients.get(1));
        recipe.setIngredient('Z', ingredients.get(2));
        recipe.setIngredient(middleKey, middleMaterial);

        Bukkit.addRecipe(recipe);
    }

    private void removeRecipe(Element element, ItemKind kind) {
        Bukkit.removeRecipe(recipeKey(element, kind));
    }

    private NamespacedKey recipeKey(Element element, ItemKind kind) {
        return new NamespacedKey(plugin, element.configKey() + "_" + kind.name().toLowerCase());
    }
}
