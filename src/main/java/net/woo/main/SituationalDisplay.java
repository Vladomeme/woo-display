package net.woo.main;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.woo.main.config.WOOConfig;

import java.util.*;

public class SituationalDisplay {

    private static final Set<Situationals> equippedSituationals = new HashSet<>();
    private static int refreshTicks = 0;
    private static ClientPlayerEntity player;

    private static boolean reflexesActive = false;
    private static boolean cloakedActive = false;
    private static final InureTypes[] inureTypes = new InureTypes[2];
    private static boolean inureReduced = false;
    public static int damageTicks = 0;
    private static int shieldingCD = 0;
    private static int guardTicks = 0;

    public static void init(MinecraftClient client) {
        player = client.player;
        updateTexts();
    }

    public static void tick() {
        ++damageTicks;
        ++refreshTicks;
        if (shieldingCD > 0) --shieldingCD;
        if (guardTicks > 0) --guardTicks;
        if (refreshTicks >= WOOConfig.INSTANCE.refreshRate) {
            player = MinecraftClient.getInstance().player;
            if (player == null) return;
            boolean hadInure = equippedSituationals.contains(Situationals.Inure);
            equippedSituationals.clear();
            checkEquipped();
            if (hadInure && !equippedSituationals.contains(Situationals.Inure)) inureTypes[0] = inureTypes[1] = null;
            if (equippedSituationals.contains(Situationals.Reflexes) || equippedSituationals.contains(Situationals.Cloaked))
                updateEntityCounts();
            refreshTicks = 0;
        }
    }

