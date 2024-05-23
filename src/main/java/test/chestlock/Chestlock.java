package test.chestlock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Chestlock extends JavaPlugin implements Listener {

    private final Map<UUID, Block> lockedChests = new HashMap<>();
    private final Map<UUID, Block> openGUIs = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String[] lines = event.getLines();

        if (lines[0].equalsIgnoreCase("[lock]")) {
            Block attachedBlock = block.getRelative(((WallSign) block.getBlockData()).getFacing().getOppositeFace());

            if (attachedBlock.getState() instanceof Container) {
                lockedChests.put(player.getUniqueId(), attachedBlock);

                event.setLine(0, ChatColor.RED + "[Locked]");
                event.setLine(1, ChatColor.GREEN + player.getDisplayName());

            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();

        if (state instanceof Sign) {
            Sign sign = (Sign) state;
            for (String line : sign.getLines()) {
                if (line.equalsIgnoreCase(ChatColor.RED + "[Locked]")) {
                    event.setCancelled(true);
                    return;
                }
            }
        } else if (lockedChests.containsValue(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                for (String line : sign.getLines()) {
                    if (line.equalsIgnoreCase(ChatColor.RED + "[Locked]")) {
                        if (event.getPlayer().isSneaking()) {
                            openUnlockGUI(event.getPlayer(), block);
                        }
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlockState) {
            Block block = ((BlockState) holder).getBlock();
            UUID ownerUUID = null;

            for (Map.Entry<UUID, Block> entry : lockedChests.entrySet()) {
                if (entry.getValue().equals(block)) {
                    ownerUUID = entry.getKey();
                    break;
                }
            }

            if (ownerUUID != null && !event.getPlayer().getUniqueId().equals(ownerUUID)) {
                event.setCancelled(true);
            }
        }
    }

    private void openUnlockGUI(Player player, Block block) {
        Inventory unlockGUI = Bukkit.createInventory(null, 27, "");
        ItemStack redDye = new ItemStack(Material.RED_DYE);
        ItemMeta meta = redDye.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Entsperren");
        redDye.setItemMeta(meta);
        unlockGUI.setItem(13, redDye);
        player.openInventory(unlockGUI);
        openGUIs.put(player.getUniqueId(), block);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.RED_DYE) {
                Block block = openGUIs.get(player.getUniqueId());
                if (block != null) {
                    Block signBlock = findAttachedSign(block);
                    if (signBlock != null) {
                        Sign sign = (Sign) signBlock.getState();
                        for (int i = 0; i < 4; i++) {
                            sign.setLine(i, "");
                        }
                        sign.update();
                    }
                    lockedChests.remove(player.getUniqueId());
                    openGUIs.remove(player.getUniqueId());
                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openGUIs.remove(event.getPlayer().getUniqueId());
    }

    private Block findAttachedSign(Block container) {
        for (BlockFace face : BlockFace.values()) {
            Block relative = container.getRelative(face);
            if (relative.getState() instanceof Sign) {
                Sign sign = (Sign) relative.getState();
                for (String line : sign.getLines()) {
                    if (line.equalsIgnoreCase(ChatColor.RED + "[Locked]")) {
                        return relative;
                    }
                }
            }
        }
        return null;
    }
}
