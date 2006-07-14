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

package org.eclipse.mylar.internal.monitor.usage;

import org.eclipse.mylar.monitor.usage.IBackgroundPage;
import org.eclipse.mylar.monitor.usage.IQuestionnairePage;
import org.eclipse.mylar.monitor.usage.MylarUsageMonitorPlugin;


/**
 * @author Mik Kersten
 * @author Leah Findlater
 */
public class StudyParameters {

	private String title = MylarUsageMonitorPlugin.DEFAULT_TITLE;

	private String description = MylarUsageMonitorPlugin.DEFAULT_DESCRIPTION;

	private IQuestionnairePage questionnairePage = null;

	private IBackgroundPage backgroundPage = null;

	private long transmitPromptPeriod = MylarUsageMonitorPlugin.DEFAULT_DELAY_BETWEEN_TRANSMITS;

	private String acceptedUrlList = MylarUsageMonitorPlugin.DEFAULT_ACCEPTED_URL_LIST;

	private String useContactField = MylarUsageMonitorPlugin.DEFAULT_CONTACT_CONSENT_FIELD;

	private String formsConsent = MylarUsageMonitorPlugin.DEFAULT_ETHICS_FORM;

	private String version = MylarUsageMonitorPlugin.DEFAULT_VERSION;

	private String scriptsUrl = MylarUsageMonitorPlugin.DEFAULT_UPLOAD_SERVER;

	private String scriptsUpload = MylarUsageMonitorPlugin.DEFAULT_UPLOAD_SCRIPT;

	private String scriptsUserId = MylarUsageMonitorPlugin.DEFAULT_UPLOAD_SCRIPT_ID;

	private String scriptsQuestionnaire = MylarUsageMonitorPlugin.DEFAULT_UPLAOD_SCRIPT_QUESTIONNAIRE;

	public String getScriptsUserId() {
		return scriptsUserId;
	}

	public void setScriptsUserId(String scriptsGetUserId) {
		if (scriptsGetUserId != null)
			this.scriptsUserId = scriptsGetUserId;
	}

	public String getScriptsQuestionnaire() {
		return scriptsQuestionnaire;
	}

	public void setScriptsQuestionnaire(String scriptsQuestionnaire) {
		if (scriptsQuestionnaire != null)
			this.scriptsQuestionnaire = scriptsQuestionnaire;
	}

	public String getScriptsUrl() {
		return scriptsUrl;
	}

	public void setScriptsUrl(String scriptsServerUrl) {
		if (scriptsServerUrl != null)
			this.scriptsUrl = scriptsServerUrl;
	}

	public String getScriptsUpload() {
		return scriptsUpload;
	}

	public void setScriptsUpload(String scriptsUpload) {
		if (scriptsUpload != null)
			this.scriptsUpload = scriptsUpload;
	}

	public String getFormsConsent() {
		return formsConsent;
	}

	public void setFormsConsent(String formsConsent) {
		if (formsConsent != null)
			this.formsConsent = formsConsent;
	}

	public long getTransmitPromptPeriod() {
		return transmitPromptPeriod;
	}

	public void setTransmitPromptPeriod(long transmitPromptPeriod) {
		this.transmitPromptPeriod = transmitPromptPeriod;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		if (description != null)
			this.description = description;
	}

	public IQuestionnairePage getQuestionnairePage() {
		return questionnairePage;
	}

	public void setQuestionnairePage(IQuestionnairePage questionnairePage) {
		if (questionnairePage != null)
			this.questionnairePage = questionnairePage;
	}

	public IBackgroundPage getBackgroundPage() {
		return backgroundPage;
	}

	public void setBackgroundPage(IBackgroundPage backgroundPage) {
		if (backgroundPage != null)
			this.backgroundPage = backgroundPage;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (title != null)
			this.title = title;
	}

	public String getAcceptedUrlList() {
		return this.acceptedUrlList;
	}

	public void setAcceptedUrlList(String acceptedUrlList) {
		if (acceptedUrlList != null)
			this.acceptedUrlList = acceptedUrlList;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		if (version != null)
			this.version = version;
	}

	public String getUseContactField() {
		return useContactField;
	}

	public void setUseContactField(String useContactField) {
		if (useContactField != null)
			this.useContactField = useContactField;
	}
}