/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Meghan Allen - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.monitor.usage.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.internal.monitor.core.collection.IUsageCollector;
import org.eclipse.mylyn.internal.monitor.core.collection.ViewUsageCollector;
import org.eclipse.mylyn.internal.monitor.usage.MonitorFileRolloverJob;
import org.eclipse.mylyn.internal.monitor.usage.StudyParameters;
import org.eclipse.mylyn.internal.monitor.usage.UiUsageMonitorPlugin;
import org.eclipse.mylyn.internal.monitor.usage.collectors.PerspectiveUsageCollector;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author Meghan Allen
 */
public class NewUsageSummaryEditorWizard extends Wizard implements INewWizard {

	private static final String TITLE = Messages.NewUsageSummaryEditorWizard_New_Usage_Summary_Report;

	private UsageSummaryEditorWizardPage usageSummaryPage;

	private final StudyParameters studyParameters;

	public NewUsageSummaryEditorWizard() {
		super();
		init();
		setWindowTitle(TITLE);
		studyParameters = UiUsageMonitorPlugin.getDefault().getStudyParameters();
	}

	private void init() {
		usageSummaryPage = new UsageSummaryEditorWizardPage();
	}

	@Override
	public boolean performFinish() {

		if (!usageSummaryPage.includePerspective() && !usageSummaryPage.includeViews()) {
			return false;
		}

		List<IUsageCollector> collectors = new ArrayList<IUsageCollector>();

		if (usageSummaryPage.includePerspective()) {
			collectors.add(new PerspectiveUsageCollector());
		}
		if (usageSummaryPage.includeViews()) {
			ViewUsageCollector mylynViewUsageCollector = new ViewUsageCollector();
			collectors.add(mylynViewUsageCollector);
		}

		MonitorFileRolloverJob job = new MonitorFileRolloverJob(collectors, studyParameters);
		job.setPriority(Job.LONG);
		job.schedule();

		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// ignore

	}

	@Override
	public void addPages() {
		addPage(usageSummaryPage);
	}

}
