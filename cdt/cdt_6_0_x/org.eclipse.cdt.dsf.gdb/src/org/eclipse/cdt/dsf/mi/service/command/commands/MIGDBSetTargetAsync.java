/*******************************************************************************
 * Copyright (c) 2009 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.dsf.mi.service.command.commands;

import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;

/**
 * 
 * -gdb-set target-async [on | off]
 * 
 * @since 2.1
 */
public class MIGDBSetTargetAsync extends MIGDBSet 
{
    public MIGDBSetTargetAsync(ICommandControlDMContext ctx, boolean isSet) {
        super(ctx, new String[] {"target-async", isSet ? "on" : "off"});//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    }
}
