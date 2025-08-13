package net.woo.main;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
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

import java.awt.*;
import java.util.*;
import java.util.List;

public class SituationalDisplay extends HudElement {

    public static final SituationalDisplay INSTANCE = new SituationalDisplay();
    private static final WOOConfig config = WOOConfig.INSTANCE;
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static ClientPlayerEntity player;

    private static final List<TextLine> texts = new ArrayList<>();
    private static int refreshTicks = 0;

    public static int damageTicks = 0;
    private static int shieldingCD = 0;
    private static int guardTicks = 0;
    private static int reflexesTicks = 0;
    private static int cloakedTicks = 0;
    private static int cloakedStatus = 0;

    private static int width = 150;
    private static int height = 12;
    private double dragXRelative;
    private double dragYRelative;
    private double dragXAbsolute;
    private double dragYAbsolute;

    public static void init() {
        player = client.player;
        updateTexts();
    }

    public static void tick() {
        ++damageTicks;
        ++refreshTicks;
        if (shieldingCD > 0) --shieldingCD;
        if (guardTicks > 0) --guardTicks;
        if (reflexesTicks > 0) --reflexesTicks;
        if (cloakedTicks > 0) --cloakedTicks;

        if (refreshTicks >= config.refreshRate) {
            player = client.player;
            if (player == null) return;

            Set<Situationals> active = checkEquipped();

            if (!active.contains(Situationals.Inure)) resetInure();
            if (active.contains(Situationals.Cloaked)) updateCloakedStatus();

            texts.clear();
            if (config.rightAlignment) {
                int maxLength = 0;
                for (Situationals situational : active) {
                    Text text = situational.getText();
                    int length = client.textRenderer.getWidth(text);
                    if (length > maxLength) maxLength = length;
                    texts.add(new TextLine(text, length));
                }
                for (TextLine line : texts) line.pos = maxLength - line.pos;
            }
            else {
                for (Situationals situational : active) texts.add(new TextLine(situational.getText(), 0));
            }
            refreshTicks = 0;
        }
    }
    
    public static boolean shouldRender() {
        return !config.hideOutOfCombat || damageTicks <= config.outOfCombatTime * 20;
    }

    @Override
    protected void render(DrawContext context, float delta) {
        int y = 0;
        float scale = config.scale;

        context.getMatrices().scale(scale, scale, scale);
        for (TextLine line : texts) {
            context.drawText(client.textRenderer, line.text, line.pos, y, 0, config.shadow);
            y += 12;
        }
    }

    @Override
    public void renderTooltip(Screen screen, DrawContext drawContext, int mouseX, int mouseY) {
        if (!dragging) drawContext.drawTooltip(client.textRenderer, Text.of("Hold ctrl to move"), mouseX, mouseY);
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

        inure_text = Text.literal("Inure: ").setStyle(styleText).append(Text.literal(type.name()).setStyle(styleActive));
    }

    private static void resetInure() {
        inure_text = Text.literal("Inure: ").setStyle(styleText).append(Text.literal("inactive").setStyle(styleInactive));
    }

    private static void updateShielding(DamageSource source) {
        if (source.getSource() instanceof LivingEntity e && e.disablesShield())
            shieldingCD = 100;
    }

    public static void updateShielding() {
        shieldingCD = 100;
    }

    public static void updateGuard() {
        if (player.getMainHandStack().getItem().equals(Items.SHIELD)) guardTicks = 120;
        else if (player.getOffHandStack().getItem().equals(Items.SHIELD)) guardTicks = 80;
    }

    public static void updateGuardOnDamage() {
        guardTicks = 80;
    }

    private static void updateCloakedStatus() {
        List<Entity> entities = player.clientWorld.getOtherEntities(player,
                        new Box(player.getX() - 5, player.getY() - 5, player.getZ() - 5,
                                player.getX() + 5, player.getY() + 5, player.getZ() + 5))
                .stream().filter(SituationalDisplay::typeFilter).toList();
        cloakedStatus = entities.size() <= 2 ? 2 : entities.size() == 3 ? 1 : 0;
    }

    public static void updateCloakedOnKill() {
        cloakedTicks = 120;
    }

    public static void updateReflexes() {
        reflexesTicks = 13;
    }

    private static boolean typeFilter(Entity entity) {
        return entity instanceof LivingEntity
                && (entity.getScoreboardTeam() == null || entity.getScoreboardTeam().getName().equals("UNPUSHABLE_TEAM"))
                && (!(entity instanceof AnimalEntity) || entity.hasCustomName())
                && !(entity instanceof ArmorStandEntity)
                && !(entity instanceof PlayerEntity)
                && !(entity instanceof MerchantEntity);
    }

