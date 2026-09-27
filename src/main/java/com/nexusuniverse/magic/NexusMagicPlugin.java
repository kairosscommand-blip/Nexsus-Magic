package com.nexusuniverse.magic;

public final class NexusMagicPlugin extends org.bukkit.plugin.java.JavaPlugin {

    private MagicConfig config;
    private WizardLevelManager levels;
    private MagicItemFactory itemFactory;
    private RecipeRegistrar recipeRegistrar;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new MagicConfig(this);
        config.reload();

        this.levels = new WizardLevelManager(this, config);
        this.itemFactory = new MagicItemFactory(this, config);
        this.recipeRegistrar = new RecipeRegistrar(this, config, itemFactory);
        recipeRegistrar.registerAll();

        GroundEffectManager groundEffects = new GroundEffectManager(this);
        CastListener castListener = new CastListener(this, config, itemFactory, levels);
        ImpactListener impactListener = new ImpactListener(config, levels, groundEffects, castListener);
        CombatXpListener combatXpListener = new CombatXpListener(config, levels);

        getServer().getPluginManager().registerEvents(castListener, this);
        getServer().getPluginManager().registerEvents(impactListener, this);
        getServer().getPluginManager().registerEvents(combatXpListener, this);

        getServer().getServicesManager().register(WizardLevelAPI.class, new WizardLevelAPIImpl(config, levels),
                this, org.bukkit.plugin.ServicePriority.Normal);

        var command = getCommand("nexusmagic");
        if (command != null) {
            command.setExecutor(new MagicCommand(config, levels, itemFactory, recipeRegistrar));
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
    }
}
