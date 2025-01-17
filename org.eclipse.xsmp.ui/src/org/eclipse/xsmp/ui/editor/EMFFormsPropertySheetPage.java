/*******************************************************************************
* Copyright (C) 2020-2022 THALES ALENIA SPACE FRANCE.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
******************************************************************************/
package org.eclipse.xsmp.ui.editor;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecp.ui.view.swt.ECPSWTViewRenderer;
import org.eclipse.emf.ecp.view.spi.context.ViewModelContextFactory;
import org.eclipse.emf.ecp.view.spi.swt.masterdetail.BasicDetailViewCache;
import org.eclipse.emf.ecp.view.spi.swt.masterdetail.DetailViewManager;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.ui.provider.ExtendedImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;

class EMFFormsPropertySheetPage implements IPropertySheetPage
{
  private DetailViewManager detailManager;

  ViewForm viewForm;

  CLabel viewMessage;

  private final AdapterFactoryEditingDomain editingDomain;

  public EMFFormsPropertySheetPage(AdapterFactoryEditingDomain editingDomain)
  {
    this.editingDomain = editingDomain;
  }

  public void refresh()
  {

  }

  @Override
  public void createControl(Composite parent)
  {

    viewForm = new ViewForm(parent, SWT.NONE);
    viewForm.marginHeight = 0;
    viewForm.marginWidth = 0;
    viewForm.verticalSpacing = 0;
    viewForm.setBorderVisible(false);
    // toolBar = new ToolBar(viewForm, SWT.FLAT | SWT.WRAP);
    // ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);

    // Image hoverImage = WorkbenchImages.getImage(IWorkbenchGraphicConstants.IMG_LCL_VIEW_MENU);
    // toolItem.setImage(hoverImage);

    // viewForm.setTopRight(toolBar);

    viewMessage = new CLabel(viewForm, SWT.NONE);

    viewMessage.setFont(JFaceResources.getBannerFont());
    viewMessage.setText(""); //$NON-NLS-1$
    viewForm.setTopLeft(viewMessage);

    final var scroll = new ScrolledComposite(viewForm, SWT.BORDER | SWT.V_SCROLL);
    viewForm.setContent(scroll);
    scroll.setLayoutData(new GridData(GridData.FILL_VERTICAL));

    scroll.setAlwaysShowScrollBars(true);
    scroll.setExpandVertical(true);
    scroll.setExpandHorizontal(true);

    scroll.setMinHeight(500);
    scroll.setLayout(new GridLayout(1, false));
    // container = scroll;
    final var container = new Composite(scroll, SWT.NULL);
    final var layout = new GridLayout();
    container.setLayout(layout);
    scroll.setContent(container);

    detailManager = new DetailViewManager(container);

    detailManager.layoutDetailParent(container);
    detailManager.setCache(new BasicDetailViewCache(30));
  }

  @Override
  public void dispose()
  {
    if (detailManager != null)
    {
      detailManager.dispose();
    }
  }

  @Override
  public Control getControl()
  {
    return viewForm;
  }

  @Override
  public void setActionBars(IActionBars actionBars)
  {
  }

  @Override
  public void setFocus()
  {
    detailManager.setFocus();
  }

  protected static Object getSelectedObject(ISelection currentSelection)
  {
    return currentSelection instanceof StructuredSelection
            ? ((StructuredSelection) currentSelection).getFirstElement()
            : null;
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection)
  {
    final var selectedObject = getSelectedObject(selection);

    // clear the current view
    detailManager.cacheCurrentDetail();

    // process the EObject
    if (selectedObject instanceof EObject)
    {

      final var eObject = (EObject) selectedObject;

      final var labelProvider = (IItemLabelProvider) editingDomain.getAdapterFactory()
              .adapt(eObject, IItemLabelProvider.class);
      if (labelProvider != null)
      {
        viewMessage.setText(eObject.eClass().getName() /* +" " + labelProvider.getText(eObject) */);
        viewMessage.setImage(
                ExtendedImageRegistry.getInstance().getImage(labelProvider.getImage(eObject)));
      }
      else
      {
        viewMessage.setText(null);
        viewMessage.setImage(null);
      }

      final var cached = detailManager.getCachedView(eObject);
      // if the view for this eObject is cached, reuse it
      if (cached != null)
      {
        cached.getViewModelContext().getViewModel()
                .setReadonly(editingDomain.isReadOnly(eObject.eResource()));
        detailManager.activate(eObject);
      }
      else
      {
        // create a new view for this eObject
        final var view = detailManager.getDetailView(eObject);
        view.setReadonly(editingDomain.isReadOnly(eObject.eResource()));
        final var modelContext = ViewModelContextFactory.INSTANCE.createViewModelContext(view,
                eObject);
        detailManager.render(modelContext, ECPSWTViewRenderer.INSTANCE::render);
      }
    }
  }

}