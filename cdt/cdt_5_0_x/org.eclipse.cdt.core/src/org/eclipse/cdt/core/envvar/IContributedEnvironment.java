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
package org.eclipse.cdt.core.envvar;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

public interface IContributedEnvironment {
	IEnvironmentVariable[] getVariables(ICConfigurationDescription des);
	
	IEnvironmentVariable getVariable(String name, ICConfigurationDescription des);
	
	boolean appendEnvironment(ICConfigurationDescription des);

	void setAppendEnvironment(boolean append, ICConfigurationDescription des);
	
	IEnvironmentVariable addVariable(String name,
			String value,
			int op,
			String delimiter,
			ICConfigurationDescription des);

	IEnvironmentVariable addVariable(IEnvironmentVariable var,
			ICConfigurationDescription des);
	
	void addVariables(IEnvironmentVariable[] vars,
			ICConfigurationDescription des);

	IEnvironmentVariable removeVariable(String name, ICConfigurationDescription des);
	
	void restoreDefaults(ICConfigurationDescription des);
	
	boolean isUserVariable(ICConfigurationDescription des, IEnvironmentVariable var);
}
