/*******************************************************************************
 * Copyright (c) 2005 Texas Instruments Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Texas Instruments Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.core;

/**
 * This interface determines whether or not the option is currently displayed,
 * enabled, and used in command-line generation.
 */
public interface IOptionApplicability {
	/**
	 * This method is queried whenever a makefile or makefile fragment is
	 * generated which uses this option, and in the C/C++ Build property
	 * pages when displaying the current command line.
	 * 
	 * @param configuration  build configuration of option 
	 *                       (may be IConfiguration or IResourceConfiguration)
	 * @param holder         contains the holder of the option
	 * @param option         the option itself
	 * 
	 * @return true if this option is to be used in command line
	 *         generation, false otherwise
	 */
	public boolean isOptionUsedInCommandLine(
			IBuildObject configuration, 
            IHoldsOptions holder, 
            IOption option);           

	/**
	 * This method is queried whenever a new option category is displayed.
	 * 
	 * @param configuration  build configuration of option 
	 *                       (may be IConfiguration or IResourceConfiguration)
	 * @param holder         contains the holder of the option
	 * @param option         the option itself
	 * 
	 * @return true if this option should be visible in the build options page,
	 *         false otherwise
	 */
	public boolean isOptionVisible(
			IBuildObject configuration, 
            IHoldsOptions holder, 
            IOption option);           

	/**
	 * Whenever the value of an option changes in the GUI, this method is
	 * queried on all other visible options for the same category. Note that
	 * this occurs when the GUI changes - the user may opt to cancel these
	 * changes.
	 * 
	 * @param configuration  build configuration of option 
	 *                       (may be IConfiguration or IResourceConfiguration)
	 * @param holder         contains the holder of the option
	 * @param option         the option itself
	 *
	 * @return true if this option should be enabled in the build options page,
	 *         or false if it should be disabled (grayed out)
	 */
	public boolean isOptionEnabled(
			IBuildObject configuration, 
		    IHoldsOptions holder, 
		    IOption option);           

}
