/*******************************************************************************
 * Copyright (c) 2000, 2005 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.core.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDICondition;
import org.eclipse.cdt.debug.core.cdi.ICDIFunctionLocation;
import org.eclipse.cdt.debug.core.cdi.ICDILineLocation;
import org.eclipse.cdt.debug.core.cdi.ICDILocator;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.model.ICDIBreakpoint;
import org.eclipse.cdt.debug.core.cdi.model.ICDILocationBreakpoint;
import org.eclipse.cdt.debug.core.cdi.model.ICDIStackFrame;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.testplugin.CDebugHelper;
import org.eclipse.cdt.debug.testplugin.CProjectHelper;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * @author Peter Graves
 * 
 * This file contains a set of generic tests for the CDI break point interfaces.
 * It will currenly use the mi implementation.
 *  
 */
public class BreakpointTests extends AbstractDebugTest {


	public static Test suite() {
		return new DebugTestWrapper(BreakpointTests.class) {};
	}
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		createDebugSession();
		assertNotNull(currentTarget);
		currentTarget.deleteAllBreakpoints();
		pause();
	}
	@Override
	protected void tearDown() throws Exception {
		/* clean up the session */
		targets[0].terminate();
		int x = 0;
		while ((!targets[0].isTerminated()) && (x < 30)) {
			Thread.sleep(100);
		}
		if (!targets[0].isTerminated())
			targets[0].terminate();
		super.tearDown();
	}


	/***************************************************************************
	 * A couple tests to make sure setting breakpoints on functions works as
	 * expected.
	 */
	public void testFunctionBreak() throws CoreException, MIException, IOException, CDIException, InterruptedException {
		
		ICDITarget cdiTarget = currentTarget;
		ICDIFunctionLocation location;
		boolean caught = false;


		/***********************************************************************
		 * Create a break point on a generic function
		 **********************************************************************/

		location = cdiTarget.createFunctionLocation(null, "func1"); //$NON-NLS-1$
		assertNotNull(location);
		cdiTarget.setFunctionBreakpoint(0, location, null, false);

		/***********************************************************************
		 * Create a break point on main
		 **********************************************************************/

		location = cdiTarget.createFunctionLocation(null, "main"); //$NON-NLS-1$
		assertNotNull(location);
		cdiTarget.setFunctionBreakpoint(0, location, null, false);

		/***********************************************************************
		 * Try to create a break point on a function name that does not exist We
		 * expect that this will cause the setLocationBreakpoint to throw a
		 * CDIException
		 **********************************************************************/

		location = cdiTarget.createFunctionLocation(null, "badname"); //$NON-NLS-1$
		assertNotNull(location);
		try {
			cdiTarget.setFunctionBreakpoint(0, location, null, false);
		} catch (CDIException e) {
			caught = true;
		}
		assertTrue(caught);

		cdiTarget.deleteAllBreakpoints();

		/***********************************************************************
		 * Create a break point on a generic function and see if it will get hit
		 * and stop program execution.
		 **********************************************************************/

		location = cdiTarget.createFunctionLocation(null, "func1"); //$NON-NLS-1$
		assertNotNull(location);
		cdiTarget.setFunctionBreakpoint(0, location, null, false);
		targets = session.getTargets();
		/*
		 * We better only have one target connected to this session or something
		 * is not right...
		 */
		assertTrue(targets.length == 1);
		/*
		 * Resume the target, this should cause it to run till it hits the
		 * breakpoint
		 */
		targets[0].resume();
		/**
		 * Give the process up to 10 seconds to become either terminated or
		 * suspended. It sould hit the breakponint almost immediatly so we
		 * should only sleep for max 100 ms
		 */
		for (int x = 0; x < 100; x++) {
			if (targets[0].isTerminated() || targets[0].isSuspended())
				break;
			Thread.sleep(100);
		}
		assertTrue(targets[0].isSuspended());
		ICDILocator locator = targets[0].getCurrentThread().getStackFrames()[0].getLocator();
		assertTrue(locator.getLineNumber() == 6);
		assertTrue(locator.getFunction().equals("func1")); //$NON-NLS-1$
		assertTrue(locator.getFile().endsWith("main.c")); //$NON-NLS-1$




	}

	/***************************************************************************
	 * A couple tests to make sure setting breakpoints on line numbers works as
	 * expected.
	 */
	public void testLineBreak() throws CoreException, MIException, IOException, CDIException, InterruptedException {
		ICDITarget cdiTarget = currentTarget;
		ICDILineLocation location;
		boolean caught = false;


		/***********************************************************************
		 * Create a break point in a generic function
		 **********************************************************************/
		location = cdiTarget.createLineLocation("main.c", 7);
		assertNotNull(location);
		cdiTarget.setLineBreakpoint(0, location, null, false);

		/***********************************************************************
		 * Create a break point in main
		 **********************************************************************/
		location = cdiTarget.createLineLocation("main.c", 18);
		assertNotNull(location);
		cdiTarget.setLineBreakpoint(0, location, null, false);

		/***********************************************************************
		 * Try to create a break point on a line that does not exist We expect
		 * that this will cause the setLocationBreakpoint to throw a
		 * CDIException
		 **********************************************************************/

		location = cdiTarget.createLineLocation("main.c", 30);
		assertNotNull(location);
		try {
			cdiTarget.setLineBreakpoint(0, location, null, false);
		} catch (CDIException e) {
			caught = true;
		}
		assertTrue(caught);

		caught = false;
		/***********************************************************************
		 * Try to create a break point on a line that does not have code on it
		 **********************************************************************/

		location = cdiTarget.createLineLocation("main.c", 11);
		assertNotNull(location);
		cdiTarget.setLineBreakpoint(0, location, null, false);

		/***********************************************************************
		 * Create a break point in a generic function without passing the source
		 * file name. At the time of writing this would just silently fail, so
		 * to make sure it works, we will do it once with a valid line number
		 * and once with an invalid line number, and the first should always
		 * succeed and the second should always throw an exception.
		 **********************************************************************/
		location = cdiTarget.createLineLocation(null, 7);
		assertNotNull(location);
		cdiTarget.setLineBreakpoint(0, location, null, false);
		caught = false;
		location = cdiTarget.createLineLocation(null, 30);
		assertNotNull(location);
		try {
			cdiTarget.setLineBreakpoint(0, location, null, false);
		} catch (CDIException e) {
			caught = true;
		}
		assertTrue("Ignoring line numbers with no file specified?", caught);

		cdiTarget.deleteAllBreakpoints();

		/***********************************************************************
		 * Create a break point on a line number and see if it will get hit and
		 * stop program execution.
		 **********************************************************************/

		location = cdiTarget.createLineLocation(null, 7);
		assertNotNull(location);
		cdiTarget.setLineBreakpoint(0, location, null, false);
		targets = session.getTargets();
		/*
		 * We better only have one target connected to this session or something
		 * is not right...
		 */
		assertTrue(targets.length == 1);
		/*
		 * Resume the target, this should cause it to run till it hits the
		 * breakpoint
		 */
		targets[0].resume();
		/**
		 * Give the process up to 10 seconds to become either terminated or
		 * suspended. It sould hit the breakponint almost immediatly so we
		 * should only sleep for max 100 ms
		 */
		for (int x = 0; x < 100; x++) {
			if (targets[0].isSuspended() || targets[0].isTerminated())
				break;
			Thread.sleep(100);
		}
		assertTrue("Suspended: " + targets[0].isSuspended() + " Termiunated: " + targets[0].isTerminated(), targets[0]
				.isSuspended());
		ICDILocator locator = targets[0].getCurrentThread().getStackFrames()[0].getLocator();
		assertTrue(locator.getLineNumber() == 7);
		assertTrue(locator.getFunction().equals("func1"));
		assertTrue(locator.getFile().endsWith("main.c"));

	}

	/***************************************************************************
	 * A couple tests to make sure getting breakpoints works as expected
	 */
	public void testGetBreak() throws CoreException, MIException, IOException, CDIException {
		ICDITarget cdiTarget = currentTarget;
		ICDIFunctionLocation location;
		ICDIBreakpoint[] breakpoints;
		ICDILocationBreakpoint curbreak;


		/***********************************************************************
		 * Make sure initially we don't have any breakpoints
		 **********************************************************************/
		breakpoints = cdiTarget.getBreakpoints();
		assertNotNull(breakpoints);
		assertTrue(breakpoints.length == 0);

		/***********************************************************************
		 * Make sure if we create a simple breakpoint, that we can get it back
		 * from the system
		 **********************************************************************/
		/* Create a break point on a generic function */
		location = cdiTarget.createFunctionLocation("main.c", "func1");
		assertNotNull(location);
		cdiTarget.setFunctionBreakpoint(0, location, null, false);

		breakpoints = cdiTarget.getBreakpoints();
		assertNotNull(breakpoints);
		assertTrue(breakpoints.length == 1);
		if (breakpoints[0] instanceof ICDILocationBreakpoint) {
			curbreak = (ICDILocationBreakpoint) breakpoints[0];
		} else
			curbreak = null;
		assertNotNull(curbreak);

		//assertTrue(curbreak.getLocator().equals(location));
		{
			ICDILocator locator = curbreak.getLocator();
			String file = locator.getFile();
			String function = locator.getFunction();
			assertTrue("main.c".equals(file));
			assertTrue("func1".equals(function));
		}

		/***********************************************************************
		 * Make sure if we create multiple break points that we can still get
		 * them all back from the system,
		 **********************************************************************/
		/* Create another break point on main */
		location = cdiTarget.createFunctionLocation("main.c", "main");
		assertNotNull(location);
		cdiTarget.setFunctionBreakpoint(0, location, null, false);

		breakpoints = cdiTarget.getBreakpoints();
		assertNotNull(breakpoints);
		assertTrue(breakpoints.length == 2);
		if (breakpoints[1] instanceof ICDILocationBreakpoint) {
			curbreak = (ICDILocationBreakpoint) breakpoints[1];
		} else
			curbreak = null;
		assertNotNull(curbreak);
		/*
		 * Make sure the location still looks like we expect it to.. .
		 */
		//assertTrue(curbreak.getLocation().equals(location));
		{
			ICDILocator locator = curbreak.getLocator();
			String file = locator.getFile();
			String function = locator.getFunction();
			assertTrue("main.c".equals(file));
			assertTrue("main".equals(function));
		}

		cdiTarget.deleteAllBreakpoints();


	}

	/***************************************************************************
	 * A couple tests to make sure deleting breakpoints works as expected
	 */
	public void testDelBreak() throws CoreException, MIException, IOException, CDIException {
		ICDITarget cdiTarget = currentTarget;
		ICDIFunctionLocation location;
		ICDILocator savedLocation;
		ICDIBreakpoint[] breakpoints, savedbreakpoints;
		ICDILocationBreakpoint curbreak;

		/* Make sure initially we don't have any breakpoints */
		breakpoints = cdiTarget.getBreakpoints();
		assertNotNull(breakpoints);
		assertTrue(breakpoints.length == 0);

		/***********************************************************************
		 * 
		 * Test to make sure if we create a new breakpoint, we can delete it by
		 * passing a refrence to it to deleteBreakpoint()
		 *  
		 **********************************************************************/

		/* Create a break point on a generic function */
		location = cdiTarget.createFunctionLocation("main.c", "func1");
		assertNotNull(location);
		curbreak = cdiTarget.setFunctionBreakpoint(0, location, null, false);
		cdiTarget.deleteBreakpoints( new ICDIBreakpoint[] { curbreak } );
		pause();
		/**
		 * we should not have any breakpoints left.
		 */
		breakpoints = cdiTarget.getBreakpoints();
		assertTrue(breakpoints.length == 0);

		/***********************************************************************
		 * 
		 * Test to make sure if we create multiple new breakpoint, we can delete
		 * one of them by passing a refrence to it to deleteBreakpoint()
		 *  
		 **********************************************************************/

		/* Create a break point on a generic function */
		location = cdiTarget.createFunctionLocation("main.c", "func1");
		assertNotNull(location);
		curbreak = cdiTarget.setFunctionBreakpoint(0, location, null, false);
		savedLocation = curbreak.getLocator();

		location = cdiTarget.createFunctionLocation("main.c", "main");
		assertNotNull(location);
		curbreak = cdiTarget.setFunctionBreakpoint(0, location, null, false);
		cdiTarget.deleteBreakpoints( new ICDIBreakpoint[] { curbreak } );
		pause();
		breakpoints = cdiTarget.getBreakpoints();
		/***********************************************************************
		 * Make sure there is only 1 breakpoint left, and it's the one we expect
		 */
		assertTrue(breakpoints.length == 1);
		curbreak = (ICDILocationBreakpoint) breakpoints[0];
		assertNotNull(curbreak);
		assertTrue(curbreak.getLocator().equals(savedLocation));
		/***********************************************************************
		 * Then delete the other breakpoint.
		 */
		cdiTarget.deleteBreakpoints( new ICDIBreakpoint[] { curbreak } );
		pause();
		breakpoints = cdiTarget.getBreakpoints();
		assertTrue(breakpoints.length == 0);

		/***********************************************************************
		 * Make sure deleteBreakpoints works when given 1 breakpoint to delete
		 **********************************************************************/
		savedbreakpoints = new ICDIBreakpoint[1];
		for (int x = 0; x < 10; x++) {
			ICDILineLocation lineLocation = cdiTarget.createLineLocation("main.c", x + 1);
			savedbreakpoints[0] = cdiTarget.setLineBreakpoint(0, lineLocation, null, false);
			assertNotNull(savedbreakpoints[0]);
		}
		cdiTarget.deleteBreakpoints(savedbreakpoints);
		pause();
		/* We should now have 9 breakpoints left. */
		breakpoints = cdiTarget.getBreakpoints();
		assertTrue(breakpoints.length == 9);
		/* Make sure we have the correct 9 breakpoints left */
		for (int x = 0; x < breakpoints.length; x++) {
			curbreak = (ICDILocationBreakpoint) breakpoints[x];
			assertTrue(curbreak.getLocator().getLineNumber() == x + 1);
		}
		cdiTarget.deleteAllBreakpoints();
		pause();
		assertTrue(cdiTarget.getBreakpoints().length == 0);

		/***********************************************************************
		 * Make sure deleteBreakpoints works when given more then 1 but less
		 * then all breakpoints to delete
		 **********************************************************************/
		savedbreakpoints = new ICDIBreakpoint[4];
		for (int x = 0; x < 10; x++) {
			ICDILineLocation lineLocation = cdiTarget.createLineLocation("main.c", x + 1);
			savedbreakpoints[x % 4] = cdiTarget.setLineBreakpoint(0, lineLocation, null, false);
			assertNotNull(savedbreakpoints[x % 4]);
		}
		cdiTarget.deleteBreakpoints(savedbreakpoints);
		pause();

		/* We should now have 6 breakpoints left. */
		breakpoints = cdiTarget.getBreakpoints();
		assertTrue(breakpoints.length == 6);
		/* Make sure we have the correct 6 breakpoints left */
		for (int x = 0; x < breakpoints.length; x++) {
			curbreak = (ICDILocationBreakpoint) breakpoints[x];
			assertTrue(curbreak.getLocator().getLineNumber() == x + 1);
		}
		cdiTarget.deleteAllBreakpoints();
		pause();
		assertTrue(cdiTarget.getBreakpoints().length == 0);

		/***********************************************************************
		 * Make sure deleteBreakpoints works when given all the breakpoints
		 **********************************************************************/
		savedbreakpoints = new ICDIBreakpoint[10];
		for (int x = 0; x < 10; x++) {
			ICDILineLocation lineLocation = cdiTarget.createLineLocation("main.c", x + 1);
			savedbreakpoints[x] = cdiTarget.setLineBreakpoint(0, lineLocation, null, false);
			assertNotNull(savedbreakpoints[x]);
		}
		cdiTarget.deleteBreakpoints(savedbreakpoints);
		pause();
		/* We should now have 0 breakpoints left. */
		breakpoints = cdiTarget.getBreakpoints();
		assertTrue(breakpoints.length == 0);

		/***********************************************************************
		 * Make sure deleteAllBreakpoints works
		 **********************************************************************/

		for (int x = 0; x < 10; x++) {
			ICDILineLocation lineLocation = cdiTarget.createLineLocation("main.c", x + 1);
			curbreak = cdiTarget.setLineBreakpoint(0, lineLocation, null, false);
			assertNotNull(curbreak);
		}
		cdiTarget.deleteAllBreakpoints();
		pause();
		/* We should now have 0 breakpoints left. */
		breakpoints = cdiTarget.getBreakpoints();
		assertTrue(breakpoints.length == 0);

	}

	/***************************************************************************
	 * A couple tests to make sure setting breakpoints with conditions seems to
	 * work as expected.
	 */
	public void testCondBreak() throws CoreException, MIException, IOException, CDIException, InterruptedException {
		boolean caught = false;
		ICDITarget cdiTarget = currentTarget;

		/***********************************************************************
		 * Create a break point on a generic function with an empty condition
		 **********************************************************************/
		ICDICondition cond = cdiTarget.createCondition(0, "");
		ICDIFunctionLocation location = cdiTarget.createFunctionLocation(null, "func1");
		assertNotNull(location);
		cdiTarget.setFunctionBreakpoint(0, location, cond, false);

		/***********************************************************************
		 * Create a break point on a generic function with an valid condition
		 **********************************************************************/
		cond = cdiTarget.createCondition(0, "x<10");
		location = cdiTarget.createFunctionLocation(null, "func1");
		assertNotNull(location);
		cdiTarget.setFunctionBreakpoint(0, location, cond, false);

		/***********************************************************************
		 * Create a break point on a generic function with an invalid condition
		 * We expect to get a CDIException when we try to set the breakpoint.
		 **********************************************************************/
		cond = cdiTarget.createCondition(0, "nonexist<10");
		location = cdiTarget.createFunctionLocation(null, "func1");
		assertNotNull(location);
		try {
			cdiTarget.setFunctionBreakpoint(0, location, cond, false);
		} catch (CDIException e) {
			caught = true;
		}
		assertTrue(caught);

		/***********************************************************************
		 * Create a break point on a line number with a condition and make sure
		 * it does not suspend execution of the application until the condition
		 * is true
		 **********************************************************************/
		cdiTarget.deleteAllBreakpoints();
		ICDILineLocation lineLocation = cdiTarget.createLineLocation(null, 23);
		assertNotNull(location);
		cond = cdiTarget.createCondition(0, "a>10");

		cdiTarget.setLineBreakpoint(0, lineLocation, cond, false);
		targets = session.getTargets();
		/*
		 * We better only have one target connected to this session or something
		 * is not right...
		 */
		assertTrue(targets.length == 1);
		/*
		 * Resume the target, this should cause it to run till it hits the
		 * breakpoint
		 */
		targets[0].resume();
		/**
		 * Give the process up to 10 seconds to become either terminated or
		 * suspended. It sould hit the breakponint almost immediatly so we
		 * should only sleep for max 100 ms
		 */
		for (int x = 0; x < 100; x++) {
			if (targets[0].isSuspended() || targets[0].isTerminated())
				break;
			Thread.sleep(100);
		}
		assertTrue("Suspended: " + targets[0].isSuspended() + " Termiunated: " + targets[0].isTerminated(), targets[0]
				.isSuspended());
		ICDIStackFrame frame = targets[0].getCurrentThread().getStackFrames()[0]; 
		ICDILocator locator = frame.getLocator();
		assertTrue(locator.getLineNumber() == 23);
		assertTrue(locator.getFunction().equals("main"));
		assertTrue(locator.getFile().endsWith("main.c"));
		/* Get the value of a and and make sure it is 11 */
		assertTrue(targets[0].evaluateExpressionToString(frame, "a"), targets[0].evaluateExpressionToString(frame, "a").equals("11"));

	}


}