    private enum Situationals {
        Poise("Poise ") {
            public Text getText() {
                float hp = player.getHealth() / player.getMaxHealth();
                return (hp > 0.9f) ? poise_active : ((hp > 0.7f) ? poise_half : poise_inactive);
            }

            public boolean shouldDisplay() {
                return config.showPoise;
            }
        },
        Inure("Inure ") {
            public Text getText() {
                return inure_text;
            }

            public boolean shouldDisplay() {
                return config.showInure;
            }
        },
        Shielding("Shielding ") {
            public Text getText() {
                return shieldingCD == 0 ? shielding_active : shielding_half;
            }

            public boolean shouldDisplay() {
                return config.showShielding;
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
                return config.showSteadfast;
            }
        },
        Guard("Guard ") {
            public Text getText() {
                return guardTicks > 0 ? guard_active : guard_inactive;
            }

            public boolean shouldDisplay() {
                return config.showGuard;
            }
        },
        SecondWind("Second Wind ") {
            public Text getText() {
                return player.getHealth() / player.getMaxHealth() < 0.5 ? sWind_active : sWind_inactive;
            }

            public boolean shouldDisplay() {
                return config.showSecondWind;
            }
        },
        Tempo("Tempo ") {
            public Text getText() {
                return damageTicks > 80 ? tempo_active : (damageTicks > 50 ? tempo_half : tempo_inactive);
            }

            public boolean shouldDisplay() {
                return config.showTempo;
            }
        },
        Reflexes("Reflexes ") {
            public Text getText() {
                return reflexesTicks > 0 ? reflexes_active : reflexes_inactive;
            }

            public boolean shouldDisplay() {
                return config.showReflexes;
            }
        },
        Cloaked("Cloaked ") {
            public Text getText() {
                if (cloakedTicks > 0) return cloaked_active;
                return cloakedStatus == 2 ? cloaked_active : cloakedStatus == 1 ? cloaked_half : cloaked_inactive;
            }

            public boolean shouldDisplay() {
                return config.showCloaked;
            }
        },
        Ethereal("Ethereal ") {
            public Text getText() {
                return damageTicks > 30 ? ethereal_inactive : ethereal_active;
            }

            public boolean shouldDisplay() {
                return config.showEthereal;
            }
        };

        public final String name;

        Situationals(String name) {
            this.name = name;
        }

        public abstract Text getText();
        public abstract boolean shouldDisplay();
    }

    private static Set<Situationals> checkEquipped() {
        Set<Situationals> situationals = new HashSet<>();
        player.getArmorItems().forEach(itemStack -> checkLore(situationals, itemStack));
        checkLore(situationals, player.getMainHandStack());
        checkLore(situationals, player.getOffHandStack());

        width = (int) (150 * config.scale);
        height = (int) (situationals.size() * 12 * config.scale);
        return situationals;
    }

    private static void checkLore(Set<Situationals> situationals, ItemStack stack) {
        stack.getTooltip(player, TooltipContext.BASIC).forEach(line -> {
            for (Situationals enchant : Situationals.values()) {
                if (!enchant.shouldDisplay()) continue;
                if (line.getString().contains(enchant.name)) situationals.add(enchant);
            }
        });
    }

