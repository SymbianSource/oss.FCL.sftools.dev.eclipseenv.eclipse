/*******************************************************************************
 * Copyright (c) 2000, 2006 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.PreferenceConstants;
import org.eclipse.cdt.ui.text.folding.ICFoldingPreferenceBlock;

import org.eclipse.cdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.cdt.internal.ui.text.folding.CFoldingStructureProviderDescriptor;
import org.eclipse.cdt.internal.ui.text.folding.CFoldingStructureProviderRegistry;
import org.eclipse.cdt.internal.ui.util.PixelConverter;

/**
 * Configures C Editor folding preferences.
 * 
 * @since 3.0
 */
class FoldingConfigurationBlock implements IPreferenceConfigurationBlock {
	
	private static class ErrorPreferences implements ICFoldingPreferenceBlock {
		private String fMessage;
		
		protected ErrorPreferences(String message) {
			fMessage= message;
		}
		
		/*
		 * @see org.eclipse.cdt.internal.ui.text.folding.ICFoldingPreferences#createControl(org.eclipse.swt.widgets.Group)
		 */
		public Control createControl(Composite composite) {
			Composite inner= new Composite(composite, SWT.NONE);
			inner.setLayout(new FillLayout(SWT.VERTICAL));

			Label label= new Label(inner, SWT.CENTER);
			label.setText(fMessage);
			
			return inner;
		}

		public void initialize() {
		}

		public void performOk() {
		}

		public void performDefaults() {
		}

		public void dispose() {
		}
		
	}

	/** The overlay preference store. */
	protected final OverlayPreferenceStore fStore;
	
	/* The controls */
	private Combo fProviderCombo;
	protected Button fFoldingCheckbox;
	private ComboViewer fProviderViewer;
	protected Map<String, CFoldingStructureProviderDescriptor> fProviderDescriptors;
	private Composite fGroup;
	private Map<String, ICFoldingPreferenceBlock> fProviderPreferences;
	private Map<String, Control> fProviderControls;
	private StackLayout fStackLayout;
	

	public FoldingConfigurationBlock(OverlayPreferenceStore store) {
		Assert.isNotNull(store);
		fStore= store;
		fStore.addKeys(createOverlayStoreKeys());
		fProviderDescriptors= createListModel();
		fProviderPreferences= new HashMap<String, ICFoldingPreferenceBlock>();
		fProviderControls= new HashMap<String, Control>();
	}

