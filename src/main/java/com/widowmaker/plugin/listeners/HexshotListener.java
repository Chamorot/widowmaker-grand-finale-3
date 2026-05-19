package com.widowmaker.plugin.listeners;

import com.widowmaker.plugin.WidowmakerPlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;

public class HexshotListener implements Listener {

    private static final String HEXSHOT_ARROW_META  = "widowmaker_hexshot";
    private static final long   COOLDOWN_MS         = 45_000L;
    private static final String CONFIG_COOLDOWN_KEY = "cooldown-expiry-ms";
    private static final long   BOSSBAR_TICK_INTERVAL = 20L;

    private final WidowmakerPlugin plugin;

    private long       cooldownExpiryMs = 0L;
    private BossBar    bossBar          = null;
    private BukkitTask bossBarTask      = null;

    public HexshotListener(WidowmakerPlugin plugin) {
        this.plugin = plugin;
        cooldownExpiryMs = plugin.getConfig().getLong(CONFIG_COOLDOWN_KEY, 0L);
    }

    // -------------------------------------------------------------------------
    // Boss bar helpers
    // -------------------------------------------------------------------------

    private BossBar getOrCreateBossBar() {
        if (bossBar == null) {
            bossBar = BossBar.bossBar(
                Component.text("\u2746 Hexshot Cooldown")
                    .color(NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.BOLD, true),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.NOTCHED_10
            );
        }
        return bossBar;
    }