    public static void updateTexts() {
        styleText = Style.EMPTY.withColor(config.textColor);
        styleActive = Style.EMPTY.withColor(config.activeColor);
        styleHalf = Style.EMPTY.withColor(config.halfColor);
        styleInactive = Style.EMPTY.withColor(config.inactiveColor);
        boolean displayPercent = config.displayPercent;

        poise_inactive = Text.literal("Poise: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));
        poise_active = Text.literal("Poise: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "100%" : "active").setStyle(styleActive));
        poise_half = Text.literal("Poise: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "50%" : "active").setStyle(styleHalf));

        inure_text = Text.literal("Inure: ").setStyle(styleText).append(Text.literal("inactive").setStyle(styleInactive));

        shielding_active = Text.literal("Shielding: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "100%" : "active").setStyle(styleActive));
        shielding_half = Text.literal("Shielding: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "50%" : "active").setStyle(styleHalf));

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
                .append(Text.literal(displayPercent ? "100%" : "active").setStyle(styleActive));
        tempo_half = Text.literal("Tempo: ").setStyle(styleText)
                .append(Text.literal(displayPercent ? "50%" : "active").setStyle(styleHalf));

        reflexes_active = Text.literal("Reflexes: ").setStyle(styleText)
                .append(Text.literal("active").setStyle(styleActive));
        reflexes_inactive = Text.literal("Reflexes: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));

        cloaked_inactive = Text.literal("Cloaked: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));
        cloaked_active = Text.literal("Cloaked: ").setStyle(styleText)
                .append(Text.literal("100%").setStyle(styleActive));
        cloaked_half = Text.literal("Cloaked: ").setStyle(styleText)
                .append(Text.literal("50%").setStyle(styleHalf));

        ethereal_active = Text.literal("Ethereal: ").setStyle(styleText)
                .append(Text.literal("active").setStyle(styleActive));
        ethereal_inactive = Text.literal("Ethereal: ").setStyle(styleText)
                .append(Text.literal("inactive").setStyle(styleInactive));
    }
    
    @Override
    protected boolean isEnabled() {
        return config.enabled;
    }

    @Override
    protected boolean isVisible() {
        return true;
    }

    @Override
    protected int getWidth() {
        return width;
    }

    @Override
    protected int getHeight() {
        return height;
    }

    @Override
    protected ElementPosition getPosition() {
        return config.textPosition;
    }

    @Override
    public Rectangle getDimension() {
        ElementPosition position = getPosition();
        int width = getWidth();
        int height = getHeight();
        int x = Math.round((float) client.getWindow().getScaledWidth() * position.offsetXRelative + (float) position.offsetXAbsolute - position.alignX * (float) width);
        int y = Math.round((float) client.getWindow().getScaledHeight() * position.offsetYRelative + (float) position.offsetYAbsolute);
        if (config.effectPadding && !(client.currentScreen instanceof ChatScreen)) {
            for (StatusEffectInstance effect : Objects.requireNonNull(client.player).getStatusEffects()) {
                if (effect.shouldShowIcon()) y += config.effectPaddingSize;
            }
        }
        return new Rectangle(x, y, width, height);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!dragging) return false;

        Rectangle dimension = getDimension();
        ElementPosition position = getPosition();
        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();
        double newX = mouseX + (double) dimension.x - dragXRelative;
        double newY = mouseY + (double) dimension.y - dragYRelative;

        double horizontalMiddle = Math.abs(newX + (double) dimension.width / (double) 2.0F - (double) scaledWidth / (double) 2.0F);
        double right = (double) scaledWidth - (newX + (double) dimension.width);
        double verticalMiddle = Math.abs(newY + (double) dimension.height / (double) 2.0F - (double) scaledHeight / (double) 2.0F);
        double bottom = (double) scaledHeight - (newY + (double) dimension.height);
        position.offsetXRelative = newX < horizontalMiddle && newX < right ? 0.0F : (horizontalMiddle < right ? 0.5F : 1.0F);
        position.offsetYRelative = newY < verticalMiddle && newY < bottom ? 0.0F : (verticalMiddle < bottom ? 0.5F : 1.0F);
        position.alignX = position.offsetXRelative;
        position.alignY = 0;
        position.offsetXAbsolute = (int) Math.round(newX - (double) ((float) scaledWidth * position.offsetXRelative) + (double) (position.alignX * (float) dimension.width));
        position.offsetYAbsolute = (int) Math.round(newY - (double) ((float) scaledHeight * position.offsetYRelative));
        if (!Screen.hasAltDown()) {
            if (position.offsetXRelative == 0.5F && Math.abs(position.offsetXAbsolute) < 10) {
                position.offsetXAbsolute = 0;
            }

            if (position.offsetYRelative == 0.5F && Math.abs(position.offsetYAbsolute) < 10) {
                position.offsetYAbsolute = 0;
            }
        }
        return true;
    }

    @Override
    protected int getZOffset() {
        return 1;
    }

    private static Style styleText = Style.EMPTY;
    private static Style styleActive = Style.EMPTY;
    private static Style styleHalf = Style.EMPTY;
    private static Style styleInactive = Style.EMPTY;

    private static Text poise_inactive = Text.empty();
    private static Text poise_active = Text.empty();
    private static Text poise_half = Text.empty();

    private static Text inure_text = Text.empty();

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
    private static Text cloaked_half = Text.empty();

    private static Text ethereal_active = Text.empty();
    private static Text ethereal_inactive = Text.empty();

    static class TextLine {
        final Text text;
        int pos;

        TextLine(Text text, int pos) {
            this.text = text;
            this.pos = pos;
        }
    }
}
