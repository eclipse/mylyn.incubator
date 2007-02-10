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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.context.core.IContextStoreListener;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.context.core.MylarContextManager;
import org.eclipse.mylar.internal.monitor.usage.wizards.UsageSubmissionWizard;
import org.eclipse.mylar.monitor.core.IInteractionEventListener;
import org.eclipse.mylar.monitor.ui.AbstractCommandMonitor;
import org.eclipse.mylar.monitor.ui.IActionExecutionListener;
import org.eclipse.mylar.monitor.ui.IMylarMonitorLifecycleListener;
import org.eclipse.mylar.monitor.ui.MylarMonitorUiPlugin;
import org.eclipse.mylar.monitor.ui.workbench.ActionExecutionMonitor;
import org.eclipse.mylar.monitor.ui.workbench.ActivityChangeMonitor;
import org.eclipse.mylar.monitor.ui.workbench.BrowserMonitor;
import org.eclipse.mylar.monitor.ui.workbench.KeybindingCommandMonitor;
import org.eclipse.mylar.monitor.ui.workbench.MenuCommandMonitor;
import org.eclipse.mylar.monitor.ui.workbench.PerspectiveChangeMonitor;
import org.eclipse.mylar.monitor.ui.workbench.PreferenceChangeMonitor;
import org.eclipse.mylar.monitor.ui.workbench.WindowChangeMonitor;
import org.eclipse.mylar.monitor.usage.IBackgroundPage;
import org.eclipse.mylar.monitor.usage.IQuestionnairePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.ui.security.Authentication;
import org.eclipse.update.internal.ui.security.UserValidationDialog;
import org.osgi.framework.BundleContext;

/**
 * @author Mik Kersten
 */
public class MylarUsageMonitorPlugin extends AbstractUIPlugin implements IStartup {

	public static final String PREF_USER_ID = "org.eclipse.mylar.user.id";

	public static String VERSION = "1.0";

	public static String UPLOAD_FILE_LABEL = "USAGE";

	private static final long HOUR = 3600 * 1000;

	private static final long DELAY_ON_USER_REQUEST = 3 * HOUR;

	private static final long DELAY_ON_FAILURE = 5 * HOUR;

	public static final String DEFAULT_TITLE = "Mylar Feedback";

	public static final String DEFAULT_DESCRIPTION = "Fill out the following form to help us improve Mylar based on your input.\n";

	public static final long DEFAULT_DELAY_BETWEEN_TRANSMITS = 14 * 24 * HOUR;

	public static final String DEFAULT_ETHICS_FORM = "doc/study-ethics.html";

	public static final String DEFAULT_VERSION = "";

	public static final String DEFAULT_UPLOAD_SERVER = "http://mylar.eclipse.org/monitor/upload";

	public static final String DEFAULT_UPLOAD_SERVLET_ID = "/GetUserIDServlet";

	public static final String DEFAULT_UPLOAD_SERVLET = "/MylarUsageUploadServlet";

	public static final String DEFAULT_ACCEPTED_URL_LIST = "";

	public static final String DEFAULT_CONTACT_CONSENT_FIELD = "false";

	public static final String UI_PLUGIN_ID = "org.eclipse.mylar.ui";

	public static final String MONITOR_LOG_NAME = "monitor-history";

	public static final String PLUGIN_ID = "org.eclipse.mylar.monitor.ui";

	private InteractionEventLogger interactionLogger;

	private String customizingPlugin = null;

	private PreferenceChangeMonitor preferenceMonitor = new PreferenceChangeMonitor();

	private PerspectiveChangeMonitor perspectiveMonitor;

	private ActivityChangeMonitor activityMonitor;

	private MenuCommandMonitor menuMonitor;

	private WindowChangeMonitor windowMonitor;

	private KeybindingCommandMonitor keybindingCommandMonitor;

	private BrowserMonitor browserMonitor;

	private static MylarUsageMonitorPlugin plugin;

	private List<IActionExecutionListener> actionExecutionListeners = new ArrayList<IActionExecutionListener>();

	private List<AbstractCommandMonitor> commandMonitors = new ArrayList<AbstractCommandMonitor>();

	private ResourceBundle resourceBundle;

