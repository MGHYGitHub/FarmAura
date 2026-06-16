/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.addons.farm;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;

public class FarmAddon extends MeteorAddon {
    @Override
    public void onInitialize() {
        // 注册模块
        Modules.get().add(new FarmAura());
        System.out.println("[FarmAddon] Farm Aura loaded!");
    }

    @Override
    public String getPackage() {
        return "meteordevelopment.meteorclient.addons.farm";
    }

    // 移除 @Override 注解，因为父类可能没有这个方法
    public ModMetadata getMetadata() {
        return FabricLoader.getInstance().getModContainer("meteor-farm-addon").orElseThrow().getMetadata();
    }

    // 移除 @Override 注解，因为父类可能没有这个方法
    public String getWebsite() {
        return "https://github.com/yourusername/farm-addon";
    }
}