    private void startBossBar(Player player) {
        if (bossBarTask != null && !bossBarTask.isCancelled()) {
            bossBarTask.cancel();
        }

        BossBar bar = getOrCreateBossBar();
        player.showBossBar(bar);

        bossBarTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now      = System.currentTimeMillis();
            long remaining = cooldownExpiryMs - now;

            if (remaining <= 0) {
                player.hideBossBar(bar);
                bossBarTask.cancel();
                bossBarTask = null;
                player.sendActionBar(
                    Component.text("\u2746 Hexshot ready!")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
                );
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                return;
            }

            long seconds = (remaining + 999) / 1000;
            float progress = Math.min(1.0f, (float) remaining / COOLDOWN_MS);

            bar.name(
                Component.text("\u2746 Hexshot Cooldown \u2014 " + seconds + "s")
                    .color(NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.BOLD, true)
            );
            bar.progress(progress);
            bar.color(progress > 0.4f ? BossBar.Color.BLUE : BossBar.Color.RED);

        }, 0L, BOSSBAR_TICK_INTERVAL);
    }

    private void hideBossBar(Player player) {
        if (bossBar != null) player.hideBossBar(bossBar);
        if (bossBarTask != null && !bossBarTask.isCancelled()) {
            bossBarTask.cancel();
            bossBarTask = null;
        }
    }

    // -------------------------------------------------------------------------
    // Join / Quit
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        cooldownExpiryMs = plugin.getConfig().getLong(CONFIG_COOLDOWN_KEY, 0L);
        Player player = event.getPlayer();
        if (plugin.getOwnerStorage().isClaimed()
                && player.getUniqueId().equals(plugin.getOwnerStorage().getOwnerUuid())
                && cooldownExpiryMs > System.currentTimeMillis()) {
            startBossBar(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hideBossBar(event.getPlayer());
    }

    // -------------------------------------------------------------------------
    // Hexshot: Sneak + Right-click Offhand
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onOffhandInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.OFF_HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        var mainHand = player.getInventory().getItemInMainHand();
        if (!plugin.getWidowmakerItem().isWidowmaker(mainHand)) return;

        event.setCancelled(true);

        long now      = System.currentTimeMillis();
        long remaining = cooldownExpiryMs - now;

        if (remaining > 0) {
            long seconds = (remaining + 999) / 1000;
            player.sendActionBar(
                Component.text("\u2746 Hexshot on cooldown \u2014 " + seconds + "s remaining")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return;
        }

        fireHexshot(player);

        cooldownExpiryMs = now + COOLDOWN_MS;
        plugin.getConfig().set(CONFIG_COOLDOWN_KEY, cooldownExpiryMs);
        plugin.saveConfig();

        startBossBar(player);
    }

    private void fireHexshot(Player player) {
        Arrow arrow = player.launchProjectile(Arrow.class,
            player.getLocation().getDirection().multiply(3.5));
        arrow.setShooter(player);
        arrow.setPierceLevel(2);
        arrow.setMetadata(HEXSHOT_ARROW_META, new FixedMetadataValue(plugin, true));

        player.getWorld().spawnParticle(Particle.WITCH,
            player.getEyeLocation(), 20, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.8f);
        player.getWorld().playSound(player.getLocation(),
            Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 0.9f);

        player.showTitle(Title.title(
            Component.text("\u2746 HEXSHOT")
                .color(NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.BOLD, true),
            Component.text("Arm Decaying Hexshot unleashed!")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false),
            Title.Times.times(
                Duration.ofMillis(100),
                Duration.ofMillis(1200),
                Duration.ofMillis(400)
            )
        ));
    }

    // -------------------------------------------------------------------------
    // On hit — Weakness I for 6 seconds
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!arrow.hasMetadata(HEXSHOT_ARROW_META)) return;

        target.addPotionEffect(new PotionEffect(
            PotionEffectType.WEAKNESS, 200, 0, false, true, true
        ));

        Location hitLoc = target.getLocation().add(0, 1, 0);

        // Original witch particles + sound
        target.getWorld().spawnParticle(Particle.WITCH,
            hitLoc, 30, 0.4, 0.5, 0.4, 0.1);
        target.getWorld().playSound(target.getLocation(),
            Sound.ENTITY_WITHER_AMBIENT, 0.7f, 1.6f);

        // Green skull particle effect
        spawnSkullParticles(hitLoc);
        target.getWorld().playSound(target.getLocation(),
            Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.5f);
        target.getWorld().playSound(target.getLocation(),
            Sound.ENTITY_GENERIC_HURT, 0.6f, 0.4f);

        if (arrow.getShooter() instanceof Player shooter) {
            shooter.sendActionBar(
                Component.text("\u2746 Hexshot struck! Weakness applied.")
                    .color(NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.ITALIC, false)
            );
        }

        arrow.remove();
    }

    // -------------------------------------------------------------------------
    // Green skull particle shape
    // Draws a rough skull: a circle for the cranium, two eye sockets, and a
    // small jaw line — all using green dust particles.
    // -------------------------------------------------------------------------

    private void spawnSkullParticles(Location center) {
        var world = center.getWorld();
        if (world == null) return;

        Particle.DustOptions green      = new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 255, 60),  1.6f);
        Particle.DustOptions darkGreen  = new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 180, 40),  1.2f);

        List<Vector> points = new ArrayList<>();

        // --- Cranium: full circle (radius 0.7, centred 0.4 above center) ---
        int craniumSteps = 28;
        double craniumR  = 0.7;
        double craniumY  = 0.4;
        for (int i = 0; i < craniumSteps; i++) {
            double angle = 2 * Math.PI * i / craniumSteps;
            points.add(new Vector(Math.cos(angle) * craniumR, craniumY, Math.sin(angle) * craniumR));
        }

        // --- Left eye socket: small circle (radius 0.18, offset left) ---
        int eyeSteps = 10;
        double eyeR  = 0.18;
        double eyeY  = 0.55;
        double eyeX  = -0.25;
        for (int i = 0; i < eyeSteps; i++) {
            double angle = 2 * Math.PI * i / eyeSteps;
            points.add(new Vector(eyeX + Math.cos(angle) * eyeR, eyeY, Math.sin(angle) * eyeR));
        }

        // --- Right eye socket: small circle (offset right) ---
        double eyeXR = 0.25;
        for (int i = 0; i < eyeSteps; i++) {
            double angle = 2 * Math.PI * i / eyeSteps;
            points.add(new Vector(eyeXR + Math.cos(angle) * eyeR, eyeY, Math.sin(angle) * eyeR));
        }

        // --- Jaw: bottom arc from -130° to +130° (radius 0.5, below center) ---
        int jawSteps = 14;
        double jawR  = 0.5;
        double jawY  = 0.0;
        double jawStart = Math.toRadians(-130);
        double jawEnd   = Math.toRadians(130);
        for (int i = 0; i <= jawSteps; i++) {
            double angle = jawStart + (jawEnd - jawStart) * i / jawSteps;
            points.add(new Vector(Math.cos(angle) * jawR, jawY, Math.sin(angle) * jawR));
        }

        // --- Teeth: three small vertical stubs along the bottom of the jaw ---
        double[][] teeth = { {-0.3, 0.0}, {0.0, 0.0}, {0.3, 0.0} };
        for (double[] tooth : teeth) {
            points.add(new Vector(tooth[0], jawY - 0.12, tooth[1]));
            points.add(new Vector(tooth[0], jawY - 0.22, tooth[1]));
        }

        // Spawn all outline points as green dust
        for (Vector v : points) {
            world.spawnParticle(Particle.DUST, center.clone().add(v), 1, 0, 0, 0, 0, green);
        }

        // Scatter a burst of darker green dust for depth/glow effect
        world.spawnParticle(Particle.DUST, center.clone().add(0, 0.4, 0),
            18, 0.55, 0.55, 0.55, 0, darkGreen);
    }

}
