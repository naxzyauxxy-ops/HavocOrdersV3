package net.eclipse.havocauction.dialog;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.eclipse.havocauction.HavocAuction;
import net.eclipse.havocauction.model.Listing;
import net.eclipse.havocauction.util.ItemNames;
import net.eclipse.havocauction.util.NumberUtil;
import net.eclipse.havocauction.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shows what is inside a shulker box before you buy it. Nobody should have to gamble
 * a few million on a box they cannot see into.
 */
public class ContainerPreviewScreen extends Screen {

    private final UUID listingId;
    private final Runnable onBack;

    public ContainerPreviewScreen(HavocAuction plugin, Player player, UUID listingId, Runnable onBack) {
        super(plugin, player);
        this.listingId = listingId;
        this.onBack = onBack;
    }

    @Override
    protected String configPath() {
        return "CONTAINER-PREVIEW";
    }

    private Listing listing() {
        return plugin.auction().byId(listingId);
    }

    private List<ItemStack> contents(Listing listing) {
        List<ItemStack> contents = new ArrayList<>();
        ItemMeta meta = listing.getItem().getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) return contents;
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox box)) return contents;
        for (ItemStack stack : box.getInventory().getContents()) {
            if (stack != null && stack.getType() != Material.AIR) contents.add(stack);
        }
        return contents;
    }

    @Override
    protected Component title() {
        Listing listing = listing();
        return titleFrom(listing == null ? Map.of() : Placeholders.of(plugin, listing));
    }

    @Override
    protected List<DialogBody> body() {
        Listing listing = listing();
        if (listing == null) {
            return List.of(DialogBody.plainMessage(Text.component(plugin.message("LISTING-UNAVAILABLE"))));
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(itemBody(listing.getItemCopy()));

        List<ItemStack> contents = contents(listing);
        if (contents.isEmpty()) {
            body.add(DialogBody.plainMessage(Text.component(string("EMPTY", "&7This container is empty."))));
            return body;
        }

        String format = string("LINE", "&7- &f{amount}x {item}");
        for (ItemStack stack : contents) {
            body.add(DialogBody.plainMessage(Text.component(Text.apply(format, Map.of(
                    "amount", NumberUtil.count(stack.getAmount()),
                    "item", ItemNames.display(stack))))));
        }
        return body;
    }

    @Override
    protected List<ActionButton> buttons() {
        return List.of(backButton("BACK", Map.of(), onBack));
    }
}
