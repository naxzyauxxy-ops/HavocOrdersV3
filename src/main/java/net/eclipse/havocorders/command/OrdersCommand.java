package net.eclipse.havocorders.command;

import net.eclipse.havocorders.HavocOrders;
import net.eclipse.havocorders.dialog.OrdersScreen;
import net.eclipse.havocorders.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One entry point. Everything else lives inside the dialogs, so there are no
 * sub-commands for your orders, collecting, or selling.
 */
public class OrdersCommand implements CommandExecutor, TabCompleter {

    private final HavocOrders plugin;

    public OrdersCommand(HavocOrders plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("havocorders.admin")) {
                sender.sendMessage(Text.component(plugin.message("NO-PERMISSION")));
                return true;
            }
            plugin.reloadEverything();
            sender.sendMessage(Text.component(plugin.message("RELOADED")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.component(plugin.message("PLAYERS-ONLY")));
            return true;
        }
        if (!player.hasPermission("havocorders.use")) {
            player.sendMessage(Text.component(plugin.message("NO-PERMISSION")));
            return true;
        }
        if (!plugin.economy().isReady()) {
            player.sendMessage(Text.component(plugin.message("NO-ECONOMY")));
            return true;
        }

        new OrdersScreen(plugin, player).show();
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("havocorders.admin")) {
            options.add("reload");
            options.removeIf(option -> !option.startsWith(args[0].toLowerCase(Locale.ROOT)));
        }
        return options;
    }
}
