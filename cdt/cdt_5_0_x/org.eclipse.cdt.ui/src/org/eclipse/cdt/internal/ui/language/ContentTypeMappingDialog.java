/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.internal.ui.language;

import java.util.HashMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.model.LanguageManager;

import org.eclipse.cdt.internal.ui.preferences.PreferencesMessages;

public abstract class ContentTypeMappingDialog extends Dialog {

	Combo fContentType;
	Combo fLanguage;
	String fSelectedContentTypeName;
	String fSelectedContentTypeID;
	String fSelectedLanguageName;
	String fSelectedLanguageID;
	String fSelectedConfigurationID;
	String fSelectedConfigurationName;
	HashMap<String, String> fContentTypeNamesToIDsMap;
	HashMap<String, String> fLanguageNamesToIDsMap;

	public ContentTypeMappingDialog(Shell parentShell) {
		super(parentShell);
		fContentTypeNamesToIDsMap = new HashMap<String, String>();
		fLanguageNamesToIDsMap = new HashMap<String, String>();
	}

	public String getSelectedContentTypeName() {
		return fSelectedContentTypeName;
	}

	public String getContentTypeID() {
		return fSelectedContentTypeID;
	}

	public String getSelectedLanguageName() {
		return fSelectedLanguageName;
	}

	public String getLanguageID() {
		return fSelectedLanguageID;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(PreferencesMessages.ContentTypeMappingsDialog_title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);

		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	protected String[] getLanguages() {
		ILanguage[] languages = LanguageManager.getInstance()
				.getRegisteredLanguages();
		String[] descriptions = new String[languages.length];
		for (int i = 0; i < descriptions.length; i++) {
			descriptions[i] = languages[i].getName();
			fLanguageNamesToIDsMap.put(descriptions[i], languages[i].getId());
		}
		return descriptions;
	}

	protected abstract boolean isValidSelection();
}
