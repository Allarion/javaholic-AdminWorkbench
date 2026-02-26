package de.javaholic.toolkit.adminworkbench.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.Route;
import de.javaholic.toolkit.persistence.core.CrudStore;
import de.javaholic.toolkit.ui.Buttons;
import de.javaholic.toolkit.ui.Dialogs;
import de.javaholic.toolkit.ui.Grids;
import de.javaholic.toolkit.ui.Inputs;
import de.javaholic.toolkit.ui.crud.CrudPanel;
import de.javaholic.toolkit.ui.crud.CrudPanels;
import de.javaholic.toolkit.ui.crud.action.CrudAction;
import de.javaholic.toolkit.ui.crud.action.CrudPresets;
import de.javaholic.toolkit.ui.form.Forms;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Route("ui-test")
public class UITestPage extends VerticalLayout {

    public UITestPage() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Toolkit Manual UI Test Bench"));
        add(new Paragraph("Manual verification page for toolkit component states, validation flow, dialogs, and CRUD actions."));

        add(
                buildInputsSection(),
                buildNumericInputsSection(),
                buildSelectionInputsSection(),
                buildDateTimeSection(),
                buildButtonsSection(),
                buildFormsSection(),
                buildDialogsSection(),
                buildGridActionsSection()
        );
    }

    private Component buildInputsSection() {
        VerticalLayout section = createSection("1) Inputs");
        Span debug = createDebugLabel();

        TextField requiredPattern = Inputs.textField()
                .label("Required + Pattern")
                .placeholder("ABC-123")
                .error("Format: AAA-999")
                .build();
        requiredPattern.setRequired(true);
        requiredPattern.setPattern("[A-Z]{3}-\\d{3}");

        TextField disabled = Inputs.textField().label("Disabled").build();
        disabled.setValue("disabled value");
        disabled.setEnabled(false);

        TextArea readOnly = Inputs.textArea().label("Read Only").build();
        readOnly.setValue("read-only note");
        readOnly.setReadOnly(true);

        requiredPattern.addValueChangeListener(event -> {
            String value = Optional.ofNullable(event.getValue()).orElse("");
            boolean valid = value.isBlank() || Pattern.matches("[A-Z]{3}-\\d{3}", value);
            requiredPattern.setInvalid(!valid);
            debug.setText("value=" + value + ", required=" + requiredPattern.isRequiredIndicatorVisible() + ", invalid=" + requiredPattern.isInvalid());
        });
        debug.setText("value=<empty>, required=true, invalid=false");

        section.add(requiredPattern, disabled, readOnly, debug);
        return section;
    }

    private Component buildNumericInputsSection() {
        VerticalLayout section = createSection("2) Numeric Inputs");
        Span debug = createDebugLabel();

        NumberField ranged = Inputs.numberField().label("Required + Min/Max").build();
        ranged.setRequiredIndicatorVisible(true);
        ranged.setMin(1);
        ranged.setMax(10);
        ranged.setStep(1);

        NumberField disabled = Inputs.numberField().label("Disabled").build();
        disabled.setValue(3d);
        disabled.setEnabled(false);

        NumberField readOnly = Inputs.numberField().label("Read Only").build();
        readOnly.setValue(5d);
        readOnly.setReadOnly(true);

        ranged.addValueChangeListener(event -> {
            Double value = event.getValue();
            boolean valid = value != null && value >= 1 && value <= 10;
            ranged.setInvalid(!valid);
            debug.setText("value=" + value + ", min=1, max=10, invalid=" + ranged.isInvalid());
        });
        debug.setText("value=null, min=1, max=10, invalid=false");

        section.add(ranged, disabled, readOnly, debug);
        return section;
    }

    private Component buildSelectionInputsSection() {
        VerticalLayout section = createSection("3) Selection Inputs");
        Span debug = createDebugLabel();

        ComboBox<String> required = new ComboBox<>("Required + Validation");
        required.setItems("A", "B", "C");
        required.setRequired(true);

        Select<String> disabled = new Select<>();
        disabled.setLabel("Disabled");
        disabled.setItems("One", "Two");
        disabled.setValue("One");
        disabled.setEnabled(false);

        Select<String> readOnly = new Select<>();
        readOnly.setLabel("Read Only");
        readOnly.setItems("Red", "Blue");
        readOnly.setValue("Red");
        readOnly.setReadOnly(true);

        required.addValueChangeListener(event -> {
            boolean valid = event.getValue() != null;
            required.setInvalid(!valid);
            debug.setText("selected=" + event.getValue() + ", invalid=" + required.isInvalid() + ", required=true");
        });
        debug.setText("selected=null, invalid=false, required=true");

        section.add(required, disabled, readOnly, debug);
        return section;
    }

    private Component buildDateTimeSection() {
        VerticalLayout section = createSection("4) Date/Time");
        Span debug = createDebugLabel();

        DatePicker date = Inputs.datePicker().label("Required Date + Min/Max").build();
        date.setRequired(true);
        date.setMin(LocalDate.now().minusDays(1));
        date.setMax(LocalDate.now().plusDays(30));

        DatePicker disabledDate = Inputs.datePicker().label("Disabled Date").build();
        disabledDate.setValue(LocalDate.now());
        disabledDate.setEnabled(false);

        TimePicker readOnlyTime = new TimePicker("Read Only Time");
        readOnlyTime.setValue(LocalTime.of(9, 30));
        readOnlyTime.setReadOnly(true);

        TimePicker requiredTime = new TimePicker("Required Time");
        requiredTime.setStep(java.time.Duration.ofMinutes(15));
        requiredTime.setRequiredIndicatorVisible(true);

        date.addValueChangeListener(event -> updateDateTimeDebug(debug, date, requiredTime));
        requiredTime.addValueChangeListener(event -> updateDateTimeDebug(debug, date, requiredTime));
        updateDateTimeDebug(debug, date, requiredTime);

        section.add(date, disabledDate, requiredTime, readOnlyTime, debug);
        return section;
    }

    private Component buildButtonsSection() {
        VerticalLayout section = createSection("5) Buttons");
        Span debug = createDebugLabel();

        TextField gate = Inputs.textField().label("Validation Gate (min 3 chars)").build();
        Button guarded = Buttons.create().label("Guarded Action").action(() -> Notification.show("Guarded action executed")).build();
        Button disabled = Buttons.create().label("Disabled Button").build();
        disabled.setEnabled(false);
        Button normal = Buttons.create().label("Normal Button").action(() -> Notification.show("Normal action executed")).build();

        gate.addValueChangeListener(event -> {
            String value = Optional.ofNullable(event.getValue()).orElse("");
            boolean valid = value.length() >= 3;
            guarded.setEnabled(valid);
            debug.setText("gate='" + value + "', guardedEnabled=" + guarded.isEnabled() + ", validation=" + valid);
        });
        guarded.setEnabled(false);
        debug.setText("gate='', guardedEnabled=false, validation=false");

        section.add(gate, new HorizontalLayout(normal, guarded, disabled), debug);
        return section;
    }

    private Component buildFormsSection() {
        VerticalLayout section = createSection("6) Forms");
        Span summary = createDebugLabel();

        Binder<FormDemoModel> binder = new Binder<>(FormDemoModel.class);
        FormDemoModel bean = new FormDemoModel();
        binder.setBean(bean);

        TextField name = Inputs.textField().label("Name (required)").build();
        TextField code = Inputs.textField().label("Code (pattern: DEMO-000)").build();
        NumberField age = Inputs.numberField().label("Age (1..120)").build();
        TextField disabled = Inputs.textField().label("Disabled").build();
        disabled.setValue("disabled");
        disabled.setEnabled(false);
        TextField readOnly = Inputs.textField().label("Read Only").build();
        readOnly.setValue("read-only");
        readOnly.setReadOnly(true);

        binder.forField(name)
                .asRequired("Name is required")
                .bind(FormDemoModel::getName, FormDemoModel::setName);

        binder.forField(code)
                .asRequired("Code is required")
                .withValidator(value -> value != null && value.matches("DEMO-\\d{3}"), "Pattern DEMO-000 required")
                .bind(FormDemoModel::getCode, FormDemoModel::setCode);

        binder.forField(age)
                .asRequired("Age is required")
                .withValidator(value -> value != null && value >= 1 && value <= 120, "Age must be 1..120")
                .bind(FormDemoModel::getAge, FormDemoModel::setAge);

        Button save = Buttons.create().label("Save").action(() -> Notification.show("Form saved")).build();
        save.setEnabled(false);

        binder.addStatusChangeListener(event -> {
            boolean valid = !event.hasValidationErrors() && binder.getBean() != null;
            save.setEnabled(valid);
            List<String> errors = binder.validate().getValidationErrors().stream()
                    .map(ValidationResult::getErrorMessage)
                    .toList();
            summary.setText("valid=" + valid + ", errors=" + errors);
        });

        section.add(name, code, age, disabled, readOnly, save, summary);
        summary.setText("valid=false, errors=[Name is required, Code is required, Age is required]");
        return section;
    }

    private Component buildDialogsSection() {
        VerticalLayout section = createSection("7) Dialogs");
        Span debug = createDebugLabel();

        Button confirm = Buttons.create()
                .label("Confirm Dialog")
                .action(() -> Dialogs.confirm()
                        .header("Confirm")
                        .description("Run confirm dialog test")
                        .confirmLabel("OK")
                        .cancelLabel("Cancel")
                        .open(result -> debug.setText("confirmResult=" + result)))
                .build();

        Button formDialog = Buttons.create()
                .label("Form Dialog")
                .action(() -> {
                    Forms.Form<FormDemoModel> dialogForm = Forms.of(FormDemoModel.class).build();
                    dialogForm.binder().setBean(new FormDemoModel());
                    Dialogs.form(dialogForm)
                            .header("Dialog Form")
                            .description("Validation in dialog")
                            .confirmLabel("Save")
                            .cancelLabel("Cancel")
                            .onOk(f -> debug.setText("dialogSaved=true, bean=" + f.binder().getBean()))
                            .open();
                })
                .build();

        Button disabled = Buttons.create().label("Disabled Trigger").build();
        disabled.setEnabled(false);

        debug.setText("confirmResult=<none>, dialogSaved=false");
        section.add(new HorizontalLayout(confirm, formDialog, disabled), debug);
        return section;
    }

    private Component buildGridActionsSection() {
        VerticalLayout section = createSection("8) Grid & Actions");
        Span debug = createDebugLabel();

        InMemoryCrudStore fullStore = new InMemoryCrudStore(List.of(
                new CrudRow("Alpha", RowStatus.ACTIVE),
                new CrudRow("Beta", RowStatus.DISABLED)
        ));
        Grid<CrudRow> fullGrid = Grids.of(CrudRow.class).build();
        fullGrid.setSelectionMode(Grid.SelectionMode.MULTI);

        CrudPanel<CrudRow> fullPanel = CrudPanels.of(CrudRow.class)
                .withStore(fullStore)
                .withGrid(fullGrid)
                .withForm(Forms.of(CrudRow.class).build())
                .preset(CrudPresets.full())
                .rowAction(CrudAction.row("Toggle Status", row -> {
                    row.setStatus(row.getStatus() == RowStatus.ACTIVE ? RowStatus.DISABLED : RowStatus.ACTIVE);
                    fullStore.save(row);
                    debug.setText("rowAction toggled: " + row.getName() + " -> " + row.getStatus());
                }))
                .selectionAction(CrudAction.selection("Mark Active", selected -> {
                    selected.forEach(row -> {
                        row.setStatus(RowStatus.ACTIVE);
                        fullStore.save(row);
                    });
                    debug.setText("selectionAction updated rows=" + selected.size());
                }))
                .build();

        InMemoryCrudStore readOnlyStore = new InMemoryCrudStore(List.of(
                new CrudRow("Gamma", RowStatus.ACTIVE),
                new CrudRow("Delta", RowStatus.DISABLED)
        ));
        CrudPanel<CrudRow> readOnlyPanel = CrudPanels.of(CrudRow.class)
                .withStore(readOnlyStore)
                .withGrid(Grids.of(CrudRow.class).build())
                .withForm(Forms.of(CrudRow.class).build())
                .preset(CrudPresets.readOnly())
                .build();

        section.add(new H3("Preset: full (with rowAction + selectionAction)"), fullPanel);
        section.add(new H3("Preset: readOnly"), readOnlyPanel);
        section.add(debug);
        debug.setText("gridDebug=<idle>");
        return section;
    }

    private void updateDateTimeDebug(Span debug, DatePicker date, TimePicker time) {
        boolean dateValid = date.getValue() != null
                && !date.getValue().isBefore(date.getMin())
                && !date.getValue().isAfter(date.getMax());
        boolean timeValid = time.getValue() != null;
        date.setInvalid(date.getValue() != null && !dateValid);
        time.setInvalid(!timeValid && time.getValue() == null);
        debug.setText("date=" + date.getValue() + ", dateValid=" + dateValid + ", time=" + time.getValue() + ", timeValid=" + timeValid);
    }

    private VerticalLayout createSection(String title) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.setWidthFull();
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "8px");
        section.add(new H3(title));
        return section;
    }

    private Span createDebugLabel() {
        Span debug = new Span();
        debug.getStyle().set("font-family", "monospace");
        debug.getStyle().set("font-size", "var(--lumo-font-size-s)");
        debug.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return debug;
    }

    public static class DemoModel {
        @NotBlank
        private String name;

        @de.javaholic.toolkit.ui.annotations.UIRequired
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

    public static class FormDemoModel {
        private String name;
        private String code;
        private Double age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Double getAge() {
            return age;
        }

        public void setAge(Double age) {
            this.age = age;
        }
    }

    public enum RowStatus {
        ACTIVE,
        DISABLED
    }

    public static class CrudRow {
        private UUID id;
        private String name;
        private RowStatus status;

        public CrudRow() {
            this.id = UUID.randomUUID();
            this.status = RowStatus.ACTIVE;
        }

        public CrudRow(String name, RowStatus status) {
            this.id = UUID.randomUUID();
            this.name = name;
            this.status = status;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public RowStatus getStatus() {
            return status;
        }

        public void setStatus(RowStatus status) {
            this.status = status;
        }
    }

    private static final class InMemoryCrudStore implements CrudStore<CrudRow, UUID> {
        private final List<CrudRow> rows = new ArrayList<>();

        private InMemoryCrudStore(List<CrudRow> seed) {
            rows.addAll(seed);
        }

        @Override
        public List<CrudRow> findAll() {
            return List.copyOf(rows);
        }

        @Override
        public Optional<CrudRow> findById(UUID id) {
            return rows.stream().filter(row -> row.getId().equals(id)).findFirst();
        }

        @Override
        public CrudRow save(CrudRow entity) {
            rows.removeIf(row -> row.getId().equals(entity.getId()));
            rows.add(entity);
            return entity;
        }

        @Override
        public void delete(CrudRow entity) {
            rows.removeIf(row -> row.getId().equals(entity.getId()));
        }
    }
}
