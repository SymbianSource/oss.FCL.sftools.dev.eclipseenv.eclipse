/*******************************************************************************
 * Copyright (c) 2005 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.macros;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;

/**
 * 
 * this interface is to be implemented by the tool-integrator
 * for supplying the configuration-specific macros
 * 
 * @since 3.0
 */
public interface IConfigurationBuildMacroSupplier {
	/**
	 *
	 * @ macroName the macro name
	 * @param configuration configuration
	 * @param provider the instance of the build macro provider to be used for querying the
	 * build macros from within the supplier. The supplier should use this provider to obtain
	 * the already defined build macros instead of using the �default� provider returned by the
	 * ManagedBuildManager.getBuildMacroProvider().
	 * The provider passed to a supplier will ignore searching macros for the levels 
	 * higher than the current supplier level, will query only the lower-precedence suppliers 
	 * for the current level and will query all suppliers for the lower levels. 
	 * This is done to avoid infinite loops that could be caused if the supplier calls the provider 
	 * and the provider in turn calls that supplier again. Also the supplier should not know anything
	 * about the build macros defined for the higher levels.
	 * @return the reference to the IBuildMacro interface representing 
	 * the build macro of a given name or null if the macro of  that name is not defined
	 */
	public IBuildMacro getMacro(String macroName, 
				IConfiguration configuration, 
				IBuildMacroProvider provider);
 
	/**
	 *
	 * @param configuration configuration
	 * @param provider the instance of the build macro provider to be used for querying the
	 * build macros from within the supplier. The supplier should use this provider to obtain
	 * the already defined build macros instead of using the �default� provider returned by the
	 * ManagedBuildManager.getBuildMacroProvider().
	 * The provider passed to a supplier will ignore searching macros for the levels 
	 * higher than the current supplier level, will query only the lower-precedence suppliers 
	 * for the current level and will query all suppliers for the lower levels. 
	 * This is done to avoid infinite loops that could be caused if the supplier calls the provider 
	 * and the provider in turn calls that supplier again. Also the supplier should not know anything
	 * about the build macros defined for the higher levels.
	 * @return the IBuildMacro[] array representing defined macros 
	 */
	public IBuildMacro[] getMacros(IConfiguration configuration,
			IBuildMacroProvider provider);
}

