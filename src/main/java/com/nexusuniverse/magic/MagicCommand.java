package com.nexusuniverse.magic;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;

public final class MagicCommand implements CommandExecutor {

    private static final String USAGE =
            "§cUsage: /nexusmagic <status [player]|elements|give <player> <element> <staff|bow>|setlevel <player> <level>|reload>";

    private final MagicConfig config;
    private final WizardLevelManager levels;
    private final MagicItemFactory itemFactory;
    private final RecipeRegistrar recipeRegistrar;

    public MagicCommand(MagicConfig config, WizardLevelManager levels, MagicItemFactory itemFactory,
                         RecipeRegistrar recipeRegistrar) {
        this.config = config;
        this.levels = levels;
        this.itemFactory = itemFactory;
        this.recipeRegistrar = recipeRegistrar;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(USAGE);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> handleStatus(sender, args);
            case "elements" -> handleElements(sender);
            case "give" -> handleGive(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(USAGE);
        }
        return true;
    }

    private void handleStatus(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("nexusmagic.admin")) {
                sender.sendMessage("§cYou can only check your own wizard status.");
                return;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cThat player isn't online.");
                return;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage("§cConsole must name a player: /nexusmagic status <player>");
            return;
        }

        int level = levels.level(target);
        double xp = levels.xpTowardNext(target);
        double needed = levels.xpNeededForNext(target);

        sender.sendMessage("§d" + target.getName() + "'s wizard status:");
        if (level >= config.maxLevel()) {
            sender.sendMessage("§7Level: §f" + level + " §7(max)");
        } else {
            sender.sendMessage("§7Level: §f" + level + " §7(" + trim(xp) + "/" + trim(needed) + " xp to next)");
        }
        sender.sendMessage("§7Backfire chance: §f" + percent(levels.backfireChance(target))
                + " §7for §f" + trim(levels.backfireDamage(target)) + "§7 damage");
        sender.sendMessage("§7Damage multiplier: §fx" + trim(levels.damageMultiplier(target))
                + " §7Reward multiplier: §fx" + trim(levels.rewardMultiplier(target)));
    }

    private void handleElements(CommandSender sender) {
        sender.sendMessage("§dElements (top row = ingredients, staff uses 2 sticks below, bow uses 2 string):");
        for (Element element : Element.values()) {
            ElementDefinition definition = config.element(element);
            if (definition == null || !definition.enabled()) {
                continue;
            }
            String ingredients = definition.ingredients().stream()
                    .map(Enum::name)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("(none configured)");
            sender.sendMessage("§7 - " + definition.displayName() + "§7: " + ingredients);
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nexusmagic.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /nexusmagic give <player> <element> <staff|bow>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cThat player isn't online.");
            return;
        }
        Element element;
        try {
            element = Element.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§cUnknown element. Try: /nexusmagic elements");
            return;
        }
        ItemKind kind;
        try {
            kind = ItemKind.valueOf(args[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§cKind must be \"staff\" or \"bow\".");
            return;
        }

        ItemStack item = itemFactory.create(element, kind);
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            target.getWorld().dropItemNaturally(target.getLocation(), item);
        }
        sender.sendMessage("§aGave " + target.getName() + " a " + config.element(element).displayName()
                + " " + kind.name().toLowerCase(Locale.ROOT) + ".");
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nexusmagic.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /nexusmagic setlevel <player> <level>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cThat player isn't online.");
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cThat's not a whole number.");
            return;
        }
        levels.setLevel(target, level);
        sender.sendMessage("§aSet " + target.getName() + "'s wizard level to " + levels.level(target) + ".");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nexusmagic.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return;
        }
        config.reload();
        recipeRegistrar.registerAll();
        sender.sendMessage("§aNexusMagic config reloaded.");
    }

    private String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    private String percent(double fraction) {
        return String.format("%.0f%%", fraction * 100.0);
    }
}
