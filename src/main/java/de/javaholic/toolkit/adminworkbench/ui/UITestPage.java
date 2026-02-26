package de.javaholic.toolkit.adminworkbench.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import de.javaholic.toolkit.ui.Buttons;
import de.javaholic.toolkit.ui.Inputs;
import de.javaholic.toolkit.ui.annotations.UIRequired;
import de.javaholic.toolkit.ui.annotations.UiHidden;
import de.javaholic.toolkit.ui.annotations.UiLabel;
import de.javaholic.toolkit.ui.annotations.UiOrder;
import de.javaholic.toolkit.ui.annotations.UiPermission;
import de.javaholic.toolkit.ui.annotations.UiReadOnly;
import de.javaholic.toolkit.ui.form.Forms;
import de.javaholic.toolkit.ui.meta.UiInspector;
import de.javaholic.toolkit.ui.meta.UiMeta;
import de.javaholic.toolkit.ui.meta.UiProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Route("ui-test")
public class UITestPage extends VerticalLayout {

    public UITestPage() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(buildHeader());
        add(buildInputContractSection());
        add(buildPermissionContractSection());
    }

    private Component buildHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        header.add(new H2("Toolkit Contract Playground"));
        header.add(new Paragraph("This page validates toolkit behavior from annotations to derived metadata to rendered UI behavior."));
        header.add(new Paragraph("Each section lists expected contract behavior and shows actual runtime state from Forms.auto + UiInspector."));
        header.add(new Paragraph("Use Validate/Reset to check binder integration and current bean values without external dependencies."));
        return header;
    }

    private Component buildInputContractSection() {
        VerticalLayout section = createSection("1) InputContractSpec");
        section.add(new Paragraph("Expected: @UiOrder controls order, @UiHidden removes fields, @UiReadOnly locks fields, and Bean Validation/@UIRequired drive required markers + validation."));

        Forms.Form<InputContractSpec> form = Forms.auto(InputContractSpec.class)
                .withTextResolver(this::resolveText)
                .build();
        AtomicReference<InputContractSpec> beanRef = new AtomicReference<>(new InputContractSpec());
        form.binder().setBean(beanRef.get());

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel();
        refreshContractStatus(status, form, InputContractSpec.class, beanRef.get(), "actual");
        refreshDebugPanel(debug, form, UiInspector.inspect(InputContractSpec.class));
        attachFieldListeners(form, InputContractSpec.class, () -> {
            refreshContractStatus(status, form, InputContractSpec.class, beanRef.get(), "actual");
            refreshDebugPanel(debug, form, UiInspector.inspect(InputContractSpec.class));
        });

        Component controls = buildContractControls(
                form,
                InputContractSpec.class,
                beanRef,
                status,
                debug,
                "actual"
        );

        section.add(buildFormAndDebugLayout(form.layout(), debug), controls, status);
        return section;
    }

    private Component buildPermissionContractSection() {
        VerticalLayout section = createSection("2) PermissionContractSpec");
        section.add(new Paragraph("Expected: @UiPermission is reflected in metadata and runtime visibility changes when permission simulation toggles."));

        Forms.Form<PermissionContractSpec> form = Forms.auto(PermissionContractSpec.class)
                .withTextResolver(this::resolveText)
                .build();
        AtomicReference<PermissionContractSpec> beanRef = new AtomicReference<>(new PermissionContractSpec());
        form.binder().setBean(beanRef.get());

        Checkbox hasPermission = Inputs.checkbox().label("Simulate has permission").build();
        hasPermission.setValue(false);

        Span status = createStatusLabel();
        TextArea debug = createDebugPanel();
        UiMeta<PermissionContractSpec> meta = UiInspector.inspect(PermissionContractSpec.class);
        applyPermissionVisibility(form, meta, hasPermission.getValue());
        refreshContractStatus(status, form, PermissionContractSpec.class, beanRef.get(), "actual");
        refreshDebugPanel(debug, form, meta);
        attachFieldListeners(form, PermissionContractSpec.class, () -> {
            refreshContractStatus(status, form, PermissionContractSpec.class, beanRef.get(), "actual");
            refreshDebugPanel(debug, form, meta);
        });
        hasPermission.addValueChangeListener(event -> {
            applyPermissionVisibility(form, meta, Boolean.TRUE.equals(event.getValue()));
            refreshContractStatus(status, form, PermissionContractSpec.class, beanRef.get(), "actual");
            refreshDebugPanel(debug, form, meta);
        });

        Component controls = buildContractControls(
                form,
                PermissionContractSpec.class,
                beanRef,
                status,
                debug,
                "actual"
        );

        section.add(hasPermission, buildFormAndDebugLayout(form.layout(), debug), controls, status);
        return section;
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

    private HorizontalLayout buildFormAndDebugLayout(Component formLayout, TextArea debug) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        formLayout.getElement().getStyle().set("flex", "1");
        debug.getElement().getStyle().set("flex", "1");
        row.add(formLayout, debug);
        return row;
    }

    private <T> Component buildContractControls(
            Forms.Form<T> form,
            Class<T> type,
            AtomicReference<T> beanRef,
            Span status,
            TextArea debug,
            String expectedPrefix
    ) {
        Component validate = Buttons.create().label("Validate").action(() -> {
            var validation = form.binder().validate();
            String result = validation.isOk() ? "OK" : "ERROR";
            status.setText("validation=" + result + " | bean=" + beanSnapshot(type, beanRef.get()) + " | " + expectedPrefix);
            refreshDebugPanel(debug, form, UiInspector.inspect(type));
        }).build();

        Component reset = Buttons.create().label("Reset values").action(() -> {
            try {
                T fresh = type.getDeclaredConstructor().newInstance();
                beanRef.set(fresh);
                form.binder().setBean(fresh);
                refreshContractStatus(status, form, type, beanRef.get(), expectedPrefix);
                refreshDebugPanel(debug, form, UiInspector.inspect(type));
            } catch (Exception ex) {
                status.setText("validation=ERROR | reset failed: " + ex.getMessage());
            }
        }).build();

        HorizontalLayout controls = new HorizontalLayout();
        controls.add(validate, reset);
        return controls;
    }

    private <T> void refreshContractStatus(Span status, Forms.Form<T> form, Class<T> type, T bean, String expectedPrefix) {
        boolean valid = form.binder().validate().isOk();
        status.setText("validation=" + (valid ? "OK" : "ERROR") + " | bean=" + beanSnapshot(type, bean) + " | " + expectedPrefix);
    }

    private <T> void applyPermissionVisibility(Forms.Form<T> form, UiMeta<T> meta, boolean hasPermission) {
        meta.properties().forEach(property -> form.field(property.name()).ifPresent(component -> {
            boolean visible = property.permissionKey().isEmpty() || hasPermission;
            component.setVisible(visible);
        }));
    }

    private <T> void refreshDebugPanel(TextArea debug, Forms.Form<T> form, UiMeta<T> meta) {
        LinkedHashMap<String, AnnotatedElement> definitions = meta.beanMeta().properties().stream()
                .collect(LinkedHashMap::new, (map, p) -> map.put(p.name(), p.definition()), LinkedHashMap::putAll);
        String lines = meta.properties()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .map(property -> {
                    Optional<Component> component = form.field(property.name());
                    String rendered = component.isPresent() ? "yes" : "no";
                    String visible = component.map(Component::isVisible).map(String::valueOf).orElse("-");
                    String labelText = component
                            .filter(HasLabel.class::isInstance)
                            .map(HasLabel.class::cast)
                            .map(HasLabel::getLabel)
                            .orElse("-");
                    String componentReadOnly = component
                            .filter(HasValue.class::isInstance)
                            .map(HasValue.class::cast)
                            .map(HasValue::isReadOnly)
                            .map(String::valueOf)
                            .orElse("-");
                    String validators = formatValidators(definitions.get(property.name()));
                    return property.name()
                            + " -> labelKey=" + property.labelKey()
                            + ", labelText=" + labelText
                            + ", order=" + property.order()
                            + ", required=" + property.isRequired()
                            + ", readOnly=" + property.isReadOnly()
                            + ", hidden=" + property.isHidden()
                            + ", permission=" + property.permissionKey().orElse("-")
                            + ", validators=" + validators
                            + ", rendered=" + rendered
                            + ", visible=" + visible
                            + ", componentReadOnly=" + componentReadOnly;
                })
                .collect(Collectors.joining("\n"));
        debug.setValue(lines);
    }

    private String formatValidators(AnnotatedElement annotations) {
        if (annotations == null) {
            return "[]";
        }
        List<String> validators = List.of(
                        readNotNull(annotations),
                        readMin(annotations),
                        readMax(annotations),
                        readSize(annotations),
                        readPattern(annotations)
                ).stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        return validators.isEmpty() ? "[]" : validators.toString();
    }

    private Optional<String> readNotNull(AnnotatedElement annotations) {
        return hasAnnotation(annotations, "jakarta.validation.constraints.NotNull", "javax.validation.constraints.NotNull")
                ? Optional.of("NotNull")
                : Optional.empty();
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
        if (min.isEmpty() || max.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("Size(" + min.get() + ".." + max.get() + ")");
    }

    private Optional<String> readPattern(AnnotatedElement annotations) {
        return findAnnotation(annotations, "jakarta.validation.constraints.Pattern", "javax.validation.constraints.Pattern")
                .flatMap(annotation -> readStringAttribute(annotation, "regexp"))
                .map(value -> "Pattern(" + value + ")");
    }

    private boolean hasAnnotation(AnnotatedElement annotations, String... annotationTypeNames) {
        for (Annotation annotation : annotations.getAnnotations()) {
            String name = annotation.annotationType().getName();
            for (String candidate : annotationTypeNames) {
                if (candidate.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<Annotation> findAnnotation(AnnotatedElement annotations, String... annotationTypeNames) {
        for (Annotation annotation : annotations.getAnnotations()) {
            String name = annotation.annotationType().getName();
            for (String candidate : annotationTypeNames) {
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

    private Optional<String> resolveText(String key, java.util.Locale locale) {
        return switch (key) {
            case "contract.input.primary.label" -> Optional.of("Primary Value");
            case "contract.permission.admin.label" -> Optional.of("Admin Secret");
            default -> Optional.of(key);
        };
    }

    private TextArea createDebugPanel() {
        TextArea area = Inputs.textArea().label("Contract Debug").widthFull().build();
        area.setMinHeight("260px");
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

    private <T> void attachFieldListeners(Forms.Form<T> form, Class<T> type, Runnable onChange) {
        UiInspector.inspect(type)
                .properties()
                .forEach(property -> form.field(property.name()).ifPresent(component -> {
                    if (component instanceof HasValue<?, ?> hasValue) {
                        hasValue.addValueChangeListener(event -> onChange.run());
                    }
                }));
    }

    private <T> String beanSnapshot(Class<T> type, T bean) {
        if (bean == null) {
            return "{}";
        }
        return UiInspector.inspect(type).properties()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .map(UiProperty::name)
                .map(name -> name + "=" + readBeanValue(type, bean, name))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private <T> Object readBeanValue(Class<T> type, T bean, String propertyName) {
        return UiInspector.inspect(type).properties()
                .filter(property -> property.name().equals(propertyName))
                .findFirst()
                .map(property -> property.read(bean))
                .orElse(null);
    }

    public static class InputContractSpec {
        @UiLabel("contract.input.primary.label")
        @UiOrder(10)
        private String primaryValue;

        @UiOrder(20)
        @UIRequired
        private String uiRequiredOnly;

        @UiOrder(30)
        @NotNull // FIXME doesn't complain when null!
        private String notNullOnly;


        @UiOrder(40)
        @Size(min = 2, max = 6)
        @Pattern(regexp = "^[A-Z]+$")
        // TODO: validation isnt equal. Pattern 'A' fine but @Size(min=2)
        private String sizeAndPattern;

        @UiOrder(50)
        @Min(5)
        @Max(99) // FIXME: no validation error (neither min, max or empty)
        private Integer rangedNumber;

        @UiOrder(60)
        @UiReadOnly
        private String readOnlyText = "readonly";

        @UiOrder(70)
        @UiHidden
        private String hiddenText = "hidden";

        public String getPrimaryValue() {
            return primaryValue;
        }

        public void setPrimaryValue(String primaryValue) {
            this.primaryValue = primaryValue;
        }

        public String getUiRequiredOnly() {
            return uiRequiredOnly;
        }

        public void setUiRequiredOnly(String uiRequiredOnly) {
            this.uiRequiredOnly = uiRequiredOnly;
        }

        public String getNotNullOnly() {
            return notNullOnly;
        }

        public void setNotNullOnly(String notNullOnly) {
            this.notNullOnly = notNullOnly;
        }

        public String getSizeAndPattern() {
            return sizeAndPattern;
        }

        public void setSizeAndPattern(String sizeAndPattern) {
            this.sizeAndPattern = sizeAndPattern;
        }

        public Integer getRangedNumber() {
            return rangedNumber;
        }

        public void setRangedNumber(Integer rangedNumber) {
            this.rangedNumber = rangedNumber;
        }

        public String getReadOnlyText() {
            return readOnlyText;
        }

        public void setReadOnlyText(String readOnlyText) {
            this.readOnlyText = readOnlyText;
        }

        public String getHiddenText() {
            return hiddenText;
        }

        public void setHiddenText(String hiddenText) {
            this.hiddenText = hiddenText;
        }
    }

    public static class PermissionContractSpec {
        @UiOrder(10)
        private String publicValue;

        @UiOrder(20)
        @UiPermission("perm.audit.view")
        private String auditValue;

        @UiOrder(30)
        @UiPermission("perm.admin.edit")
        @UiLabel("contract.permission.admin.label")
        private String adminValue;

        @UiOrder(40)
        private String unrestrictedValue;

        public String getPublicValue() {
            return publicValue;
        }

        public void setPublicValue(String publicValue) {
            this.publicValue = publicValue;
        }

        public String getAuditValue() {
            return auditValue;
        }

        public void setAuditValue(String auditValue) {
            this.auditValue = auditValue;
        }

        public String getAdminValue() {
            return adminValue;
        }

        public void setAdminValue(String adminValue) {
            this.adminValue = adminValue;
        }

        public String getUnrestrictedValue() {
            return unrestrictedValue;
        }

        public void setUnrestrictedValue(String unrestrictedValue) {
            this.unrestrictedValue = unrestrictedValue;
        }
    }
}
