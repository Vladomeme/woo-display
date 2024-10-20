package net.woo.main.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.woo.main.SituationalDisplay;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

public class WOOConfig {

    public boolean enabled = true;
    public float x = 0.015f;
    public float y = 0.1f;
    public float scale = 1f;
    public boolean shadow = true;
    public int refreshRate = 5;
    public boolean hideOutOfCombat = true;

    public boolean displayPercent = true;
    public boolean showPoise = true;
    public boolean showInure = true;
    public boolean inureDetailedDisplay = true;
    public boolean showShielding = false;
    public boolean showSteadfast = true;
    public boolean showGuard = true;
    public boolean showSecondWind = true;
    public boolean showTempo = true;
    public boolean showReflexes = true;
    public boolean showCloaked = true;
    public boolean showEthereal = true;

    public int textColor = -11141121;
    public int activeColor = -11141291;
    public int halfColor = -171;
    public int inactiveColor = -43691;

    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "woo.json");

    public static final WOOConfig INSTANCE = read();

    public static WOOConfig read() {
        if (!FILE.exists())
            return new WOOConfig().write();

        Reader reader = null;
        try {
            return new Gson().fromJson(reader = new FileReader(FILE), WOOConfig.class);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(reader);
        }
    }
    private void onSave() {
        write();
        SituationalDisplay.updateTexts();
        SituationalDisplay.damageTicks = 0;
    }

    public WOOConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(FILE));
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(this, WOOConfig.class), writer);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(writer);
        }
        return this;
    }

    public Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .save(this::onSave)
                .title(Text.literal("WOO Display"))

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("General"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Enabled"))
                                .binding(true, () -> enabled, newVal -> enabled = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("x"))
                                .description(OptionDescription.of(Text.literal(
                                        "Left side of the screen - 0, right - 1")))
                                .binding(0.015f, () -> x, newVal -> x = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("y"))
                                .description(OptionDescription.of(Text.literal(
                                        "Top of the screen - 0, bottom - 1")))
                                .binding(0.1f, () -> y, newVal -> y = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("Scale"))
                                .description(OptionDescription.of(Text.literal(
                                        "Size of display, default - 1")))
                                .binding(1f, () -> scale, newVal -> scale = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Draw text shadow"))
                                .binding(true, () -> shadow, newVal -> shadow = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Refresh rate"))
                                .description(OptionDescription.of(Text.literal(
                                        "Refresh rate for the more expensive operations")))
                                .binding(5, () -> refreshRate, newVal -> refreshRate = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)
                                        .formatValue(val -> Text.literal(val + " ticks"))).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Hide out of combat"))
                                .description(OptionDescription.of(Text.literal(
                                        "Display will be hidden if damage was not taken in 30 seconds.")))
                                .binding(true, () -> hideOutOfCombat, newVal -> hideOutOfCombat = newVal)
                                .controller(TickBoxControllerBuilder::create).build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Situationals"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Display percent"))
                                .binding(true, () -> displayPercent, newVal -> displayPercent = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Poise"))
                                .binding(true, () -> showPoise, newVal -> showPoise = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Inure"))
                                .binding(true, () -> showInure, newVal -> showInure = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Detailed Inure Display"))
                                .binding(true, () -> inureDetailedDisplay, newVal -> inureDetailedDisplay = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Shielding"))
                                .binding(false, () -> showShielding, newVal -> showShielding = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Steadfast"))
                                .binding(true, () -> showSteadfast, newVal -> showSteadfast = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Guard"))
                                .binding(true, () -> showGuard, newVal -> showGuard = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Second Wind"))
                                .binding(true, () -> showSecondWind, newVal -> showSecondWind = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Tempo"))
                                .binding(true, () -> showTempo, newVal -> showTempo = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Reflexes"))
                                .binding(true, () -> showReflexes, newVal -> showReflexes = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Cloaked"))
                                .binding(true, () -> showCloaked, newVal -> showCloaked = newVal)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Ethereal"))
                                .binding(true, () -> showEthereal, newVal -> showEthereal = newVal)
                                .controller(TickBoxControllerBuilder::create).build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Colours"))

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Text colour"))
                                .binding(new Color(-11141121, true),
                                        () -> new Color(textColor, true), newVal -> textColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Active colour"))
                                .binding(new Color(-11141291, true),
                                        () -> new Color(activeColor, true), newVal -> activeColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Half colour"))
                                .binding(new Color(-171, true),
                                        () -> new Color(halfColor, true), newVal -> halfColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Inactive colour"))
                                .binding(new Color(-43691, true),
                                        () -> new Color(inactiveColor, true), newVal -> inactiveColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}
