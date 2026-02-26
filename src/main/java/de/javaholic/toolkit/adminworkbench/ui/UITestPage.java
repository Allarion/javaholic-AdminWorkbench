package de.javaholic.toolkit.adminworkbench.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.javaholic.toolkit.iam.core.api.PermissionChecker;
import de.javaholic.toolkit.introspection.BeanProperty;
import de.javaholic.toolkit.introspection.BeanPropertyTypes;
import de.javaholic.toolkit.persistence.core.CrudStore;
import de.javaholic.toolkit.ui.Buttons;
import de.javaholic.toolkit.ui.Dialogs;
import de.javaholic.toolkit.ui.Grids;
import de.javaholic.toolkit.ui.Inputs;
import de.javaholic.toolkit.ui.action.Actions;
import de.javaholic.toolkit.ui.annotations.UIRequired;
import de.javaholic.toolkit.ui.annotations.UiHidden;
import de.javaholic.toolkit.ui.annotations.UiLabel;
import de.javaholic.toolkit.ui.annotations.UiOrder;
import de.javaholic.toolkit.ui.annotations.UiPermission;
import de.javaholic.toolkit.ui.annotations.UiReadOnly;
import de.javaholic.toolkit.ui.resource.ResourcePanel;
import de.javaholic.toolkit.ui.resource.ResourcePanels;
import de.javaholic.toolkit.ui.resource.action.ResourceAction;
import de.javaholic.toolkit.ui.resource.action.ResourcePresets;
import de.javaholic.toolkit.ui.form.Forms;
import de.javaholic.toolkit.ui.form.fields.FieldContext;
import de.javaholic.toolkit.ui.form.fields.FieldRegistry;
import de.javaholic.toolkit.ui.form.state.FormState;
import de.javaholic.toolkit.ui.meta.UiInspector;
import de.javaholic.toolkit.ui.meta.UiMeta;
import de.javaholic.toolkit.ui.meta.UiProperty;
import de.javaholic.toolkit.ui.state.DerivedState;
import de.javaholic.toolkit.ui.state.MutableState;
import de.javaholic.toolkit.ui.state.ObservableValue;
import de.javaholic.toolkit.ui.state.Trigger;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Route("ui-test")
public class UITestPage extends VerticalLayout {

