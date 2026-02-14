package de.javaholic.toolkit.adminworkbench.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import de.javaholic.toolkit.iam.core.spi.PermissionStore;
import de.javaholic.toolkit.iam.core.spi.RoleStore;
import de.javaholic.toolkit.iam.core.spi.UserStore;
import de.javaholic.toolkit.iam.ui.IamPanels;
import de.javaholic.toolkit.iam.ui.adapter.PermissionCrudStoreAdapter;
import de.javaholic.toolkit.iam.ui.adapter.RoleCrudStoreAdapter;
import de.javaholic.toolkit.iam.ui.adapter.UserCrudStoreAdapter;

@Route("") // Startseite
public class HomeRoute extends VerticalLayout {

    private final UserCrudStoreAdapter userStore;
    private final RoleCrudStoreAdapter roleStore;
    private final PermissionCrudStoreAdapter permissionStore;

    public HomeRoute(UserStore userStore, RoleStore roleStore, PermissionStore permStore) {
        this.userStore = new UserCrudStoreAdapter(userStore);
        this.roleStore = new RoleCrudStoreAdapter(roleStore);
        this.permissionStore = new PermissionCrudStoreAdapter(permStore);
        setSizeFull();
        initContent();
    }

    private void initContent() {
        var tabs = new Tabs(
                new Tab("Users"), //
                new Tab("Roles"), //
                new Tab("Permissions"));

        var content = new Div();
        content.setSizeFull();
        add(tabs, content);
        expand(content);

        tabs.addSelectedChangeListener(e -> {
            content.removeAll();
            switch (tabs.getSelectedIndex()) {
                case 0 -> content.add(IamPanels.users(userStore, roleStore));
                case 1 -> content.add(IamPanels.roles(roleStore, permissionStore));
                case 2 -> content.add(IamPanels.permissions(permissionStore));
            }
        });

        tabs.setSelectedIndex(0); // initial
        content.add(IamPanels.users(userStore, roleStore));
    }
}