	private static Date lastTransmit = null;

	private boolean notifiedOfUserIdSubmission = false;

	private Authentication uploadAuthentication = null;

	private static boolean performingUpload = false;

	private boolean questionnaireEnabled = true;

	private boolean backgroundEnabled = false;

	private StudyParameters studyParameters = new StudyParameters();

	private ListenerList lifecycleListeners = new ListenerList();

	private IWindowListener WINDOW_LISTENER = new IWindowListener() {
		public void windowActivated(IWorkbenchWindow window) {
		}

		public void windowDeactivated(IWorkbenchWindow window) {
		}

		public void windowClosed(IWorkbenchWindow window) {
			if (window.getShell() != null) {
				window.getShell().removeShellListener(SHELL_LISTENER);
			}
		}

		public void windowOpened(IWorkbenchWindow window) {
			if (window.getShell() != null && !PlatformUI.getWorkbench().isClosing()) {
				window.getShell().addShellListener(SHELL_LISTENER);
			}
		}
	};

	private ShellListener SHELL_LISTENER = new ShellListener() {

		public void shellDeactivated(ShellEvent arg0) {
			if (!isPerformingUpload() && ContextCorePlugin.getDefault() != null) {
				for (IInteractionEventListener listener : MylarMonitorUiPlugin.getDefault().getInteractionListeners())
					listener.stopMonitoring();
			}
		}

		public void shellActivated(ShellEvent arg0) {
			if (!ContextCorePlugin.getDefault().suppressWizardsOnStartup() && ContextCorePlugin.getDefault() != null) {
				// checkForStudyPhasePromotion();
				checkForStatisticsUpload();
			}
			if (!isPerformingUpload() && ContextCorePlugin.getDefault() != null) {
				for (IInteractionEventListener listener : MylarMonitorUiPlugin.getDefault().getInteractionListeners())
					listener.startMonitoring();
			}
		}

		public void shellDeiconified(ShellEvent arg0) {
		}

		public void shellIconified(ShellEvent arg0) {
		}

		public void shellClosed(ShellEvent arg0) {
		}
	};

	private IContextStoreListener DATA_DIR_MOVE_LISTENER = new IContextStoreListener() {

		public void contextStoreMoved() {
			if (!isPerformingUpload()) {
				for (IInteractionEventListener listener : MylarMonitorUiPlugin.getDefault().getInteractionListeners())
					listener.stopMonitoring();
				interactionLogger.moveOutputFile(getMonitorLogFile().getAbsolutePath());
				for (IInteractionEventListener listener : MylarMonitorUiPlugin.getDefault().getInteractionListeners())
					listener.startMonitoring();
			}
		}
	};

	public MylarUsageMonitorPlugin() {
		plugin = this;
	}

