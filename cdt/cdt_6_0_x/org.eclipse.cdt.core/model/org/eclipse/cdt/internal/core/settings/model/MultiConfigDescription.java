/*******************************************************************************
 * Copyright (c) 2007, 2008 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.internal.core.settings.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.settings.model.CConfigurationStatus;
import org.eclipse.cdt.core.settings.model.ICBuildSetting;
import org.eclipse.cdt.core.settings.model.ICConfigExtensionReference;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExternalSetting;
import org.eclipse.cdt.core.settings.model.ICFileDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingContainer;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingObject;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.ICTargetPlatformSetting;
import org.eclipse.cdt.core.settings.model.MultiItemsHolder;
import org.eclipse.cdt.core.settings.model.WriteAccessException;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;

/**
 * This class represents multi-configuration description holder
 */
public class MultiConfigDescription extends MultiItemsHolder implements
		ICMultiConfigDescription {

	ICConfigurationDescription[] fCfgs = null;

	public MultiConfigDescription(ICConfigurationDescription[] des) {
		fCfgs = des;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.internal.core.settings.model.MultiItemsHolder#getItems()
	 */
	@Override
	public Object[] getItems() {
		return fCfgs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#create(java.lang.String, java.lang.String)
	 */
	public ICConfigExtensionReference create(String extensionPoint,
			String extension) throws CoreException {
		System.out.println("Bad multi access: MultiConfigDescription.create()");
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#createExternalSetting(java.lang.String[], java.lang.String[], java.lang.String[], org.eclipse.cdt.core.settings.model.ICSettingEntry[])
	 */
	public ICExternalSetting createExternalSetting(String[] languageIDs,
			String[] contentTypeIds, String[] extensions,
			ICSettingEntry[] entries) throws WriteAccessException {
		System.out.println("Bad multi access: MultiConfigDescription.createExtSett()");
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#createFileDescription(org.eclipse.core.runtime.IPath, org.eclipse.cdt.core.settings.model.ICResourceDescription)
	 */
	public ICFileDescription createFileDescription(IPath path,
			ICResourceDescription base) throws CoreException,
			WriteAccessException {
		System.out.println("Bad multi access: MultiConfigDescription.createFileDesc()");		
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#createFolderDescription(org.eclipse.core.runtime.IPath, org.eclipse.cdt.core.settings.model.ICFolderDescription)
	 */
	public ICFolderDescription createFolderDescription(IPath path,
			ICFolderDescription base) throws CoreException,
			WriteAccessException {
		System.out.println("Bad multi access: MultiConfigDescription.createFolderDesc()");		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#get(java.lang.String)
	 */
	public ICConfigExtensionReference[] get(String extensionPointID) {
		System.out.println("Bad multi access: MultiConfigDescription.get()");		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getBuildSetting()
	 */
	public ICBuildSetting getBuildSetting() {
		System.out.println("Bad multi access: MultiConfigDescription.getBuildSetting()");
		return null;
	}

	public String[][] getErrorParserIDs() {
		String[][] out = new String[fCfgs.length][]; 
		for (int i=0; i<fCfgs.length; i++)
			out[i] = fCfgs[i].getBuildSetting().getErrorParserIDs();
		return out;
	}

	public void setErrorParserIDs(String[] ids) {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].getBuildSetting().setErrorParserIDs(ids);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getBuildSystemId()
	 */
	public String getBuildSystemId() {
		return fCfgs[0].getBuildSystemId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getBuildVariablesContributor()
	 */
	public ICdtVariablesContributor getBuildVariablesContributor() {
		return fCfgs[0].getBuildVariablesContributor();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getConfigurationData()
	 */
	public CConfigurationData getConfigurationData() {
		System.out.println("Bad multi access: MultiConfigDescription.getCfgData()");
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getConfigurationStatus()
	 */
	public CConfigurationStatus getConfigurationStatus() {
		CConfigurationStatus st = null;
		for (int i=1; i<fCfgs.length; i++) {
			st = fCfgs[0].getConfigurationStatus();
			if (! st.isOK())
				return st; // report error in any cfg
		}
		return st;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getDescription()
	 */
	public String getDescription() {
		return "Multi Configuration"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getExternalSettings()
	 */
	public ICExternalSetting[] getExternalSettings() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getExternalSettingsProviderIds()
	 */
	public String[] getExternalSettingsProviderIds() {
		System.out.println("Bad multi access: MultiConfigDescription.getExtSettProviderIds()");
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getFileDescriptions()
	 */
	public ICFileDescription[] getFileDescriptions() {
		ArrayList<ICFileDescription> lst = new ArrayList<ICFileDescription>();
		for (int i=0; i<fCfgs.length; i++) 
			lst.addAll(Arrays.asList(fCfgs[i].getFileDescriptions()));
		return (ICFileDescription[])lst.toArray(new ICFileDescription[lst.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getFolderDescriptions()
	 */
	public ICFolderDescription[] getFolderDescriptions() {
		ArrayList<ICFolderDescription> lst = new ArrayList<ICFolderDescription>();
		for (int i=0; i<fCfgs.length; i++) 
			lst.addAll(Arrays.asList(fCfgs[i].getFolderDescriptions()));
		return (ICFolderDescription[])lst.toArray(new ICFolderDescription[lst.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getLanguageSettingForFile(org.eclipse.core.runtime.IPath, boolean)
	 */
	public ICLanguageSetting getLanguageSettingForFile(IPath path,
			boolean ignoreExludeStatus) {
		System.out.println("Bad multi access: MultiConfigDescription.getLangSettForFile()");
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getProjectDescription()
	 */
	public ICProjectDescription getProjectDescription() {
		ICProjectDescription pd = fCfgs[0].getProjectDescription();
		if (pd == null)
			return null;
		for (int i=1; i<fCfgs.length; i++)
			if (! pd.equals(fCfgs[i].getProjectDescription()))
				return null; // Different projects ! 
		return pd;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getReferenceInfo()
	 */
	public Map<String, String> getReferenceInfo() {
		System.out.println("Bad multi access: MultiConfigDescription.getReferenceInfo()");
		return Collections.emptyMap();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getResolvedSourceEntries()
	 */
	public ICSourceEntry[] getResolvedSourceEntries() {
		return new ICSourceEntry[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getResourceDescription(org.eclipse.core.runtime.IPath, boolean)
	 */
	public ICResourceDescription getResourceDescription(IPath path,
			boolean isForFolder) {
		ArrayList<ICResourceDescription> lst = new ArrayList<ICResourceDescription>();
		for (int i=0; i<fCfgs.length; i++) {
			ICResourceDescription rd = fCfgs[i].getResourceDescription(path, false);
			if (! path.equals(rd.getPath()) ) {
				try {
					if (isForFolder)
						rd = fCfgs[i].createFolderDescription(path, (ICFolderDescription)rd);
					else
						rd = fCfgs[i].createFileDescription(path, rd);
				} catch (CoreException e) {}
			}
			if (rd != null)
				lst.add(rd);
		}
		if (lst.size() == 0)
			return null;
		if (lst.size() == 1)
			return (ICResourceDescription)lst.get(0);
		if (isForFolder)
			return new MultiFolderDescription(
				(ICFolderDescription[])lst.toArray(new ICFolderDescription[lst.size()]));
		else
			return new MultiFileDescription(
					(ICFileDescription[])lst.toArray(new ICFileDescription[lst.size()]));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getResourceDescriptions()
	 */
	public ICResourceDescription[] getResourceDescriptions() {
		ArrayList<ICResourceDescription> lst = new ArrayList<ICResourceDescription>();
		for (int i=0; i<fCfgs.length; i++) 
			lst.addAll(Arrays.asList(fCfgs[i].getResourceDescriptions()));
		return (ICResourceDescription[])lst.toArray(new ICResourceDescription[lst.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getRootFolderDescription()
	 */
	public ICFolderDescription getRootFolderDescription() {
		ICFolderDescription[] rds = new ICFolderDescription[fCfgs.length];
		for (int i=0; i<fCfgs.length; i++) 
			rds[i] = fCfgs[i].getRootFolderDescription();
		return new MultiFolderDescription(rds);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getSessionProperty(org.eclipse.core.runtime.QualifiedName)
	 */
	public Object getSessionProperty(QualifiedName name) {
		System.out.println("Bad multi access: MultiConfigDescription.getSessionProperty()"); //$NON-NLS-1$
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getSourceEntries()
	 */
	public ICSourceEntry[] getSourceEntries() {
		return new ICSourceEntry[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#getTargetPlatformSetting()
	 */
	public ICTargetPlatformSetting getTargetPlatformSetting() {
		System.out.println("Bad multi access: MultiConfigDescription.getTargetPlatfSetting()");
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#isActive()
	 */
	public boolean isActive() {
		for (int i=0; i<fCfgs.length; i++)
			if (fCfgs[i].isActive())
					return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#isModified()
	 */
	public boolean isModified() {
		for (int i=0; i<fCfgs.length; i++)
			if (fCfgs[i].isModified())
					return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#isPreferenceConfiguration()
	 */
	public boolean isPreferenceConfiguration() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#remove(org.eclipse.cdt.core.settings.model.ICConfigExtensionReference)
	 */
	public void remove(ICConfigExtensionReference ext) throws CoreException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].remove(ext);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#remove(java.lang.String)
	 */
	public void remove(String extensionPoint) throws CoreException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].remove(extensionPoint);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#removeExternalSetting(org.eclipse.cdt.core.settings.model.ICExternalSetting)
	 */
	public void removeExternalSetting(ICExternalSetting setting)
			throws WriteAccessException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].removeExternalSetting(setting);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#removeExternalSettings()
	 */
	public void removeExternalSettings() throws WriteAccessException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].removeExternalSettings();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#removeResourceDescription(org.eclipse.cdt.core.settings.model.ICResourceDescription)
	 */
	public void removeResourceDescription(ICResourceDescription des)
			throws CoreException, WriteAccessException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#setActive()
	 */
	public void setActive() throws WriteAccessException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].setActive();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#setConfigurationData(java.lang.String, org.eclipse.cdt.core.settings.model.extension.CConfigurationData)
	 */
	public void setConfigurationData(String buildSystemId,
			CConfigurationData data) throws WriteAccessException {
		System.out.println("Bad multi access: MultiConfigDescription.getConfigurationData()"); //$NON-NLS-1$
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#setDescription(java.lang.String)
	 */
	public void setDescription(String des) throws WriteAccessException {
		System.out.println("Bad multi access: MultiConfigDescription.setDescription()"); //$NON-NLS-1$
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#setExternalSettingsProviderIds(java.lang.String[])
	 */
	public void setExternalSettingsProviderIds(String[] ids) {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].setExternalSettingsProviderIds(ids);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#setName(java.lang.String)
	 */
	public void setName(String name) throws WriteAccessException {
		System.out.println("Bad multi access: MultiConfigDescription.setName()");
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#setReferenceInfo(java.util.Map)
	 */
	public void setReferenceInfo(Map<String, String> refs) throws WriteAccessException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].setReferenceInfo(refs);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#setSessionProperty(org.eclipse.core.runtime.QualifiedName, java.lang.Object)
	 */
	public void setSessionProperty(QualifiedName name, Object value) {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].setSessionProperty(name, value);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#setSourceEntries(org.eclipse.cdt.core.settings.model.ICSourceEntry[])
	 */
	public void setSourceEntries(ICSourceEntry[] entries) throws CoreException,
			WriteAccessException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].setSourceEntries(entries);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICConfigurationDescription#updateExternalSettingsProviders(java.lang.String[])
	 */
	public void updateExternalSettingsProviders(String[] ids)
			throws WriteAccessException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].updateExternalSettingsProviders(ids);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingContainer#getChildSettings()
	 */
	public ICSettingObject[] getChildSettings() {
		return new ICSettingObject[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingObject#getConfiguration()
	 */
	public ICConfigurationDescription getConfiguration() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingObject#getId()
	 */
	public String getId() {
		return fCfgs[0].getId() + "_etc";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingObject#getName()
	 */
	public String getName() {
		return "Multiple Config Description";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingObject#getParent()
	 */
	public ICSettingContainer getParent() {
		ICSettingContainer p = fCfgs[0].getParent();
		if (p == null)
			return null;
		for (int i=1; i<fCfgs.length; i++)
			if (! p.equals(fCfgs[i].getParent()))
				return null;
		return p;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingObject#getType()
	 */
	public int getType() {
		int t = fCfgs[0].getType();
		for (int i=1; i<fCfgs.length; i++)
			if (t != fCfgs[i].getType())
				return 0;
		return t;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingObject#isReadOnly()
	 */
	public boolean isReadOnly() {
		for (int i=0; i<fCfgs.length; i++)
			if (! fCfgs[i].isReadOnly())
				return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingsStorage#setReadOnly(boolean, boolean)
	 */
	public void setReadOnly(boolean readOnly, boolean keepModify) {
		for (ICConfigurationDescription cfg : fCfgs)
			cfg.setReadOnly(readOnly, keepModify);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingObject#isValid()
	 */
	public boolean isValid() {
		for (int i=0; i<fCfgs.length; i++)
			if (! fCfgs[i].isValid())
				return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingsStorage#getStorage(java.lang.String, boolean)
	 */
	public ICStorageElement getStorage(String id, boolean create)
			throws CoreException {
		System.out.println("Bad multi access: MultiConfigDescription.getStorage()");
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingsStorage#getStorage(java.lang.String, boolean)
	 */
	public ICStorageElement importStorage(String id, ICStorageElement el) throws UnsupportedOperationException, CoreException {
		System.out.println("Bad multi access: MultiConfigDescription.importStorage()");
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.settings.model.ICSettingsStorage#removeStorage(java.lang.String)
	 */
	public void removeStorage(String id) throws CoreException {
		for (int i=0; i<fCfgs.length; i++)
			fCfgs[i].removeStorage(id);
	}

}