    public static void render(DrawContext context) {
        if (WOOConfig.INSTANCE.hideOutOfCombat && damageTicks > 600) return;

        float scale = WOOConfig.INSTANCE.scale;
        int x = (int) (WOOConfig.INSTANCE.x * context.getScaledWindowWidth() / scale);
        int y = (int) (WOOConfig.INSTANCE.y * context.getScaledWindowHeight() / scale);

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, scale);
        for (Situationals line : equippedSituationals) {
            context.drawText(MinecraftClient.getInstance().textRenderer, line.getText(), x, y, 0, WOOConfig.INSTANCE.shadow);
            y += 12;
        }
        context.getMatrices().pop();
    }

    public static void onDamage(DamageSource source) {
        damageTicks = 0;
        updateInure(source);
        updateShielding(source);
    }

    private static void updateInure(DamageSource source) {
        InureTypes type;

        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) type = InureTypes.proj;
        else if (source.isIn(DamageTypeTags.IS_EXPLOSION)) type = InureTypes.blast;
        else if (source.isOf(DamageTypes.MOB_ATTACK)) type = InureTypes.melee;
        else if (source.isOf(DamageTypes.MAGIC)) type = InureTypes.magic;
        else return;

        if (inureTypes[0] != null) {
            if (inureTypes[1] == null) {
                if (type == inureTypes[0]) {
                    inureTypes[1] = type;
                    inureReduced = false;
                }
                else inureTypes[0] = type;
            }
            else if (inureTypes[0] == inureTypes[1]) {
                if (type != inureTypes[1]) {
                    inureTypes[1] = type;
                    inureReduced = true;
                }
            }
            else if (type != inureTypes[0] && type != inureTypes[1]) {
                inureTypes[0] = type;
                inureTypes[1] = null;
            }
        }
        else inureTypes[0] = type;

        if (WOOConfig.INSTANCE.inureDetailedDisplay && inureTypes[1] != null)
            inure_detailed = Text.literal("Inure: ").setStyle(styleText)
                .append(Text.literal(inureTypes[0].name() + ", " + inureTypes[1].name()).setStyle(
                        inureReduced ? styleHalf : styleActive));
    }

    private static void updateShielding(DamageSource source) {
        if (source.getSource() instanceof LivingEntity e && e.disablesShield())
            shieldingCD = 100;
    }

    public static void updateShielding() {
        shieldingCD = 100;
    }

    public static void updateGuard() {
        if (player.getMainHandStack().getItem().equals(Items.SHIELD)) guardTicks = 100;
        else if (player.getOffHandStack().getItem().equals(Items.SHIELD)) guardTicks = 50;
    }

    private static void updateEntityCounts() {
        List<Entity> entities = null;
        if (equippedSituationals.contains(Situationals.Reflexes)) {
            entities = player.clientWorld.getOtherEntities(player,
                            new Box(player.getX() - 8, player.getY() - 8, player.getZ() - 8,
                                    player.getX() + 8, player.getY() + 8, player.getZ() + 8))
                    .stream().filter(SituationalDisplay::typeFilter).toList();
            reflexesActive = entities.size() >= 4;
        }
        if (equippedSituationals.contains(Situationals.Cloaked)) {
            if (entities == null) {
                entities = player.clientWorld.getOtherEntities(player,
                                new Box(player.getX() - 5, player.getY() - 5, player.getZ() - 5,
                                        player.getX() + 5, player.getY() + 5, player.getZ() + 5))
                        .stream().filter(SituationalDisplay::typeFilter).toList();
                cloakedActive = entities.size() <= 2;
            }
            else cloakedActive = entities.stream().filter(SituationalDisplay::distanceFilter).count() <= 2;

        }
    }

    private static boolean typeFilter(Entity entity) {
        return entity instanceof LivingEntity
                && (entity.getScoreboardTeam() == null || entity.getScoreboardTeam().getName().equals("UNPUSHABLE_TEAM"))
                && (!(entity instanceof AnimalEntity) || entity.hasCustomName())
                && !(entity instanceof ArmorStandEntity)
                && !(entity instanceof PlayerEntity)
                && !(entity instanceof MerchantEntity);
    }

    private static boolean distanceFilter(Entity entity) {
        double pX = player.getX();
        double pY = player.getY();
        double pZ = player.getZ();
        double eX = entity.getX();
        double eY = entity.getY();
        double eZ = entity.getZ();
        return eX > pX - 5 && eX < pX + 5 && eY > pY - 5 && eY < pY + 5 && eZ > pZ - 5 && eZ < pZ + 5;
    }

    private enum Situationals {
        Poise("Poise ") {
            public Text getText() {
                float hp = player.getHealth() / player.getMaxHealth();
                return (hp > 0.9f) ? poise_active : ((hp > 0.7f) ? poise_half : poise_inactive);
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showPoise;
            }
        },
        Inure("Inure ") {
            public Text getText() {
                if (inureTypes[1] == null) return inure_inactive;
                return WOOConfig.INSTANCE.inureDetailedDisplay ? inure_detailed :
                        ((inureTypes[0] == inureTypes[1]) && !inureReduced ? inure_active : inure_half);
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showInure;
            }
        },
        Shielding("Shielding ") {
            public Text getText() {
                return shieldingCD == 0 ? shielding_active : shielding_half;
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showShielding;
            }
        },
        Steadfast("Steadfast ") {
            public Text getText() {
                int defence = (int) Math.min((100 - (player.getHealth() / player.getMaxHealth() * 100)) / 3, 20);
                return steadfast.copy().append(Text.literal(defence + "%").setStyle(
                        defence < 6.66f ? styleInactive : (defence < 13.3f ? styleHalf : styleActive)
                ));
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showSteadfast;
            }
        },
        Guard("Guard ") {
            public Text getText() {
                return guardTicks != 0 ? guard_active : guard_inactive;
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showGuard;
            }
        },
        SecondWind("Second Wind ") {
            public Text getText() {
                return player.getHealth() / player.getMaxHealth() < 0.5 ? sWind_active : sWind_inactive;
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showSecondWind;
            }
        },
        Tempo("Tempo ") {
            public Text getText() {
                return damageTicks > 80 ? tempo_active : (damageTicks > 40 ? tempo_half : tempo_inactive);
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showTempo;
            }
        },
        Reflexes("Reflexes ") {
            public Text getText() {
                return reflexesActive ? reflexes_active : reflexes_inactive;
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showReflexes;
            }
        },
        Cloaked("Cloaked ") {
            public Text getText() {
                return cloakedActive ? cloaked_active : cloaked_inactive;
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showCloaked;
            }
        },
        Ethereal("Ethereal ") {
            public Text getText() {
                return damageTicks > 30 ? ethereal_inactive : ethereal_active;
            }

            public boolean shouldDisplay() {
                return WOOConfig.INSTANCE.showEthereal;
            }
        };

        public final String name;

        Situationals(String name) {
            this.name = name;
        }

        public abstract Text getText();
        public abstract boolean shouldDisplay();
    }

    private static void checkEquipped() {
        player.getArmorItems().forEach(SituationalDisplay::checkLore);
        checkLore(player.getMainHandStack());
        checkLore(player.getOffHandStack());
    }

    private static void checkLore(ItemStack stack) {
        stack.getTooltip(player, TooltipContext.BASIC).forEach(line -> {
            for (Situationals enchant : Situationals.values()) {
                if (!enchant.shouldDisplay()) continue;
                if (line.getString().contains(enchant.name)) equippedSituationals.add(enchant);
            }
        });
    }

    public static void updateTexts() {
        styleText = Style.EMPTY.withColor(WOOConfig.INSTANCE.textColor);
        styleActive = Style.EMPTY.withColor(WOOConfig.INSTANCE.activeColor);
        styleHalf = Style.EMPTY.withColor(WOOConfig.INSTANCE.halfColor);
        styleInactive = Style.EMPTY.withColor(WOOConfig.INSTANCE.inactiveColor);
        boolean displayPercent = WOOConfig.INSTANCE.displayPercent;

        poise_inactive = Text.literal("Poise: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));
        poise_active = Text.literal("Poise: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "20%" : "active").setStyle(styleActive));
        poise_half = Text.literal("Poise: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "10%" : "active").setStyle(styleHalf));

        inure_inactive = Text.literal("Inure: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));
        inure_active = Text.literal("Inure: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "20%" : "active").setStyle(styleActive));
        inure_half = Text.literal("Inure: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "10%" : "active").setStyle(styleHalf));

        shielding_active = Text.literal("Shielding: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "20%" : "active").setStyle(styleActive));
        shielding_half = Text.literal("Shielding: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "10%" : "active").setStyle(styleHalf));

        steadfast = Text.literal("Steadfast: ").setStyle(styleText);

        sWind_active = Text.literal("Second Wind: ").setStyle(styleText)
                .append(Text.literal("active").setStyle(styleActive));
        sWind_inactive = Text.literal("Second Wind: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));

        guard_active = Text.literal("Guard: ").setStyle(styleText)
                .append(Text.literal("active").setStyle(styleActive));
        guard_inactive = Text.literal("Guard: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));

        tempo_inactive = Text.literal("Tempo: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));
        tempo_active = Text.literal("Tempo: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "20%" : "active").setStyle(styleActive));
        tempo_half = Text.literal("Tempo: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "10%" : "active").setStyle(styleHalf));

        reflexes_active = Text.literal("Reflexes: ").setStyle(styleText)
                .append(Text.literal("active").setStyle(styleActive));
        reflexes_inactive = Text.literal("Reflexes: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));

        cloaked_active = Text.literal("Cloaked: ").setStyle(styleText)
                .append(Text.literal("active").setStyle(styleActive));
        cloaked_inactive = Text.literal("Cloaked: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));

        ethereal_active = Text.literal("Ethereal: ").setStyle(styleText)
                .append(Text.literal("active").setStyle(styleActive));
        ethereal_inactive = Text.literal("Ethereal: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));
    }

    private static Style styleText = Style.EMPTY;
    private static Style styleActive = Style.EMPTY;
    private static Style styleHalf = Style.EMPTY;
    private static Style styleInactive = Style.EMPTY;

    private static Text poise_inactive = Text.empty();
    private static Text poise_active = Text.empty();
    private static Text poise_half = Text.empty();

    private static Text inure_inactive = Text.empty();
    private static Text inure_active = Text.empty();
    private static Text inure_half = Text.empty();
    private static Text inure_detailed = Text.empty();

    private static Text shielding_active = Text.empty();
    private static Text shielding_half = Text.empty();

    private static Text steadfast = Text.empty();

    private static Text sWind_active = Text.empty();
    private static Text sWind_inactive = Text.empty();

    private static Text guard_active = Text.empty();
    private static Text guard_inactive = Text.empty();

    private static Text tempo_inactive = Text.empty();
    private static Text tempo_active = Text.empty();
    private static Text tempo_half = Text.empty();

    private static Text reflexes_active = Text.empty();
    private static Text reflexes_inactive = Text.empty();

    private static Text cloaked_active = Text.empty();
    private static Text cloaked_inactive = Text.empty();

    private static Text ethereal_active = Text.empty();
    private static Text ethereal_inactive = Text.empty();
}