	private void initDefaultPrefs() {
		getPreferenceStore().setDefault(MylarMonitorPreferenceConstants.PREF_MONITORING_OBFUSCATE, true);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		initDefaultPrefs();
		new MonitorUsageExtensionPointReader().initExtensions();

		try {
			interactionLogger = new InteractionEventLogger(getMonitorLogFile());

			perspectiveMonitor = new PerspectiveChangeMonitor();
			activityMonitor = new ActivityChangeMonitor();
			windowMonitor = new WindowChangeMonitor();
			menuMonitor = new MenuCommandMonitor();
			keybindingCommandMonitor = new KeybindingCommandMonitor();
			browserMonitor = new BrowserMonitor();

			setAcceptedUrlMatchList(studyParameters.getAcceptedUrlList());
			studyParameters.setServletUrl(DEFAULT_UPLOAD_SERVER + DEFAULT_UPLOAD_SERVLET);

			final IWorkbench workbench = PlatformUI.getWorkbench();
			workbench.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {

						if (getPreferenceStore().getBoolean(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLED)) {
							// will be reset
							getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLED,
									false);
							startMonitoring();
						}

						if (plugin.getPreferenceStore().contains(
								MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE)) {
							lastTransmit = new Date(plugin.getPreferenceStore().getLong(
									MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE));
						} else {
							lastTransmit = new Date();
							plugin.getPreferenceStore()
									.setValue(MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE,
											lastTransmit.getTime());
						}
					} catch (Throwable t) {
						MylarStatusHandler.fail(t, "monitor failed to start", false);
					}
				}
			});
		} catch (Throwable t) {
			MylarStatusHandler.fail(t, "monitor failed to start", false);
		}
	}

	/**
	 * Used to start plugin on startup -> entry in plugin.xml to invoke this
	 */
	public void earlyStartup() {
		// final IWorkbench workbench = PlatformUI.getWorkbench();
		// workbench.getDisplay().asyncExec(new Runnable() {
		// public void run() {
		//
		// }
		// });
	}

	public void startMonitoring() {
		if (getPreferenceStore().getBoolean(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLED))
			return;
		interactionLogger.startMonitoring();
		for (IInteractionEventListener listener : MylarMonitorUiPlugin.getDefault().getInteractionListeners())
			listener.startMonitoring();

		IWorkbench workbench = PlatformUI.getWorkbench();
		MylarMonitorUiPlugin.getDefault().addInteractionListener(interactionLogger);
		getCommandMonitors().add(keybindingCommandMonitor);

		getActionExecutionListeners().add(new ActionExecutionMonitor());
		workbench.addWindowListener(WINDOW_LISTENER);
		for (IWorkbenchWindow w : workbench.getWorkbenchWindows()) {
			if (w.getShell() != null) {
				w.getShell().addShellListener(SHELL_LISTENER);
			}
		}
		ContextCorePlugin.getDefault().getContextStore().addListener(DATA_DIR_MOVE_LISTENER);
		MylarMonitorUiPlugin.getDefault().addWindowPerspectiveListener(perspectiveMonitor);
		workbench.getActivitySupport().getActivityManager().addActivityManagerListener(activityMonitor);
		workbench.getDisplay().addFilter(SWT.Selection, menuMonitor);
		workbench.addWindowListener(windowMonitor);

		installBrowserMonitor(workbench);

		for (Object listener : lifecycleListeners.getListeners()) {
			((IMylarMonitorLifecycleListener) listener).startMonitoring();
		}

		if (!ContextCorePlugin.getDefault().suppressWizardsOnStartup()) {
			checkForFirstMonitorUse();
		}
		getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLED, true);
	}

	public void addMonitoredPreferences(Preferences preferences) {
		preferences.addPropertyChangeListener(preferenceMonitor);
	}

	public void removeMonitoredPreferences(Preferences preferences) {
		preferences.removePropertyChangeListener(preferenceMonitor);
	}

	public boolean isObfuscationEnabled() {
		return MylarUsageMonitorPlugin.getPrefs().getBoolean(MylarMonitorPreferenceConstants.PREF_MONITORING_OBFUSCATE);
	}

	public void stopMonitoring() {
		if (!getPreferenceStore().getBoolean(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLED))
			return;

		for (Object listener : lifecycleListeners.getListeners()) {
			((IMylarMonitorLifecycleListener) listener).stopMonitoring();
		}

		for (IInteractionEventListener listener : MylarMonitorUiPlugin.getDefault().getInteractionListeners())
			listener.stopMonitoring();

		IWorkbench workbench = PlatformUI.getWorkbench();
		MylarMonitorUiPlugin.getDefault().removeInteractionListener(interactionLogger);

		getCommandMonitors().remove(keybindingCommandMonitor);
		getActionExecutionListeners().remove(new ActionExecutionMonitor());

		workbench.removeWindowListener(WINDOW_LISTENER);
		for (IWorkbenchWindow w : workbench.getWorkbenchWindows()) {
			if (w.getShell() != null) {
				w.getShell().removeShellListener(SHELL_LISTENER);
			}
		}
		ContextCorePlugin.getDefault().getContextStore().removeListener(DATA_DIR_MOVE_LISTENER);
		// ContextCorePlugin.getDefault().getPluginPreferences().removePropertyChangeListener(DATA_DIR_MOVE_LISTENER);

		MylarMonitorUiPlugin.getDefault().removeWindowPerspectiveListener(perspectiveMonitor);
		workbench.getActivitySupport().getActivityManager().removeActivityManagerListener(activityMonitor);
		workbench.getDisplay().removeFilter(SWT.Selection, menuMonitor);
		workbench.removeWindowListener(windowMonitor);

		uninstallBrowserMonitor(workbench);

		interactionLogger.stopMonitoring();

		getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLED, false);
	}

	public void addMonitoringLifecycleListener(IMylarMonitorLifecycleListener listener) {
		lifecycleListeners.add(listener);
		if (isMonitoringEnabled()) {
			listener.startMonitoring();
		}
	}

	public void removeMonitoringLifecycleListener(IMylarMonitorLifecycleListener listener) {
		lifecycleListeners.remove(listener);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		resourceBundle = null;
	}

	public void actionObserved(IAction action, String info) {
		for (IActionExecutionListener listener : actionExecutionListeners) {
			listener.actionObserved(action);
		}
	}

	public List<IActionExecutionListener> getActionExecutionListeners() {
		return actionExecutionListeners;
	}

	public List<AbstractCommandMonitor> getCommandMonitors() {
		return commandMonitors;
	}

	private void installBrowserMonitor(IWorkbench workbench) {
		workbench.addWindowListener(browserMonitor);
		IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			windows[i].addPageListener(browserMonitor);
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j++) {
				pages[j].addPartListener(browserMonitor);
			}
		}
	}

	private void uninstallBrowserMonitor(IWorkbench workbench) {
		workbench.removeWindowListener(browserMonitor);
		IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			windows[i].removePageListener(browserMonitor);
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j++) {
				pages[j].removePartListener(browserMonitor);
			}
		}
	}

	public File getMonitorLogFile() {
		File rootDir;
		if (ContextCorePlugin.getDefault().getContextStore() != null) {
			rootDir = ContextCorePlugin.getDefault().getContextStore().getRootDirectory();
		} else {
			rootDir = new File(getStateLocation().toString());
		}

		File file = new File(rootDir, MONITOR_LOG_NAME + MylarContextManager.CONTEXT_FILE_EXTENSION_OLD);

		// File oldFile = new
		// File(ContextCorePlugin.getDefault().getContextStore().getRootDirectory(),
		// "workspace" + MylarContextManager.CONTEXT_FILE_EXTENSION_OLD);
		// if (oldFile.exists()) {
		// oldFile.renameTo(file);
		// } else
		if (!file.exists() || !file.canWrite()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				MylarStatusHandler.log(e, "could not create monitor file");
			}
		}
		return file;
	}

	private long getUserTransimitDelay() {
		return DELAY_ON_USER_REQUEST / HOUR;
	}

	public void userCancelSubmitFeedback(Date currentTime, boolean wait3Hours) {
		if (wait3Hours) {
			lastTransmit.setTime(currentTime.getTime() + DELAY_ON_USER_REQUEST
					- studyParameters.getTransmitPromptPeriod());
			plugin.getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE,
					lastTransmit.getTime());
		} else {
			long day = HOUR * 24;
			lastTransmit.setTime(currentTime.getTime() + day - studyParameters.getTransmitPromptPeriod());
			plugin.getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE,
					lastTransmit.getTime());
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static MylarUsageMonitorPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = MylarUsageMonitorPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle("org.eclipse.mylar.monitor.ui.MonitorPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}

	private void checkForFirstMonitorUse() {
		if (!isMonitoringEnabled())
			return;
		if (!notifiedOfUserIdSubmission
				&& !MylarUsageMonitorPlugin.getDefault().getPreferenceStore().contains(
						MylarUsageMonitorPlugin.PREF_USER_ID)) {
			notifiedOfUserIdSubmission = true;
			UsageSubmissionWizard wizard = new UsageSubmissionWizard(false);
			wizard.init(PlatformUI.getWorkbench(), null);
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			dialog.create();
			dialog.open();
		}
	}

	private void checkForStatisticsUpload() {
		if (!isMonitoringEnabled())
			return;
		if (plugin == null || plugin.getPreferenceStore() == null)
			return;

		if (plugin.getPreferenceStore().contains(MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE)) {

			lastTransmit = new Date(plugin.getPreferenceStore().getLong(
					MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE));
		} else {
			lastTransmit = new Date();
			plugin.getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE,
					lastTransmit.getTime());
		}
		Date currentTime = new Date();

		if (currentTime.getTime() > lastTransmit.getTime() + studyParameters.getTransmitPromptPeriod()
				&& getPreferenceStore().getBoolean(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLE_SUBMISSION)) {

			String ending = getUserTransimitDelay() == 1 ? "" : "s";
			MessageDialog message = new MessageDialog(Display.getDefault().getActiveShell(), "Send Usage Feedback",
					null, "Send Mylar Usage Statistics feedback now?", MessageDialog.QUESTION, new String[] {
							IDialogConstants.YES_LABEL, "Remind me in " + getUserTransimitDelay() + " hour" + ending,
							"Remind me tomorrow" }, 0);
			int result = 0;
			if ((result = message.open()) == 0) {
				// time must be stored right away into preferences, to prevent
				// other threads
				lastTransmit.setTime(new Date().getTime());
				plugin.getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE,
						currentTime.getTime());

				if (!plugin.getPreferenceStore().contains(
						MylarMonitorPreferenceConstants.PREF_MONITORING_MYLAR_ECLIPSE_ORG_CONSENT_VIEWED)
						|| !plugin.getPreferenceStore().getBoolean(
								MylarMonitorPreferenceConstants.PREF_MONITORING_MYLAR_ECLIPSE_ORG_CONSENT_VIEWED)) {
					MessageDialog consentMessage = new MessageDialog(
							Display.getDefault().getActiveShell(),
							"Consent",
							null,
							"All data that is submitted to mylar.eclipse.org will be publicly available under the "
									+ "Eclipse Public License (EPL).  By submitting your data, you are agreeing that it can be publicly "
									+ "available. Please press cancel on the submission dialog box if you do not wish to share your data.",
							MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
					consentMessage.open();
					plugin.getPreferenceStore().setValue(
							MylarMonitorPreferenceConstants.PREF_MONITORING_MYLAR_ECLIPSE_ORG_CONSENT_VIEWED, true);
				}

				UsageSubmissionWizard wizard = new UsageSubmissionWizard();
				wizard.init(PlatformUI.getWorkbench(), null);
				// Instantiates the wizard container with the wizard and
				// opens it
				WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
				dialog.create();
				dialog.open();
				if (wizard.failed()) {
					lastTransmit.setTime(currentTime.getTime() + DELAY_ON_FAILURE
							- studyParameters.getTransmitPromptPeriod());
					plugin.getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_PREVIOUS_TRANSMIT_DATE,
							currentTime.getTime());
				}
			} else {
				if (result == 1) {
					userCancelSubmitFeedback(currentTime, true);
				} else {
					userCancelSubmitFeedback(currentTime, false);
				}
				message.close();
			}
		}
	}

	public void incrementObservedEvents(int increment) {
		int numEvents = getPreferenceStore().getInt(MylarMonitorPreferenceConstants.PREF_NUM_USER_EVENTS);
		numEvents += increment;
		getPreferenceStore().setValue(MylarMonitorPreferenceConstants.PREF_NUM_USER_EVENTS, numEvents);
	}

	public void configureProxy(HttpClient httpClient) {
		if (UpdateCore.getPlugin().getPluginPreferences().getBoolean(UpdateCore.HTTP_PROXY_ENABLE)) {
			String proxyHost = UpdateCore.getPlugin().getPluginPreferences().getString(UpdateCore.HTTP_PROXY_HOST);
			int proxyPort = UpdateCore.getPlugin().getPluginPreferences().getInt(UpdateCore.HTTP_PROXY_PORT);
			httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);

			if (uploadAuthentication == null)
				uploadAuthentication = UserValidationDialog.getAuthentication(proxyHost,
						"(Leave fields blank if authentication is not required)");
			if (uploadAuthentication != null) {
				httpClient.getState().setProxyCredentials(
						new AuthScope(proxyHost, proxyPort),
						new UsernamePasswordCredentials(uploadAuthentication.getUser(), uploadAuthentication
								.getPassword()));
			}
		}
	}

	public static IPreferenceStore getPrefs() {
		return getDefault().getPreferenceStore();
	}

	public static boolean isPerformingUpload() {
		return performingUpload;
	}

	public static void setPerformingUpload(boolean performingUpload) {
		MylarUsageMonitorPlugin.performingUpload = performingUpload;
	}

	public InteractionEventLogger getInteractionLogger() {
		return interactionLogger;
	}

	public boolean isQuestionnaireEnabled() {
		return questionnaireEnabled;
	}

	public void setQuestionnaireEnabled(boolean questionnaireEnabled) {
		this.questionnaireEnabled = questionnaireEnabled;
	}

	class MonitorUsageExtensionPointReader {

		public static final String EXTENSION_ID_STUDY = "org.eclipse.mylar.monitor.ui.study";

		public static final String ELEMENT_SCRIPTS = "scripts";

		public static final String ELEMENT_SCRIPTS_VERSION = "version";

		public static final String ELEMENT_SCRIPTS_SERVER_URL = "url";

		public static final String ELEMENT_SCRIPTS_UPLOAD_USAGE = "upload";

		public static final String ELEMENT_SCRIPTS_GET_USER_ID = "userId";

		public static final String ELEMENT_SCRIPTS_UPLOAD_QUESTIONNAIRE = "questionnaire";

		public static final String ELEMENT_UI = "ui";

		public static final String ELEMENT_UI_TITLE = "title";

		public static final String ELEMENT_UI_DESCRIPTION = "description";

		public static final String ELEMENT_UI_UPLOAD_PROMPT = "daysBetweenUpload";

		public static final String ELEMENT_UI_QUESTIONNAIRE_PAGE = "questionnairePage";

		public static final String ELEMENT_UI_BACKGROUND_PAGE = "backgroundPage";

		public static final String ELEMENT_UI_CONSENT_FORM = "consentForm";

		public static final String ELEMENT_UI_CONTACT_CONSENT_FIELD = "useContactField";

		public static final String ELEMENT_MONITORS = "monitors";

		public static final String ELEMENT_MONITORS_BROWSER_URL = "browserUrlFilter";

		private boolean extensionsRead = false;

		// private MonitorUsageExtensionPointReader thisReader = new
		// MonitorUsageExtensionPointReader();

		@SuppressWarnings("deprecation")
		public void initExtensions() {
			try {
				if (!extensionsRead) {
					IExtensionRegistry registry = Platform.getExtensionRegistry();
					IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_ID_STUDY);
					if (extensionPoint != null) {
						IExtension[] extensions = extensionPoint.getExtensions();
						for (int i = 0; i < extensions.length; i++) {
							IConfigurationElement[] elements = extensions[i].getConfigurationElements();
							for (int j = 0; j < elements.length; j++) {
								if (elements[j].getName().compareTo(ELEMENT_SCRIPTS) == 0) {
									readScripts(elements[j]);
								} else if (elements[j].getName().compareTo(ELEMENT_UI) == 0) {
									readForms(elements[j]);
								} else if (elements[j].getName().compareTo(ELEMENT_MONITORS) == 0) {
									readMonitors(elements[j]);
								}
							}
							customizingPlugin = extensions[i].getNamespace();
							getPreferenceStore()
									.setValue(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLED, true);
						}
						extensionsRead = true;
					}
				}
			} catch (Throwable t) {
				MylarStatusHandler.fail(t, "could not read monitor extension", false);
			}
		}

		private void readScripts(IConfigurationElement element) {
			studyParameters.setVersion(element.getAttribute(ELEMENT_SCRIPTS_VERSION));
		}

		private void readForms(IConfigurationElement element) throws CoreException {
			studyParameters.setTitle(element.getAttribute(ELEMENT_UI_TITLE));
			studyParameters.setDescription(element.getAttribute(ELEMENT_UI_DESCRIPTION));
			if (element.getAttribute(ELEMENT_UI_UPLOAD_PROMPT) != null) {
				Integer uploadInt = new Integer(element.getAttribute(ELEMENT_UI_UPLOAD_PROMPT));
				studyParameters.setTransmitPromptPeriod(HOUR * 24 * uploadInt);
			}
			studyParameters.setUseContactField(element.getAttribute(ELEMENT_UI_CONTACT_CONSENT_FIELD));

			try {
				if (element.getAttribute(ELEMENT_UI_QUESTIONNAIRE_PAGE) != null) {
					Object questionnaireObject = element.createExecutableExtension(ELEMENT_UI_QUESTIONNAIRE_PAGE);
					if (questionnaireObject instanceof IQuestionnairePage) {
						IQuestionnairePage page = (IQuestionnairePage) questionnaireObject;
						studyParameters.setQuestionnairePage(page);
					}
				} else {
					MylarUsageMonitorPlugin.getDefault().setQuestionnaireEnabled(false);
				}
			} catch (CoreException throwable) {
				MylarStatusHandler.fail(throwable, "could not load questionnaire", false);
				MylarUsageMonitorPlugin.getDefault().setQuestionnaireEnabled(false);
			}

			try {
				if (element.getAttribute(ELEMENT_UI_BACKGROUND_PAGE) != null) {
					Object backgroundObject = element.createExecutableExtension(ELEMENT_UI_BACKGROUND_PAGE);
					if (backgroundObject instanceof IBackgroundPage) {
						IBackgroundPage page = (IBackgroundPage) backgroundObject;
						studyParameters.setBackgroundPage(page);
						MylarUsageMonitorPlugin.getDefault().setBackgroundEnabled(true);
					}
				} else {
					MylarUsageMonitorPlugin.getDefault().setBackgroundEnabled(false);
				}
			} catch (CoreException throwable) {
				MylarStatusHandler.fail(throwable, "could not load background page", false);
				MylarUsageMonitorPlugin.getDefault().setBackgroundEnabled(false);
			}

			studyParameters.setFormsConsent("/" + element.getAttribute(ELEMENT_UI_CONSENT_FORM));

		}

		private void readMonitors(IConfigurationElement element) throws CoreException {
			// TODO: This should parse a list of filters but right now it takes
			// the
			// entire string as a single filter.
			// ArrayList<String> urlList = new ArrayList<String>();
			String urlList = element.getAttribute(ELEMENT_MONITORS_BROWSER_URL);
			studyParameters.setAcceptedUrlList(urlList);
		}
	}

	public StudyParameters getStudyParameters() {
		return studyParameters;
	}

	public String getCustomizingPlugin() {
		return customizingPlugin;
	}

	public boolean isMonitoringEnabled() {
		return getPreferenceStore().getBoolean(MylarMonitorPreferenceConstants.PREF_MONITORING_ENABLED);
	}

	public String getCustomizedByMessage() {
		String customizedBy = MylarUsageMonitorPlugin.getDefault().getCustomizingPlugin();
		String message = "NOTE: You have previously downloaded the Mylar monitor and a user study plug-in with id: "
				+ customizedBy + "\n" + "If you are not familiar with this plug-in do not upload data.";
		return message;
	}

	/**
	 * @return true if the list was set
	 */
	public boolean setAcceptedUrlMatchList(String urlBuffer) {
		if (browserMonitor != null) {
			browserMonitor.setAcceptedUrls(urlBuffer);
			return true;
		} else {
			return false;
		}
	}

	public boolean isBackgroundEnabled() {
		return backgroundEnabled;
	}

	public void setBackgroundEnabled(boolean backgroundEnabled) {
		this.backgroundEnabled = backgroundEnabled;
	}

	public String getExtensionVersion() {
		return studyParameters.getVersion();
	}

	public boolean usingContactField() {
		if (studyParameters.getUseContactField().equals("true"))
			return true;
		else
			return false;
	}
}

// private void installPreferenceMonitoring() {
// ((IEclipsePreferences) getPreferenceStore().getp())
// .addNodeChangeListener(getNodeChangeListener());

// Bundle[] bundles =
// PDERuntimePlugin.getDefault().getBundleContext().getBundles();
// for (Bundle bundle : bundles) {
// if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
// if (bundle.getState() == Bundle.ACTIVE) {
// System.err.println(">> " + bundle.getClass());
// }
// }
// }

// for (String namespace :
// Platform.getExtensionRegistry().getNamespaces()) {
// IExtension extension =
// Platform.getExtensionRegistry().getExtension(namespace);
// }
// }