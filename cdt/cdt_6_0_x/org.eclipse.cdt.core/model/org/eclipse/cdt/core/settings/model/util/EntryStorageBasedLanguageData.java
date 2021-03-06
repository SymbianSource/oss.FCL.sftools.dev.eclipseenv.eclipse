/*******************************************************************************
 * Copyright (c) 2007 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.settings.model.util;

import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.impl.CDefaultLanguageData;

public abstract class EntryStorageBasedLanguageData extends CDefaultLanguageData {

	public EntryStorageBasedLanguageData() {
		super();
	}

	public EntryStorageBasedLanguageData(String id, CLanguageData base) {
		super(id, base);
	}

	public EntryStorageBasedLanguageData(String id, String languageId,
			String[] ids, boolean isContentTypes) {
		super(id, languageId, ids, isContentTypes);
	}

	@Override
	public ICLanguageSettingEntry[] getEntries(int kind) {
		AbstractEntryStorage storage = getStorage(kind);
		if(storage != null){
			List list = storage.getEntries(null);
			return (ICLanguageSettingEntry[])list.toArray(new ICLanguageSettingEntry[list.size()]); 
		}
		return new ICLanguageSettingEntry[0];
	}

	@Override
	public void setEntries(int kind, ICLanguageSettingEntry[] entries) {
		AbstractEntryStorage storage = getStorage(kind);
		if(storage != null){
			storage.setEntries(entries);
		}
	}
	
	protected void setEntriesToStore(int kind, ICLanguageSettingEntry[] entries){
		fStore.storeEntries(kind, entries);
	}

	protected ICLanguageSettingEntry[] getEntriesFromStore(int kind){
		return fStore.getEntries(kind);
	}
	
	@Override
	protected EntryStore createStore(){
		return new EntryStore(false);
	}

	protected abstract AbstractEntryStorage getStorage(int kind);

}
