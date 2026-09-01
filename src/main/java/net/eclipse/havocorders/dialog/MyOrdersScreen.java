package net.eclipse.havocorders.dialog;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.eclipse.havocorders.HavocOrders;
import net.eclipse.havocorders.model.Order;
import net.eclipse.havocorders.util.NumberUtil;
import net.eclipse.havocorders.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyOrdersScreen extends Screen {

    public MyOrdersScreen(HavocOrders plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String configPath() {
        return "MY-ORDERS";
    }

    private int perPage() {
        return Math.max(1, plugin.getConfig().getInt("SETTINGS.ORDERS-PER-PAGE", 8));
    }

    private List<Order> results() {
        return plugin.orders().ordersOf(player.getUniqueId());
    }

    private Map<String, String> screenPlaceholders(List<Order> results) {
        int pages = totalPages(results.size(), perPage());
        Map<String, String> map = new HashMap<>();
        map.put("count", NumberUtil.count(results.size()));
        map.put("collectable", NumberUtil.count(plugin.orders().collectableTotal(player.getUniqueId())));
        map.put("escrow", NumberUtil.money(plugin.orders().escrowHeld(player.getUniqueId())));
        map.put("page", String.valueOf(Math.min(session.getMyOrdersPage() + 1, pages)));
        map.put("pages", String.valueOf(pages));
        return map;
    }

    @Override
    protected Component title() {
        return titleFrom(screenPlaceholders(results()));
    }

    @Override
    protected List<DialogBody> body() {
        List<Order> results = results();
        List<DialogBody> body = Dialogs.body(lines("BODY"), screenPlaceholders(results));
        if (results.isEmpty()) {
            body.add(DialogBody.plainMessage(Text.component(string("EMPTY", "&7No orders."))));
        }
        return body;
    }

    @Override
    protected ActionButton exitButton() {
        return backButton("BACK", screenPlaceholders(results()),
                () -> new OrdersScreen(plugin, player).show());
    }

    @Override
    protected List<ActionButton> buttons() {
        List<Order> results = results();
        Map<String, String> screen = screenPlaceholders(results);
        int pages = totalPages(results.size(), perPage());
        List<ActionButton> buttons = new ArrayList<>();

        for (Order order : slice(results, session.getMyOrdersPage(), perPage())) {
            buttons.add(configButton("ORDER", Placeholders.of(order), (view, audience) -> {
                click();
                new ManageOrderScreen(plugin, player, order.getId()).show();
            }));
        }

        if (session.getMyOrdersPage() > 0) {
            buttons.add(configButton("PREVIOUS", screen, (view, audience) -> {
                session.setMyOrdersPage(session.getMyOrdersPage() - 1);
                click();
                show();
            }));
        }
        if (session.getMyOrdersPage() < pages - 1) {
            buttons.add(configButton("NEXT", screen, (view, audience) -> {
                session.setMyOrdersPage(session.getMyOrdersPage() + 1);
                click();
                show();
            }));
        }

        buttons.add(configButton("NEW-ORDER", screen, (view, audience) -> {
            click();
            session.clearDraft();
            new NewOrderScreen(plugin, player).show();
        }));
        buttons.add(configButton("COLLECT", screen, (view, audience) -> {
            click();
            new CollectScreen(plugin, player).show();
        }));
        return buttons;
    }
}