	private Map<String, CFoldingStructureProviderDescriptor> createListModel() {
		CFoldingStructureProviderRegistry reg= CUIPlugin.getDefault().getFoldingStructureProviderRegistry();
		reg.reloadExtensions();
		CFoldingStructureProviderDescriptor[] descs= reg.getFoldingProviderDescriptors();
		Map<String, CFoldingStructureProviderDescriptor> map= new HashMap<String, CFoldingStructureProviderDescriptor>();
		for (int i= 0; i < descs.length; i++) {
			map.put(descs[i].getId(), descs[i]);
		}
		return map;
	}

	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		ArrayList<OverlayKey> overlayKeys= new ArrayList<OverlayKey>();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_ENABLED));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FOLDING_PROVIDER));
		
		OverlayPreferenceStore.OverlayKey[] keys= new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}

	/*
	 * @see org.eclipse.cdt.internal.ui.preferences.IPreferenceConfigurationBlock#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createControl(Composite parent) {

		Composite composite= new Composite(parent, SWT.NULL);
		// assume parent page uses griddata
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_FILL);
		composite.setLayoutData(gd);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		PixelConverter pc= new PixelConverter(composite);
		layout.verticalSpacing= pc.convertHeightInCharsToPixels(1) / 2;
		composite.setLayout(layout);
		
		
		/* check box for new editors */
		fFoldingCheckbox= new Button(composite, SWT.CHECK);
		fFoldingCheckbox.setText(PreferencesMessages.FoldingConfigurationBlock_enable); 
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		fFoldingCheckbox.setLayoutData(gd);
		fFoldingCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled= fFoldingCheckbox.getSelection(); 
				fStore.setValue(PreferenceConstants.EDITOR_FOLDING_ENABLED, enabled);
				updateCheckboxDependencies();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Label label= new Label(composite, SWT.CENTER);
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);

		/* list */
		Composite comboComp= new Composite(composite, SWT.NONE);
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		GridLayout gridLayout= new GridLayout(2, false);
		gridLayout.marginWidth= 0;
		comboComp.setLayout(gridLayout);
		
		Label comboLabel= new Label(comboComp, SWT.CENTER);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		comboLabel.setLayoutData(gd);
		comboLabel.setText(PreferencesMessages.FoldingConfigurationBlock_combo_caption); 
		
		label= new Label(composite, SWT.CENTER);
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);

		fProviderCombo= new Combo(comboComp, SWT.READ_ONLY | SWT.DROP_DOWN);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
		fProviderCombo.setLayoutData(gd);

		/* list viewer */
		fProviderViewer= new ComboViewer(fProviderCombo);
		fProviderViewer.setContentProvider(new IStructuredContentProvider() {

			/*
			 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
			 */
			public void dispose() {
			}

			/*
			 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
			 */
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			/*
			 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
			 */
			public Object[] getElements(Object inputElement) {
				return fProviderDescriptors.values().toArray();
			}
		});
		fProviderViewer.setLabelProvider(new LabelProvider() {
			/*
			 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
			 */
			@Override
			public Image getImage(Object element) {
				return null;
			}
			
			/*
			 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
			 */
			@Override
			public String getText(Object element) {
				return ((CFoldingStructureProviderDescriptor) element).getName();
			}
		});
		fProviderViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel= (IStructuredSelection) event.getSelection();
				if (!sel.isEmpty()) {
					fStore.setValue(PreferenceConstants.EDITOR_FOLDING_PROVIDER, ((CFoldingStructureProviderDescriptor) sel.getFirstElement()).getId());
					updateListDependencies();
				}
			}
		});
		fProviderViewer.setInput(fProviderDescriptors);
		fProviderViewer.refresh();
		
		Composite groupComp= new Composite(composite, SWT.NONE);
		gd= new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan= 2;
		groupComp.setLayoutData(gd);
		gridLayout= new GridLayout(1, false);
		gridLayout.marginWidth= 0;
		groupComp.setLayout(gridLayout);
		
		/* contributed provider preferences. */
		fGroup= new Composite(groupComp, SWT.NONE);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		fGroup.setLayoutData(gd);
		fStackLayout= new StackLayout();
		fGroup.setLayout(fStackLayout);
		
		return composite;
	}

	protected void updateCheckboxDependencies() {
	}

	void updateListDependencies() {
		String id= fStore.getString(PreferenceConstants.EDITOR_FOLDING_PROVIDER);
		CFoldingStructureProviderDescriptor desc= fProviderDescriptors.get(id);
		ICFoldingPreferenceBlock prefs;
		
		if (desc == null) {
			// safety in case there is no such descriptor
			String message= PreferencesMessages.FoldingConfigurationBlock_error_not_exist; 
			CUIPlugin.log(new Status(IStatus.WARNING, CUIPlugin.getPluginId(), IStatus.OK, message, null));
			prefs= new ErrorPreferences(message);
		} else {
			prefs= fProviderPreferences.get(id);
			if (prefs == null) {
				try {
					prefs= desc.createPreferences();
					fProviderPreferences.put(id, prefs);
				} catch (CoreException e) {
					CUIPlugin.log(e);
					prefs= new ErrorPreferences(e.getLocalizedMessage());
				}
			}
		}
		
		Control control= fProviderControls.get(id);
		if (control == null) {
			control= prefs.createControl(fGroup);
			if (control == null) {
				String message= PreferencesMessages.FoldingConfigurationBlock_info_no_preferences; 
				control= new ErrorPreferences(message).createControl(fGroup);
			} else {
				fProviderControls.put(id, control);
			}
		}
		fStackLayout.topControl= control;
		control.pack();
		fGroup.layout();
		fGroup.getParent().layout();
		
		prefs.initialize();
	}

	/*
	 * @see org.eclipse.cdt.internal.ui.preferences.IPreferenceConfigurationBlock#initialize()
	 */
	public void initialize() {
		restoreFromPreferences();
	}

	/*
	 * @see org.eclipse.cdt.internal.ui.preferences.IPreferenceConfigurationBlock#performOk()
	 */
	public void performOk() {
		for (Iterator<ICFoldingPreferenceBlock> it= fProviderPreferences.values().iterator(); it.hasNext();) {
			ICFoldingPreferenceBlock prefs= it.next();
			prefs.performOk();
		}
	}
	
	/*
	 * @see org.eclipse.cdt.internal.ui.preferences.IPreferenceConfigurationBlock#performDefaults()
	 */
	public void performDefaults() {
		restoreFromPreferences();
		for (Iterator<ICFoldingPreferenceBlock> it= fProviderPreferences.values().iterator(); it.hasNext();) {
			ICFoldingPreferenceBlock prefs= it.next();
			prefs.performDefaults();
		}
	}
	
	/*
	 * @see org.eclipse.cdt.internal.ui.preferences.IPreferenceConfigurationBlock#dispose()
	 */
	public void dispose() {
		for (Iterator<ICFoldingPreferenceBlock> it= fProviderPreferences.values().iterator(); it.hasNext();) {
			ICFoldingPreferenceBlock prefs= it.next();
			prefs.dispose();
		}
	}

	private void restoreFromPreferences() {
		boolean enabled= fStore.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED);
		fFoldingCheckbox.setSelection(enabled);
		updateCheckboxDependencies();
		
		String id= fStore.getString(PreferenceConstants.EDITOR_FOLDING_PROVIDER);
		Object provider= fProviderDescriptors.get(id);
		if (provider != null) {
			fProviderViewer.setSelection(new StructuredSelection(provider), true);
			updateListDependencies();
		}
	}
}

