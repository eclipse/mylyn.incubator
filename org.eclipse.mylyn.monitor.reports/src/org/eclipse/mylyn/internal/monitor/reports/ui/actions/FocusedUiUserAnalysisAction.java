/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.monitor.reports.ui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.mylyn.core.MylarStatusHandler;
import org.eclipse.mylyn.internal.monitor.core.collection.IUsageCollector;
import org.eclipse.mylyn.internal.monitor.reports.MonitorReportsPlugin;
import org.eclipse.mylyn.internal.monitor.reports.collectors.FocusedUiUsageAnalysisCollector;
import org.eclipse.mylyn.internal.monitor.usage.UiUsageMonitorPlugin;
import org.eclipse.mylyn.internal.monitor.usage.editors.UsageStatsEditorInput;
import org.eclipse.mylyn.monitor.usage.ReportGenerator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ViewPluginAction;

/**
 * @author Mik Kersten
 */
public class FocusedUiUserAnalysisAction implements IViewActionDelegate {

	public void init(IViewPart view) {
		// ignore
	}

	public void run(IAction action) {
		if (action instanceof ViewPluginAction) {
			ViewPluginAction objectAction = (ViewPluginAction) action;
			final List<File> files = EclipseUsageSummaryAction.getStatsFilesFromSelection(objectAction);
			if (files.isEmpty()) {
				files.add(UiUsageMonitorPlugin.getDefault().getMonitorLogFile());
			}
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						List<IUsageCollector> collectors = new ArrayList<IUsageCollector>();
						collectors.add(new FocusedUiUsageAnalysisCollector());
						ReportGenerator generator = new ReportGenerator(UiUsageMonitorPlugin.getDefault()
								.getInteractionLogger(), collectors);

						IWorkbenchPage page = MonitorReportsPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
								.getActivePage();
						if (page == null)
							return;
						IEditorInput input = new UsageStatsEditorInput(files, generator);
						page.openEditor(input, MonitorReportsPlugin.REPORT_USERS_ID);
					} catch (PartInitException ex) {
						MylarStatusHandler.log(ex, "couldn't open summary editor");
					}
				}
			});
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
	}
}