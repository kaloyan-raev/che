/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.client.project.properties;

import elemental.events.KeyboardEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.PropertiesPagePresenter;
import org.eclipse.che.ide.ui.list.CategoriesList;
import org.eclipse.che.ide.ui.list.Category;
import org.eclipse.che.ide.ui.list.CategoryRenderer;
import org.eclipse.che.ide.ui.window.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The implementation of {@link ProjectPropertiesView}.
 *
 * @author Artem Zatsarynnyi
 * @author Oleksii Orel
 */
@Singleton
public class ProjectPropertiesViewImpl extends Window implements ProjectPropertiesView {

    private static final PropertiesViewImplUiBinder UI_BINDER = GWT.create(PropertiesViewImplUiBinder.class);

    private final ProjectPropertiesResources commandResources;
    private       Button                     cancelButton;
    private       Button                     saveButton;
    private       Button                     closeButton;

    private final CategoryRenderer<PropertiesPagePresenter> projectPropertiesRenderer =
            new CategoryRenderer<PropertiesPagePresenter>() {
                @Override
                public void renderElement(Element element, PropertiesPagePresenter data) {
                    element.setInnerText(data.getTitle());
                }

                @Override
                public SpanElement renderCategory(Category<PropertiesPagePresenter> category) {
                    SpanElement spanElement = Document.get().createSpanElement();
                    spanElement.setClassName(commandResources.getCss().categoryHeader());
                    spanElement.setInnerText(category.getTitle());
                    return spanElement;
                }
            };

    private final Category.CategoryEventDelegate<PropertiesPagePresenter> projectPropertiesDelegate =
            new Category.CategoryEventDelegate<PropertiesPagePresenter>() {
                @Override
                public void onListItemClicked(Element listItemBase, PropertiesPagePresenter itemData) {
                    delegate.onConfigurationSelected(itemData);
                }
            };

    private ActionDelegate                               delegate;
    private CategoriesList                               list;
    private Map<Object, List<Object>> categories;

/*    @UiField(provided = true)
    MachineLocalizationConstant machineLocale;*/
    @UiField
    SimplePanel                 categoriesPanel;
    @UiField
    SimplePanel                 contentPanel;
    @UiField
    FlowPanel                   overFooter;

    @Inject
    protected ProjectPropertiesViewImpl(org.eclipse.che.ide.Resources resources,
                                        JavaLocalizationConstant localization,
                                        ProjectPropertiesResources commandResources,
                                        IconRegistry iconRegistry) {
        this.commandResources = commandResources;

        categories = new HashMap<>();

        commandResources.getCss().ensureInjected();

        setWidget(UI_BINDER.createAndBindUi(this));
        setTitle(localization.projectPropertiesTitle());
        getWidget().getElement().setId("commandsManagerView");

        list = new CategoriesList(resources);
        list.addDomHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                switch (event.getNativeKeyCode()) {
                    case KeyboardEvent.KeyCode.INSERT:
                        break;
                    case KeyboardEvent.KeyCode.DELETE:
                        break;
                }
            }
        }, KeyDownEvent.getType());
        categoriesPanel.add(list);

        contentPanel.clear();

        createButtons();

        getWidget().getElement().getStyle().setPadding(0, Style.Unit.PX);
    }

    private void createButtons() {
        saveButton = createButton("Save", "window-edit-configurations-save", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onSaveClicked();
            }
        });
        saveButton.addStyleName(this.resources.windowCss().primaryButton());
        overFooter.add(saveButton);

        cancelButton = createButton("Cancel", "window-edit-configurations-cancel", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        overFooter.add(cancelButton);

        closeButton = createButton("Close", "window-edit-configurations-close",
                                   new ClickHandler() {
                                       @Override
                                       public void onClick(ClickEvent event) {
                                           delegate.onCloseClicked();
                                       }
                                   });
        closeButton.addDomHandler(new BlurHandler() {
            @Override
            public void onBlur(BlurEvent blurEvent) {
                //set default focus
                /*selectText(filterInputField.getElement());*/
            }
        }, BlurEvent.getType());

        addButtonToFooter(closeButton);

        Element dummyFocusElement = DOM.createSpan();
        dummyFocusElement.setTabIndex(0);
        getFooter().getElement().appendChild(dummyFocusElement);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void close() {
        this.hide();
    }

    @Override
    public AcceptsOneWidget getCommandConfigurationsContainer() {
        return contentPanel;
    }

    @Override
    public void setCancelButtonState(boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    @Override
    public void setSaveButtonState(boolean enabled) {
        saveButton.setEnabled(enabled);
    }

    @Override
    public void setCloseButtonInFocus() {
        closeButton.setFocus(true);
    }

    @Override
    protected void onEnterClicked() {
        delegate.onEnterClicked();
    }

    @Override
    protected void onClose() {
        /*setSelectedConfiguration(selectConfiguration);*/
    }

    @Override
    public boolean isCancelButtonInFocus() {
        return isWidgetFocused(cancelButton);
    }

    @Override
    public boolean isCloseButtonInFocus() {
        return isWidgetFocused(closeButton);
    }

    @Override
    public void setProperties(Map<String, Set<PropertiesPagePresenter>> properties) {
        List<Category<?>> categoriesList = new ArrayList<>();
        for (Map.Entry<String, Set<PropertiesPagePresenter>> entry : properties.entrySet()) {
            categoriesList.add(new Category<>(entry.getKey(),
                                              projectPropertiesRenderer,
                                              entry.getValue(),
                                              projectPropertiesDelegate));
        }

        list.render(categoriesList);
    }

    @Override
    public void selectProperty(PropertiesPagePresenter property) {
        list.selectElement(property);
    }

    interface PropertiesViewImplUiBinder extends UiBinder<Widget, ProjectPropertiesViewImpl> {
    }
}
