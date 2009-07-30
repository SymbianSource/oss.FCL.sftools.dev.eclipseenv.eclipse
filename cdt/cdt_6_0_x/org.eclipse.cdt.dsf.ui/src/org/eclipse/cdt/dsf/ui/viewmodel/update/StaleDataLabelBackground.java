/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.dsf.ui.viewmodel.update;

import org.eclipse.cdt.dsf.debug.ui.IDsfDebugUIConstants;
import org.eclipse.cdt.dsf.ui.viewmodel.properties.LabelBackground;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.RGB;

/**
 * Stale data backgroun color label attribute to use with the 
 * PropertyBasedLabelProvider.  The background color should only be 
 * used when the view is in no-columns mode. 
 * 
 * @since 2.0
 */
public class StaleDataLabelBackground extends LabelBackground {

    public StaleDataLabelBackground() {
        super(null);
    }
    
    @Override
    public RGB getBackground() {
        return JFaceResources.getColorRegistry().getRGB(
            IDsfDebugUIConstants.PREF_COLOR_STALE_DATA_BACKGROUND);
    }
    
    @Override
    public boolean isEnabled(IStatus status, java.util.Map<String,Object> properties) {
        return Boolean.TRUE.equals(properties.get(ICachingVMProvider.PROP_CACHE_ENTRY_DIRTY));
    }
}