package de.javaholic.toolkit.adminworkbench.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import de.javaholic.toolkit.i18n.TextResolver;
import de.javaholic.toolkit.i18n.dto.spi.I18nEntryDtoStore;
import de.javaholic.toolkit.i18n.ui.I18nCrudPanels;
import de.javaholic.toolkit.iam.dto.spi.PermissionFormDtoStore;
import de.javaholic.toolkit.iam.dto.spi.RoleDtoStore;
import de.javaholic.toolkit.iam.dto.spi.UserFormDtoStore;
import de.javaholic.toolkit.iam.ui.IAMCrudPanels;
import de.javaholic.toolkit.persistence.core.CrudStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

@Route("")
public class HomeRoute extends VerticalLayout {

    @Autowired
    ApplicationContext ctx;

    @PostConstruct
    public void debug() {
        System.out.println(Arrays.toString(ctx.getBeanNamesForType(CrudStore.class)));
    }

    public HomeRoute(
            UserFormDtoStore userStore,
            RoleDtoStore roleStore,
            PermissionFormDtoStore permissionStore,
            I18nEntryDtoStore i18nStore,
            TextResolver textResolver
    ) {
        setSizeFull();

        var tabs = new Tabs(
                new Tab("IAM"),
                new Tab("I18N")
        );

        var content = new Div();
        content.setSizeFull();

        add(tabs, content);
        expand(content);

        tabs.addSelectedChangeListener(e -> {
            content.removeAll();
            switch (tabs.getSelectedIndex()) {
                case 0 -> content.add(
                        IAMCrudPanels.createView(userStore, roleStore, permissionStore, textResolver)
                );
                case 1 -> content.add(
                        I18nCrudPanels.createView(i18nStore, textResolver)
                );
            }
        });

        tabs.setSelectedIndex(0);
        content.add(IAMCrudPanels.createView(userStore, roleStore, permissionStore, textResolver));
    }
}
