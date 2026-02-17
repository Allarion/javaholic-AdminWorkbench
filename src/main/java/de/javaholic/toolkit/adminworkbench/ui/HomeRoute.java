package de.javaholic.toolkit.adminworkbench.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import de.javaholic.toolkit.iam.core.spi.PermissionStore;
import de.javaholic.toolkit.iam.core.spi.RoleStore;
import de.javaholic.toolkit.iam.core.spi.UserStore;
import de.javaholic.toolkit.iam.ui.IAMCrudPanels;
import de.javaholic.toolkit.iam.ui.dto.PermissionDto;
import de.javaholic.toolkit.iam.ui.dto.RoleDto;
import de.javaholic.toolkit.iam.ui.dto.UserDto;
import de.javaholic.toolkit.persistence.core.CrudStore;

import java.util.UUID;


@Route("") // Startseite
public class HomeRoute extends VerticalLayout {


    private final CrudStore<UserDto, UUID> userStore;
    private final CrudStore<RoleDto, UUID> roleStore;
    private final CrudStore<PermissionDto, UUID> permissionStore;

    public HomeRoute(CrudStore<UserDto, UUID> userStore, CrudStore<RoleDto, UUID> roleStore, CrudStore<PermissionDto, UUID> permissionStore) {
        this.userStore = userStore;
        this.roleStore = roleStore;
        this.permissionStore = permissionStore;


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
                case 0 -> content.add(IAMCrudPanels.users(userStore));
                case 1 -> content.add(IAMCrudPanels.roles(roleStore, permissionStore));
                case 2 -> content.add(IAMCrudPanels.permissions(permissionStore));
            }
        });

        tabs.setSelectedIndex(0); // initial
        content.add(IAMCrudPanels.users(userStore));
    }
}
