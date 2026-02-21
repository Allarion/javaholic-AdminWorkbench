package de.javaholic.toolkit.adminworkbench.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.javaholic.toolkit.ui.Buttons;
import de.javaholic.toolkit.ui.Dialogs;
import de.javaholic.toolkit.ui.Inputs;
import de.javaholic.toolkit.ui.action.Action;
import de.javaholic.toolkit.ui.action.Actions;
import de.javaholic.toolkit.ui.annotations.UIRequired;
import de.javaholic.toolkit.ui.form.Forms;
import de.javaholic.toolkit.ui.form.state.FormState;
import de.javaholic.toolkit.ui.layout.Layouts;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashMap;
import java.util.Map;

@Route("ui-test")
public class UITestPage extends VerticalLayout {

    public UITestPage() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H3("UI Core Fluent API Test Page"));

        Tab reactiveTab = new Tab("Reactive + Actions");
        Tab inputsTab = new Tab("Inputs");
        Tab formsTab = new Tab("Forms");
        Tab dialogsTab = new Tab("Dialogs");

        Tabs tabs = new Tabs(reactiveTab, inputsTab, formsTab, dialogsTab);
        Div content = new Div();
        content.setWidthFull();

        Map<Tab, Component> views = new LinkedHashMap<>();
        views.put(reactiveTab, buildReactiveLiteExample());
        views.put(inputsTab, buildInputsExample());
        views.put(formsTab, buildFormsExample());
        views.put(dialogsTab, buildDialogsExample());

        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            content.add(views.get(event.getSelectedTab()));
        });

        add(tabs, content);
        expand(content);

        tabs.setSelectedTab(reactiveTab);
        content.add(views.get(reactiveTab));
    }

    private Component buildReactiveLiteExample() {
        VerticalLayout demo = Layouts.vbox();
        demo.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        demo.getStyle().set("padding", "var(--lumo-space-m)");

        Forms.Form<DemoModel> form = Forms.of(DemoModel.class).build();
        DemoModel model = new DemoModel();
        form.binder().setBean(model);

        FormState state = Forms.state(form.binder());

        Checkbox busy = Inputs.checkbox().label("Busy").build();
        Checkbox admin = Inputs.checkbox().label("Admin").build();

        busy.addValueChangeListener(event -> state.setSubmitting(Boolean.TRUE.equals(event.getValue())));

        Action saveAction = Actions.create()
                .label("Save")
                .tooltip("Domain validation + UIRequired gate submit")
                .enabledBy(state.canSubmit())
                .onClick(() -> Notification.show("Saved: " + model.getName() + ", age=" + model.getAge()))
                .build();

        Action deleteAction = Actions.create()
                .label("Delete")
                .tooltip("Visible only for admin and not busy")
                .visibleWhen(admin)
                .hiddenWhen(busy)
                .onClick(() -> Notification.show("Delete clicked"))
                .build();

        demo.add(form.layout(), admin, busy);
        demo.add(Layouts.toolbar().action(saveAction).spacer().action(deleteAction).build());
        demo.add(Layouts.menu().item(saveAction).separator().item(deleteAction).build());
        return demo;
    }

    private Component buildInputsExample() {
        VerticalLayout box = Layouts.vbox();
        box.add(new H3("Inputs Fluent Builder"));

        TextField title = Inputs.textField()
                .label("Title")
                .placeholder("Enter title")
                .description("Simple text input")
                .widthFull()
                .build();

        TextArea description = Inputs.textArea()
                .label("Description")
                .placeholder("Long text")
                .widthFull()
                .build();

        Checkbox enabled = Inputs.checkbox().label("Enabled").build();

        box.add(title, description, enabled);
        return box;
    }

    private Component buildFormsExample() {
        VerticalLayout box = Layouts.vbox();
        box.add(new H3("Forms + Buttons Combination"));

        Forms.Form<DemoModel> form = Forms.of(DemoModel.class).build();

        DemoModel model = new DemoModel();
        form.binder().setBean(model);
        FormState state = Forms.state(form.binder());

        Action save = Actions.create()
                .label("Save Form")
                .enabledBy(state.canSubmit())
                .onClick(() -> Notification.show("Form saved: " + model.getName()))
                .build();

        box.add(form.layout(), Buttons.action(save));
        return box;
    }

    private Component buildDialogsExample() {
        VerticalLayout box = Layouts.vbox();
        box.add(new H3("Dialogs Fluent API"));

        Button confirm = Buttons.create()
                .label("Open Confirm Dialog")
                .action(() -> Dialogs.confirm()
                        .header("Confirm")
                        .description("Run fluent confirm dialog?")
                        .confirmLabel("OK")
                        .cancelLabel("Cancel")
                        .open(result -> Notification.show("Confirm result: " + result)))
                .build();

        Button formDialog = Buttons.create()
                .label("Open Form Dialog")
                .action(() -> {
                    Forms.Form<DemoModel> dialogForm = Forms.of(DemoModel.class).build();
                    dialogForm.binder().setBean(new DemoModel());

                    Dialogs.form(dialogForm)
                            .header("Edit")
                            .description("Domain validation in dialog")
                            .confirmLabel("Save")
                            .cancelLabel("Cancel")
                            .onOk(f -> Notification.show("Dialog form saved"))
                            .open();
                })
                .build();

        Grid<String> selectionGrid = new Grid<>();
        selectionGrid.addColumn(item -> item).setHeader("Value");
        selectionGrid.setItems("One", "Two", "Three");
        selectionGrid.setHeight("180px");

        Button selectDialog = Buttons.create()
                .label("Open Select Dialog")
                .action(() -> Dialogs.select(selectionGrid)
                        .header("Select Value")
                        .description("Choose one entry")
                        .confirmLabel("Select")
                        .cancelLabel("Cancel")
                        .open(result -> Notification.show("Selected: " + result.orElse("none"))))
                .build();

        box.add(new HorizontalLayout(confirm, formDialog, selectDialog));
        return box;
    }

    public static class DemoModel {
        @NotBlank
        private String name;

        @UIRequired
        @Min(1)
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}