    public UITestPage() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(buildHeader());
        add(buildPrimitiveBuilderSection());
        add(buildStateSystemSection());
        add(buildAutoMappingSection());
        add(buildBinderContractSection());
        add(buildDialogContractSection());
        add(buildActionSystemSection());
        add(buildGridAndPresetSection());
        add(buildFullIntegrationSection());
    }

    private Component buildHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        header.add(new H2("Toolkit Test Matrix"));
        header.add(new Paragraph("This page is a contract test bench for toolkit DSLs and runtime wiring."));
        header.add(new Paragraph("Each section shows expected behavior, live status output, and debug where useful."));
        return header;
    }

    private Component buildPrimitiveBuilderSection() {
        VerticalLayout section = createSection(
                "SECTION 1 - Primitive Builder Tests",
                "Covers Inputs.* and Buttons builders only.",
                "Expected: builder DSL applies labels/placeholders/themes and button wiring without Forms.auto or Binder."
        );

        TextField name = Inputs.textField().label("primitive.name.label").placeholder("primitive.name.placeholder")
                .description("primitive.name.description").tooltip("primitive.name.tooltip")
                .widthFull().withClassName("primitive-input").build();

        NumberField amount = Inputs.numberField().label("primitive.amount").placeholder("primitive.amount.placeholder").build();
        Checkbox allow = Inputs.checkbox().label("primitive.allow").build();
        allow.setValue(false);

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel("Primitive Builder Debug");

        Button run = Buttons.create().label("Run Primitive Action")
                .enabledWhen(() -> Boolean.TRUE.equals(allow.getValue())).revalidateOn(allow).done()
                .action(() -> setStatus(status, "primitive-action", "clicked with name='" + name.getValue() + "'"))
                .build();

        Button fill = Buttons.create().label("Apply Sample").action(() -> {
            name.setValue("builder-sample");
            amount.setValue(42d);
            setStatus(status, "primitive-fill", "sample values set");
        }).build();

        Runnable refresh = () -> debug.setValue(String.join("\n",
                "textField.label=" + name.getLabel(),
                "textField.placeholder=" + name.getPlaceholder(),
                "textField.classNames=" + name.getClassNames(),
                "numberField.label=" + amount.getLabel(),
                "checkbox.label=" + allow.getLabel(),
                "checkbox.value=" + allow.getValue(),
                "runButton.enabled=" + run.isEnabled()));

        name.addValueChangeListener(e -> refresh.run());
        amount.addValueChangeListener(e -> refresh.run());
        allow.addValueChangeListener(e -> refresh.run());
        refresh.run();
        setStatus(status, "primitive-ready", "builder-only contract initialized");

        section.add(name, amount, allow, new HorizontalLayout(fill, run), debug, status);
        return section;
    }

    private Component buildStateSystemSection() {
        VerticalLayout section = createSection(
                "SECTION 2 - State System Tests",
                "Covers MutableState, DerivedState, Trigger, ObservableValue, and enabledWhen wiring.",
                "Expected: state changes propagate to UI enablement and debug snapshots immediately."
        );

        Checkbox active = Inputs.checkbox().label("state.active").build();
        TextField token = Inputs.textField().label("state.token").build();
        MutableState<Boolean> activeState = MutableState.of(false);
        MutableState<String> tokenState = MutableState.of("");
        Trigger trigger = new Trigger();

        ObservableValue<Boolean> readyState = DerivedState.of(
                () -> Boolean.TRUE.equals(activeState.get()) && tokenState.get() != null && tokenState.get().trim().length() >= 3 && trigger.get() % 2L == 0L,
                activeState, tokenState, trigger
        );

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel("State Debug");

        Button fireTrigger = Buttons.create().label("Fire Trigger").action(() -> {
            trigger.fire();
            setStatus(status, "trigger", "trigger fired");
        }).build();

        Button derivedAction = Buttons.create().label("Derived Action").enabledBy(readyState)
                .action(() -> setStatus(status, "derived-action", "executed")).build();

        Button legacy = Buttons.create().label("enabledWhen Action")
                .enabledWhen(() -> Boolean.TRUE.equals(active.getValue()))
                .revalidateOn(active).revalidateOn(fireTrigger).done()
                .action(() -> setStatus(status, "enabledWhen-action", "executed")).build();

        Runnable refresh = () -> debug.setValue(String.join("\n",
                "activeState=" + activeState.get(),
                "tokenState='" + tokenState.get() + "'",
                "trigger=" + trigger.get(),
                "readyState(derived)=" + readyState.get(),
                "derivedAction.enabled=" + derivedAction.isEnabled(),
                "enabledWhenAction.enabled=" + legacy.isEnabled()));

        active.addValueChangeListener(e -> activeState.set(Boolean.TRUE.equals(e.getValue())));
        token.addValueChangeListener(e -> tokenState.set(e.getValue() == null ? "" : e.getValue()));
        activeState.subscribe(v -> refresh.run());
        tokenState.subscribe(v -> refresh.run());
        trigger.subscribe(v -> refresh.run());
        readyState.subscribe(v -> refresh.run());
        refresh.run();
        setStatus(status, "state-ready", "toggle active/token and fire trigger to observe propagation");

        section.add(active, token, new HorizontalLayout(fireTrigger, derivedAction, legacy), debug, status);
        return section;
    }

    private Component buildAutoMappingSection() {
        VerticalLayout section = createSection(
                "SECTION 3 - Auto Mapping Contract Tests",
                "Covers Forms.auto + UiInspector + FieldRegistry-backed rendering and annotation semantics.",
                "Expected: BeanValidation, UiHidden, UiOrder, UiReadOnly, UiPermission, and UIRequired are reflected in runtime UI + debug."
        );

        Checkbox hasPermission = Inputs.checkbox().label("auto-mapping.has-permission").build();
        hasPermission.setValue(false);
        PermissionChecker checker = permission -> Boolean.TRUE.equals(hasPermission.getValue());

        Forms.Form<AutoMappingContractSpec> form = Forms.auto(AutoMappingContractSpec.class)
                .withTextResolver(this::resolveText)
                .withPermissionChecker(checker)
                .build();
        AutoMappingContractSpec bean = new AutoMappingContractSpec();
        form.binder().setBean(bean);

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel("Auto Mapping Debug");
        UiMeta<AutoMappingContractSpec> meta = UiInspector.inspect(AutoMappingContractSpec.class);

        Runnable refresh = () -> {
            // Re-triggers policy evaluation when simulated permission changes.
            form.binder().setBean(form.binder().getBean());
            refreshAutoMappingStatus(status, form, bean);
            refreshAutoMappingDebug(debug, form, meta);
        };

        hasPermission.addValueChangeListener(e -> refresh.run());
        attachFieldListeners(form, AutoMappingContractSpec.class, refresh);
        refresh.run();

        Button validate = Buttons.create().label("Validate Auto Mapping").action(() -> {
            form.binder().validate();
            refresh.run();
        }).build();

        Button reset = Buttons.create().label("Reset").action(() -> {
            form.binder().setBean(new AutoMappingContractSpec());
            refresh.run();
        }).build();

        section.add(hasPermission, form.layout(), new HorizontalLayout(validate, reset), debug, status);
        return section;
    }

    private Component buildBinderContractSection() {
        VerticalLayout section = createSection(
                "SECTION 4 - Binder Contract Tests",
                "Covers Binder validation lifecycle, save enablement, revalidateOn(...), and statusChangeListener signals.",
                "Expected: save button tracks binder validity without manual polling."
        );

        Forms.Form<BinderContractSpec> form = Forms.of(BinderContractSpec.class).withTextResolver(this::resolveText).build();
        form.binder().setBean(new BinderContractSpec());

        TextField name = (TextField) form.field("name").orElseThrow();
        TextField code = (TextField) form.field("code").orElseThrow();
        Span status = createStatusLabel();
        TextArea debug = createDebugPanel("Binder Debug");
        AtomicInteger statusEvents = new AtomicInteger();

        Runnable refresh = () -> refreshBinderDebug(debug, form, statusEvents.get());
        form.binder().addStatusChangeListener(e -> {
            statusEvents.incrementAndGet();
            refresh.run();
            setStatus(status, "binder-status", "statusChangeListener fired");
        });

        Button validate = Buttons.create().label("Validate Binder").action(() -> {
            form.binder().validate();
            refresh.run();
            setStatus(status, "binder-validate", "manual validation executed");
        }).build();

        Button save = Buttons.create().label("Save").style(ButtonVariant.LUMO_PRIMARY)
                .enabledWhen(() -> form.binder().isValid())
                .revalidateOn(name).revalidateOn(code)
                .revalidateOn(registrar -> form.binder().addStatusChangeListener(e -> registrar.run())).done()
                .action(() -> setStatus(status, "binder-save", "saved bean=" + beanSnapshot(BinderContractSpec.class, form.binder().getBean())))
                .build();

        name.addValueChangeListener(e -> refresh.run());
        code.addValueChangeListener(e -> refresh.run());
        refresh.run();
        setStatus(status, "binder-ready", "edit fields, then validate and save");

        section.add(form.layout(), new HorizontalLayout(validate, save), debug, status);
        return section;
    }

    private Component buildDialogContractSection() {
        VerticalLayout section = createSection(
                "SECTION 5 - Dialog Contract Tests",
                "Covers Dialogs builders for confirm + form dialog and close handling.",
                "Expected: completion callbacks and close behavior are explicit and observable."
        );

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel("Dialog Debug");

        Button openConfirm = Buttons.create().label("Open Confirm Dialog")
                .action(() -> Dialogs.confirm().header("dialog.confirm.title").description("dialog.confirm.desc")
                        .confirmLabel("dialog.ok").cancelLabel("dialog.cancel")
                        .open(confirmed -> {
                            setStatus(status, "confirm", "result=" + confirmed);
                            debug.setValue("confirm completion=" + confirmed);
                        }))
                .build();

        Button openFormDialog = Buttons.create().label("Open Form Dialog").action(() -> {
            Forms.Form<DialogContractSpec> form = Forms.of(DialogContractSpec.class).withTextResolver(this::resolveText).build();
            form.binder().setBean(new DialogContractSpec());
            Dialogs.FormDialog<DialogContractSpec> dialog = Dialogs.form(form)
                    .header("dialog.form.title").description("dialog.form.desc")
                    .confirmLabel("dialog.save").cancelLabel("dialog.cancel")
                    .onOk(f -> {
                        setStatus(status, "form-dialog-ok", "bean=" + beanSnapshot(DialogContractSpec.class, f.binder().getBean()));
                        debug.setValue("form dialog: confirmed");
                    })
                    .onCancel(() -> {
                        setStatus(status, "form-dialog-cancel", "cancel invoked");
                        debug.setValue("form dialog: cancel callback");
                    });
            dialog.dialog().addOpenedChangeListener(e -> {
                if (!e.isOpened()) {
                    setStatus(status, "form-dialog-close", "dialog closed");
                }
            });
            dialog.open();
        }).build();

        setStatus(status, "dialog-ready", "open a dialog and observe completion + close updates");
        section.add(new HorizontalLayout(openConfirm, openFormDialog), debug, status);
        return section;
    }

    private Component buildActionSystemSection() {
        VerticalLayout section = createSection(
                "SECTION 6 - Action System Tests",
                "Covers Actions builder standalone plus toolbarAction/rowAction/selectionAction contracts.",
                "Expected: enableWhen/visibleWhen and CRUD action scopes react consistently."
        );

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel("Action System Debug");
        Checkbox enableStandalone = Inputs.checkbox().label("action.enable-standalone").build();
        Checkbox showStandalone = Inputs.checkbox().label("action.show-standalone").build();
        Checkbox hasActionPermission = Inputs.checkbox().label("action.has-admin-permission").build();

        Button standalone = Buttons.action(Actions.create().label("Standalone Action")
                .enabledWhen(enableStandalone).visibleWhen(showStandalone)
                .onClick(() -> setStatus(status, "standalone-action", "clicked")).build());

        PermissionChecker checker = permission -> Boolean.TRUE.equals(hasActionPermission.getValue());
        var protectedAction = Actions.create()
                .label("Permission Action")
                .permission("perm.admin.edit")
                .onClick(() -> setStatus(status, "permission-action", "clicked"))
                .build();
        HorizontalLayout protectedActionHost = new HorizontalLayout();
        Runnable rebuildProtectedAction = () -> {
            protectedActionHost.removeAll();
            protectedActionHost.add(Buttons.action(protectedAction, checker));
        };
        rebuildProtectedAction.run();

        Grid<ActionMatrixRow> grid = Grids.of(ActionMatrixRow.class)
                .items(List.of(new ActionMatrixRow("A", true), new ActionMatrixRow("B", false)))
                .column(ActionMatrixRow::getName).header("Name").and().build();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        ResourcePanel<ActionMatrixRow> panel = ResourcePanels.of(ActionMatrixRow.class)
                .withStore(new InMemoryCrudStore<>(new ArrayList<>(List.of(new ActionMatrixRow("A", true), new ActionMatrixRow("B", false)))))
                .withGrid(grid)
                .preset(ResourcePresets.none())
                .toolbarAction(ResourceAction.toolbar("Toolbar Action", () -> {
                    setStatus(status, "toolbar-action", "invoked");
                    refreshActionDebug(debug, standalone, protectedActionHost, grid);
                }))
                .rowAction(ResourceAction.<ActionMatrixRow>row("Row Action", row -> {
                    setStatus(status, "row-action", "row=" + row.getName());
                    refreshActionDebug(debug, standalone, protectedActionHost, grid);
                }).enabledWhen(ActionMatrixRow::isEnabled))
                .selectionAction(ResourceAction.<ActionMatrixRow>selection("Selection Action", selection -> {
                    setStatus(status, "selection-action", "selected=" + selection.size());
                    refreshActionDebug(debug, standalone, protectedActionHost, grid);
                }))
                .build();

        Runnable refresh = () -> refreshActionDebug(debug, standalone, protectedActionHost, grid);
        enableStandalone.addValueChangeListener(e -> refresh.run());
        showStandalone.addValueChangeListener(e -> refresh.run());
        hasActionPermission.addValueChangeListener(e -> {
            rebuildProtectedAction.run();
            refresh.run();
        });
        grid.addSelectionListener(e -> refresh.run());
        refresh.run();
        setStatus(status, "action-ready", "toggle standalone states and invoke toolbar/row/selection actions");

        section.add(enableStandalone, showStandalone, hasActionPermission, standalone, protectedActionHost, panel, debug, status);
        return section;
    }

    private Component buildGridAndPresetSection() {
        VerticalLayout section = createSection(
                "SECTION 7 - Grid & Preset Tests",
                "Covers Grids.auto and ResourcePanels.auto with readOnly/full presets.",
                "Expected: preset changes default actions while custom selection actions remain propagated."
        );

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel("Grid & Preset Debug");
        InMemoryCrudStore<GridPresetSpec> readOnlyStore = new InMemoryCrudStore<>(new ArrayList<>(sampleGridPresetRows()));
        InMemoryCrudStore<GridPresetSpec> fullStore = new InMemoryCrudStore<>(new ArrayList<>(sampleGridPresetRows()));

        ResourcePanel<GridPresetSpec> readOnlyPanel = ResourcePanels.auto(GridPresetSpec.class)
                .withStore(readOnlyStore).withTextResolver(this::resolveText).preset(ResourcePresets.readOnly())
                .selectionAction(ResourceAction.selection("ReadOnly Selection", selection -> setStatus(status, "readonly-selection", "selected=" + selection.size())))
                .toolbarAction(ResourceAction.toolbar("ReadOnly Toolbar", () -> setStatus(status, "readonly-toolbar", "toolbar invoked")))
                .build();

        ResourcePanel<GridPresetSpec> fullPanel = ResourcePanels.auto(GridPresetSpec.class)
                .withStore(fullStore).withTextResolver(this::resolveText).preset(ResourcePresets.full())
                .selectionAction(ResourceAction.selection("Full Selection", selection -> setStatus(status, "full-selection", "selected=" + selection.size())))
                .toolbarAction(ResourceAction.toolbar("Full Toolbar", () -> setStatus(status, "full-toolbar", "toolbar invoked")))
                .build();

        Button inspect = Buttons.create().label("Refresh Grid/Preset Debug")
                .action(() -> refreshGridPresetDebug(debug, readOnlyPanel, fullPanel)).build();

        refreshGridPresetDebug(debug, readOnlyPanel, fullPanel);
        setStatus(status, "grid-preset-ready", "use toolbar/create/selection controls and refresh debug snapshot");
        section.add(new HorizontalLayout(readOnlyPanel, fullPanel), inspect, debug, status);
        return section;
    }

    private Component buildFullIntegrationSection() {
        VerticalLayout section = createSection(
                "SECTION 8 - Full Integration Test",
                "Combines Grid + Form + Dialog + State + Actions in one interaction flow.",
                "Expected: selection drives state, actions open dialogs, and binder validity controls save behavior."
        );

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel("Integration Debug");
        List<IntegrationSpec> rows = new ArrayList<>(List.of(new IntegrationSpec("INT-1", "Alpha"), new IntegrationSpec("INT-2", "Beta")));

        Grid<IntegrationSpec> grid = Grids.auto(IntegrationSpec.class).withTextResolver(this::resolveText).build();
        grid.setItems(rows);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        MutableState<IntegrationSpec> selectedState = MutableState.of(null);
        Trigger selectionTrigger = new Trigger();
        ObservableValue<Boolean> hasSelection = DerivedState.of(() -> selectedState.get() != null, selectedState, selectionTrigger);

        grid.addSelectionListener(e -> {
            selectedState.set(e.getFirstSelectedItem().orElse(null));
            selectionTrigger.fire();
            refreshIntegrationDebug(debug, grid, selectedState, hasSelection);
        });

        Button edit = Buttons.action(Actions.create().label("Edit Selected").enabledBy(hasSelection).onClick(() -> {
            IntegrationSpec selected = selectedState.get();
            if (selected == null) {
                return;
            }
            Forms.Form<IntegrationSpec> form = Forms.auto(IntegrationSpec.class).withTextResolver(this::resolveText).build();
            form.binder().setBean(selected);
            FormState state = Forms.state(form.binder());
            Dialogs.form(form).header("integration.dialog.title").description("integration.dialog.desc")
                    .confirmLabel("dialog.save").cancelLabel("dialog.cancel")
                    .onOk(f -> {
                        state.setSubmitting(true);
                        grid.getDataProvider().refreshAll();
                        setStatus(status, "integration-save", "saved=" + beanSnapshot(IntegrationSpec.class, selected));
                        state.setSubmitting(false);
                        refreshIntegrationDebug(debug, grid, selectedState, hasSelection);
                    })
                    .onCancel(() -> setStatus(status, "integration-cancel", "dialog canceled"))
                    .open();
        }).build());

        Button delete = Buttons.action(Actions.create().label("Delete Selected").enabledBy(hasSelection)
                .visibleWhen(() -> !rows.isEmpty())
                .onClick(() -> {
                    IntegrationSpec selected = selectedState.get();
                    if (selected == null) {
                        return;
                    }
                    Dialogs.confirm().header("integration.delete.title").description("integration.delete.desc")
                            .confirmLabel("dialog.ok").cancelLabel("dialog.cancel")
                            .open(confirmed -> {
                                if (!confirmed) {
                                    setStatus(status, "integration-delete", "delete canceled");
                                    return;
                                }
                                rows.remove(selected);
                                selectedState.set(null);
                                selectionTrigger.fire();
                                grid.setItems(rows);
                                setStatus(status, "integration-delete", "deleted selected row");
                                refreshIntegrationDebug(debug, grid, selectedState, hasSelection);
                            });
                }).build());

        refreshIntegrationDebug(debug, grid, selectedState, hasSelection);
        setStatus(status, "integration-ready", "select row, edit via dialog, and test delete confirmation");
        section.add(new HorizontalLayout(edit, delete), grid, debug, status);
        return section;
    }

    private VerticalLayout createSection(String title, String line1, String line2) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.setWidthFull();
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "8px");
        section.add(new H3(title), new Paragraph(line1), new Paragraph(line2));
        return section;
    }

    private TextArea createDebugPanel(String label) {
        TextArea area = Inputs.textArea().label(label).widthFull().build();
        area.setMinHeight("180px");
        area.setReadOnly(true);
        return area;
    }

    private Span createStatusLabel() {
        Span status = new Span();
        status.getStyle().set("font-family", "monospace");
        status.getStyle().set("font-size", "var(--lumo-font-size-s)");
        status.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return status;
    }

    private void setStatus(Span status, String key, String detail) {
        status.setText("status=" + key + " | " + detail);
    }

    private void refreshAutoMappingStatus(Span status, Forms.Form<AutoMappingContractSpec> form, AutoMappingContractSpec bean) {
        boolean valid = form.binder().validate().isOk();
        setStatus(status, valid ? "auto-valid" : "auto-invalid", beanSnapshot(AutoMappingContractSpec.class, bean));
    }

    private void refreshAutoMappingDebug(TextArea debug, Forms.Form<AutoMappingContractSpec> form, UiMeta<AutoMappingContractSpec> meta) {
        FieldRegistry registry = new FieldRegistry();
        Map<String, BeanProperty<AutoMappingContractSpec, ?>> beanProps = meta.beanMeta().properties().stream()
                .collect(LinkedHashMap::new, (map, p) -> map.put(p.name(), p), Map::putAll);

        String lines = meta.properties().sorted(Comparator.comparingInt(UiProperty::order)).map(property -> {
            BeanProperty<AutoMappingContractSpec, ?> beanProperty = beanProps.get(property.name());
            String expectedFieldType = "-";
            if (beanProperty != null) {
                Class<?> elementType = BeanPropertyTypes.resolveCollectionElementType(AutoMappingContractSpec.class, beanProperty);
                FieldContext context = new FieldContext(AutoMappingContractSpec.class, property.name(), beanProperty.type(), elementType, beanProperty.definition());
                expectedFieldType = registry.create(context, property.labelKey(), property.isReadOnly()).getClass().getSimpleName();
            }
            Optional<Component> field = form.field(property.name());
            String rendered = field.map(c -> c.getClass().getSimpleName()).orElse("<hidden>");
            String visible = field.map(Component::isVisible).map(String::valueOf).orElse("false");
            String readOnly = field.filter(HasValue.class::isInstance).map(HasValue.class::cast).map(HasValue::isReadOnly).map(String::valueOf).orElse("-");
            String required = field.filter(HasValueAndElement.class::isInstance).map(HasValueAndElement.class::cast).map(HasValueAndElement::isRequiredIndicatorVisible).map(String::valueOf).orElse("-");

            return property.name() + " -> order=" + property.order()
                    + ", labelKey=" + property.labelKey()
                    + ", required=" + property.isRequired()
                    + ", readOnly=" + property.isReadOnly()
                    + ", hidden=" + property.isHidden()
                    + ", permission=" + property.permissionKey().orElse("-")
                    + ", validators=" + formatValidators(beanProperty != null ? beanProperty.definition() : null)
                    + ", expectedFieldRegistry=" + expectedFieldType
                    + ", rendered=" + rendered
                    + ", visible=" + visible
                    + ", componentReadOnly=" + readOnly
                    + ", requiredIndicator=" + required;
        }).collect(Collectors.joining("\n"));
        debug.setValue(lines);
    }

    private void refreshBinderDebug(TextArea debug, Forms.Form<BinderContractSpec> form, int statusEvents) {
        debug.setValue(String.join("\n",
                "binder.isValid=" + form.binder().isValid(),
                "binder.hasChanges=" + form.binder().hasChanges(),
                "statusChangeListener.events=" + statusEvents,
                "bean=" + beanSnapshot(BinderContractSpec.class, form.binder().getBean())));
    }

    private void refreshActionDebug(TextArea debug, Button standalone, HorizontalLayout protectedActionHost, Grid<ActionMatrixRow> grid) {
        boolean permissionActionEnabled = protectedActionHost.getChildren()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .findFirst()
                .map(Button::isEnabled)
                .orElse(false);
        debug.setValue(String.join("\n",
                "standalone.visible=" + standalone.isVisible(),
                "standalone.enabled=" + standalone.isEnabled(),
                "permissionAction.enabled=" + permissionActionEnabled,
                "selectedRows=" + grid.getSelectedItems().size(),
                "grid.items=" + grid.getDataProvider().size(new com.vaadin.flow.data.provider.Query<>())));
    }

    private void refreshGridPresetDebug(TextArea debug, ResourcePanel<GridPresetSpec> readOnlyPanel, ResourcePanel<GridPresetSpec> fullPanel) {
        boolean readOnlyCreate = findButtonByText(readOnlyPanel, "Create").isPresent();
        boolean fullCreate = findButtonByText(fullPanel, "Create").isPresent();
        debug.setValue(String.join("\n",
                "readOnly.createVisible=" + readOnlyCreate,
                "full.createVisible=" + fullCreate,
                "readOnly.buttonCount=" + countButtons(readOnlyPanel),
                "full.buttonCount=" + countButtons(fullPanel),
                "presetPropagation=custom toolbar/selection actions remain available in both presets"));
    }

    private void refreshIntegrationDebug(TextArea debug, Grid<IntegrationSpec> grid, MutableState<IntegrationSpec> selectedState, ObservableValue<Boolean> hasSelection) {
        debug.setValue(String.join("\n",
                "rows=" + grid.getDataProvider().size(new com.vaadin.flow.data.provider.Query<>()),
                "selected=" + Optional.ofNullable(selectedState.get()).map(IntegrationSpec::getCode).orElse("-"),
                "hasSelection(state)=" + hasSelection.get()));
    }

    private <T> void attachFieldListeners(Forms.Form<T> form, Class<T> type, Runnable onChange) {
        UiInspector.inspect(type).properties().forEach(property -> form.field(property.name()).ifPresent(component -> {
            if (component instanceof HasValue<?, ?> hasValue) {
                hasValue.addValueChangeListener(event -> onChange.run());
            }
        }));
    }

    private Optional<Button> findButtonByText(Component root, String text) {
        if (root instanceof Button button && Objects.equals(button.getText(), text)) {
            return Optional.of(button);
        }
        return root.getChildren().map(child -> findButtonByText(child, text)).flatMap(Optional::stream).findFirst();
    }

    private int countButtons(Component root) {
        int own = root instanceof Button ? 1 : 0;
        return own + root.getChildren().mapToInt(this::countButtons).sum();
    }

    private String formatValidators(AnnotatedElement annotations) {
        if (annotations == null) {
            return "[]";
        }
        List<String> validators = List.of(readNotNull(annotations), readMin(annotations), readMax(annotations), readSize(annotations), readPattern(annotations)).stream()
                .filter(Optional::isPresent).map(Optional::get).toList();
        return validators.isEmpty() ? "[]" : validators.toString();
    }

    private Optional<String> readNotNull(AnnotatedElement annotations) {
        return hasAnnotation(annotations, "jakarta.validation.constraints.NotNull", "javax.validation.constraints.NotNull") ? Optional.of("NotNull") : Optional.empty();
    }

    private Optional<String> readMin(AnnotatedElement annotations) {
        return findAnnotation(annotations, "jakarta.validation.constraints.Min", "javax.validation.constraints.Min")
                .flatMap(annotation -> readLongAttribute(annotation, "value"))
                .map(value -> "Min(" + value + ")");
    }

    private Optional<String> readMax(AnnotatedElement annotations) {
        return findAnnotation(annotations, "jakarta.validation.constraints.Max", "javax.validation.constraints.Max")
                .flatMap(annotation -> readLongAttribute(annotation, "value"))
                .map(value -> "Max(" + value + ")");
    }

    private Optional<String> readSize(AnnotatedElement annotations) {
        Optional<Annotation> size = findAnnotation(annotations, "jakarta.validation.constraints.Size", "javax.validation.constraints.Size");
        Optional<Integer> min = size.flatMap(annotation -> readIntAttribute(annotation, "min"));
        Optional<Integer> max = size.flatMap(annotation -> readIntAttribute(annotation, "max"));
        return min.isPresent() && max.isPresent() ? Optional.of("Size(" + min.get() + ".." + max.get() + ")") : Optional.empty();
    }

    private Optional<String> readPattern(AnnotatedElement annotations) {
        return findAnnotation(annotations, "jakarta.validation.constraints.Pattern", "javax.validation.constraints.Pattern")
                .flatMap(annotation -> readStringAttribute(annotation, "regexp"))
                .map(value -> "Pattern(" + value + ")");
    }

    private boolean hasAnnotation(AnnotatedElement annotations, String... names) {
        for (Annotation annotation : annotations.getAnnotations()) {
            String name = annotation.annotationType().getName();
            for (String candidate : names) {
                if (candidate.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<Annotation> findAnnotation(AnnotatedElement annotations, String... names) {
        for (Annotation annotation : annotations.getAnnotations()) {
            String name = annotation.annotationType().getName();
            for (String candidate : names) {
                if (candidate.equals(name)) {
                    return Optional.of(annotation);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Long> readLongAttribute(Annotation annotation, String attribute) {
        try {
            Object value = annotation.annotationType().getMethod(attribute).invoke(annotation);
            return value instanceof Number number ? Optional.of(number.longValue()) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<Integer> readIntAttribute(Annotation annotation, String attribute) {
        try {
            Object value = annotation.annotationType().getMethod(attribute).invoke(annotation);
            return value instanceof Number number ? Optional.of(number.intValue()) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<String> readStringAttribute(Annotation annotation, String attribute) {
        try {
            Object value = annotation.annotationType().getMethod(attribute).invoke(annotation);
            return value instanceof String text ? Optional.of(text) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private <T> String beanSnapshot(Class<T> type, T bean) {
        if (bean == null) {
            return "{}";
        }
        return UiInspector.inspect(type).properties().sorted(Comparator.comparingInt(UiProperty::order)).map(UiProperty::name)
                .map(name -> name + "=" + readBeanValue(type, bean, name))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private <T> Object readBeanValue(Class<T> type, T bean, String propertyName) {
        return UiInspector.inspect(type).properties().filter(property -> property.name().equals(propertyName)).findFirst().map(property -> property.read(bean)).orElse(null);
    }

    private Optional<String> resolveText(String key, Locale locale) {
        return switch (key) {
            case "primitive.name.label" -> Optional.of("Name");
            case "primitive.name.placeholder" -> Optional.of("Enter a name");
            case "primitive.name.description" -> Optional.of("Primitive input builder test");
            case "primitive.name.tooltip" -> Optional.of("Tooltip from builder DSL");
            case "primitive.amount" -> Optional.of("Amount");
            case "primitive.amount.placeholder" -> Optional.of("0..99");
            case "primitive.allow" -> Optional.of("Allow action");
            case "state.active" -> Optional.of("Active");
            case "state.token" -> Optional.of("Token (min 3)");
            case "auto-mapping.has-permission" -> Optional.of("Simulate permission");
            case "auto.primary.label" -> Optional.of("Primary Value");
            case "auto.secret.label" -> Optional.of("Secret Value");
            case "dialog.confirm.title" -> Optional.of("Confirm Action");
            case "dialog.confirm.desc" -> Optional.of("Confirm to continue");
            case "dialog.form.title" -> Optional.of("Form Dialog");
            case "dialog.form.desc" -> Optional.of("Edit values and confirm");
            case "dialog.save", "dialog.ok" -> Optional.of("OK");
            case "dialog.cancel" -> Optional.of("Cancel");
            case "integration.dialog.title" -> Optional.of("Integration Edit");
            case "integration.dialog.desc" -> Optional.of("Grid + Form + Dialog + State + Actions");
            case "integration.delete.title" -> Optional.of("Delete Selection");
            case "integration.delete.desc" -> Optional.of("Delete currently selected row?");
            default -> Optional.of(key);
        };
    }

    private List<GridPresetSpec> sampleGridPresetRows() {
        return List.of(new GridPresetSpec("A-1", "Alpha", true), new GridPresetSpec("B-2", "Beta", false), new GridPresetSpec("C-3", "Gamma", true));
    }

    public static class AutoMappingContractSpec {
        @UiOrder(10)
        @UiLabel("auto.primary.label")
        private String primary;
        @UiOrder(20)
        @UIRequired
        private String uiRequiredOnly;
        @UiOrder(30)
        @NotNull
        private String notNullOnly;
        @UiOrder(40)
        @Size(min = 2, max = 6)
        @Pattern(regexp = "^[A-Z]+$")
        private String sizePattern;
        @UiOrder(50)
        @Min(1)
        @Max(99)
        private Integer ranged;
        @UiOrder(60)
        @UiReadOnly
        private String readOnly = "readonly";
        @UiOrder(70)
        @UiHidden
        private String hidden = "hidden";
        @UiOrder(80)
        @UiPermission("perm.contract.secret")
        @UiLabel("auto.secret.label")
        private String permissionProtected;

        public String getPrimary() { return primary; }
        public void setPrimary(String primary) { this.primary = primary; }
        public String getUiRequiredOnly() { return uiRequiredOnly; }
        public void setUiRequiredOnly(String uiRequiredOnly) { this.uiRequiredOnly = uiRequiredOnly; }
        public String getNotNullOnly() { return notNullOnly; }
        public void setNotNullOnly(String notNullOnly) { this.notNullOnly = notNullOnly; }
        public String getSizePattern() { return sizePattern; }
        public void setSizePattern(String sizePattern) { this.sizePattern = sizePattern; }
        public Integer getRanged() { return ranged; }
        public void setRanged(Integer ranged) { this.ranged = ranged; }
        public String getReadOnly() { return readOnly; }
        public void setReadOnly(String readOnly) { this.readOnly = readOnly; }
        public String getHidden() { return hidden; }
        public void setHidden(String hidden) { this.hidden = hidden; }
        public String getPermissionProtected() { return permissionProtected; }
        public void setPermissionProtected(String permissionProtected) { this.permissionProtected = permissionProtected; }
    }

    public static class BinderContractSpec {
        @NotBlank
        private String name;
        @Pattern(regexp = "^[A-Z]{2,6}$")
        private String code;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class DialogContractSpec {
        @NotBlank
        private String title;
        @Size(min = 3, max = 20)
        private String value;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class ActionMatrixRow {
        private String name;
        private boolean enabled;

        public ActionMatrixRow() { }
        public ActionMatrixRow(String name, boolean enabled) { this.name = name; this.enabled = enabled; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof ActionMatrixRow that)) { return false; }
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() { return Objects.hash(name); }
    }

    public static class GridPresetSpec {
        @UiOrder(10)
        private String code;
        @UiOrder(20)
        private String label;
        @UiOrder(30)
        private Boolean active;

        public GridPresetSpec() { }
        public GridPresetSpec(String code, String label, Boolean active) { this.code = code; this.label = label; this.active = active; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof GridPresetSpec that)) { return false; }
            return Objects.equals(code, that.code);
        }

        @Override
        public int hashCode() { return Objects.hash(code); }
    }

    public static class IntegrationSpec {
        @UiOrder(10)
        private String code;
        @UiOrder(20)
        @NotBlank
        private String name;

        public IntegrationSpec() { }
        public IntegrationSpec(String code, String name) { this.code = code; this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof IntegrationSpec that)) { return false; }
            return Objects.equals(code, that.code);
        }

        @Override
        public int hashCode() { return Objects.hash(code); }
    }

    private static final class InMemoryCrudStore<T> implements CrudStore<T, Long> {
        private final List<T> entries;

        private InMemoryCrudStore(List<T> entries) {
            this.entries = entries;
        }

        @Override
        public List<T> findAll() {
            return List.copyOf(entries);
        }

        @Override
        public Optional<T> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public T save(T entity) {
            entries.remove(entity);
            entries.add(entity);
            return entity;
        }

        @Override
        public void delete(T entity) {
            entries.remove(entity);
        }
    }
}

