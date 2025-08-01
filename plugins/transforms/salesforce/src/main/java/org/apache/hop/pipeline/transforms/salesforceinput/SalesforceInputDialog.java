/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.salesforceinput;

import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.XmlObject;
import java.util.HashSet;
import java.util.Set;
import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.util.StringUtil;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.PipelinePreviewFactory;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceConnection;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceConnectionUtils;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceTransformDialog;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceTransformMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.EnterNumberDialog;
import org.apache.hop.ui.core.dialog.EnterTextDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.dialog.PreviewRowsDialog;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.LabelTextVar;
import org.apache.hop.ui.core.widget.StyledTextComp;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.dialog.PipelinePreviewProgressDialog;
import org.apache.hop.ui.pipeline.transform.ComponentSelectionListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class SalesforceInputDialog extends SalesforceTransformDialog {

  private static final Class<?> PKG = SalesforceInputMeta.class;

  private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'.000'XXX";
  private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
  public static final String CONST_SYSTEM_COMBO_NO = "System.Combo.No";
  public static final String CONST_DATETIME = "datetime";
  public static final String CONST_BOOLEAN = "boolean";
  public static final String CONST_SYSTEM_BUTTON_OK = "System.Button.OK";
  public static final String CONST_SYSTEM_COMBO_YES = "System.Combo.Yes";

  private Button wInclURL;
  private Button wInclModule;
  private Button wInclRownum;
  private Button wUseCompression;
  private Button wQueryAll;

  private Label wlInclURLField;
  private Label wlInclModule;
  private Label wlInclRownumField;
  private Label wlInclModuleField;
  private Label wlModule;
  private Label wlInclSQLField;
  private Label wlInclTimestampField;
  private Label wlQueryAll;
  private Label wlInclDeletionDateField;
  private Label wlInclDeletionDate;

  private Button wInclSQL;

  private TextVar wInclURLField;
  private TextVar wInclModuleField;
  private TextVar wInclRownumField;
  private TextVar wInclSQLField;
  private TextVar wInclDeletionDateField;
  private Button wInclTimestamp;
  private Button wInclDeletionDate;

  private TextVar wInclTimestampField;

  private TableView wFields;

  private final SalesforceInputMeta input;

  private LabelTextVar wUserName;
  private LabelTextVar wURL;
  private LabelTextVar wPassword;

  private Label wlCondition;
  private StyledTextComp wCondition;
  private Label wlQuery;
  private StyledTextComp wQuery;

  private Label wlPosition;

  private Button wSpecifyQuery;

  private TextVar wTimeOut;
  private TextVar wLimit;

  private ComboVar wModule;

  private Label wlRecordsFilter;
  private CCombo wRecordsFilter;

  private Label wlReadFrom;
  private TextVar wReadFrom;
  private Button open;

  private Label wlReadTo;
  private TextVar wReadTo;
  private Button opento;

  private Listener lsTest;

  private boolean gotModule = false;

  private boolean getModulesListError = false; /* True if error getting modules list */

  private ColumnInfo[] columns;

  public SalesforceInputDialog(
      Shell parent,
      IVariables variables,
      SalesforceInputMeta transformMeta,
      PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    input = transformMeta;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    ModifyListener lsMod = e -> input.setChanged();

    SelectionListener checkBoxModifyListener =
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            input.setChanged();
          }
        };

    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.DialogTitle"));

    int middle = props.getMiddlePct();
    int margin = PropsUi.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "System.TransformName.Label"));
    wlTransformName.setToolTipText(BaseMessages.getString(PKG, "System.TransformName.Tooltip"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.top = new FormAttachment(0, margin);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    wlTransformName.setLayoutData(fdlTransformName);
    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    PropsUi.setLook(wTransformName);
    wTransformName.addModifyListener(lsMod);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(middle, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    fdTransformName.right = new FormAttachment(100, 0);
    wTransformName.setLayoutData(fdTransformName);

    // THE BUTTONS at the bottom...
    //
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, CONST_SYSTEM_BUTTON_OK));
    wOk.addListener(SWT.Selection, e -> ok());
    wPreview = new Button(shell, SWT.PUSH);
    wPreview.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.Button.PreviewRows"));
    wPreview.addListener(SWT.Selection, e -> preview());
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    wCancel.addListener(SWT.Selection, e -> cancel());
    setButtonPositions(new Button[] {wOk, wPreview, wCancel}, margin, null);

    // The tabfolder in between the name and the buttons...
    //
    CTabFolder wTabFolder = new CTabFolder(shell, SWT.BORDER);
    PropsUi.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);
    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.top = new FormAttachment(wTransformName, margin);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(wOk, -2 * margin);
    wTabFolder.setLayoutData(fdTabFolder);

    // ////////////////////////
    // START OF FILE TAB ///
    // ////////////////////////
    CTabItem wFileTab = new CTabItem(wTabFolder, SWT.NONE);
    wFileTab.setFont(GuiResource.getInstance().getFontDefault());
    wFileTab.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.File.Tab"));

    Composite wFileComp = new Composite(wTabFolder, SWT.NONE);
    PropsUi.setLook(wFileComp);

    FormLayout fileLayout = new FormLayout();
    fileLayout.marginWidth = 3;
    fileLayout.marginHeight = 3;
    wFileComp.setLayout(fileLayout);

    // ////////////////////////
    // START CONNECTION GROUP

    Group wConnectionGroup = new Group(wFileComp, SWT.SHADOW_ETCHED_IN);
    wConnectionGroup.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.ConnectionGroup.Label"));
    FormLayout fconnLayout = new FormLayout();
    fconnLayout.marginWidth = 3;
    fconnLayout.marginHeight = 3;
    wConnectionGroup.setLayout(fconnLayout);
    PropsUi.setLook(wConnectionGroup);

    // Webservice URL
    wURL =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceInputDialog.URL.Label"),
            BaseMessages.getString(PKG, "SalesforceInputDialog.URL.Tooltip"));
    PropsUi.setLook(wURL);
    wURL.addModifyListener(lsMod);
    FormData fdURL = new FormData();
    fdURL.left = new FormAttachment(0, 0);
    fdURL.top = new FormAttachment(0, margin);
    fdURL.right = new FormAttachment(100, 0);
    wURL.setLayoutData(fdURL);

    // UserName line
    wUserName =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceInputDialog.User.Label"),
            BaseMessages.getString(PKG, "SalesforceInputDialog.User.Tooltip"));
    PropsUi.setLook(wUserName);
    wUserName.addModifyListener(lsMod);
    FormData fdUserName = new FormData();
    fdUserName.left = new FormAttachment(0, 0);
    fdUserName.top = new FormAttachment(wURL, margin);
    fdUserName.right = new FormAttachment(100, 0);
    wUserName.setLayoutData(fdUserName);

    // Password line
    wPassword =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceInputDialog.Password.Label"),
            BaseMessages.getString(PKG, "SalesforceInputDialog.Password.Tooltip"),
            true);
    PropsUi.setLook(wPassword);
    wPassword.addModifyListener(lsMod);
    FormData fdPassword = new FormData();
    fdPassword.left = new FormAttachment(0, 0);
    fdPassword.top = new FormAttachment(wUserName, margin);
    fdPassword.right = new FormAttachment(100, 0);
    wPassword.setLayoutData(fdPassword);

    // Test Salesforce connection button
    Button wTest = new Button(wConnectionGroup, SWT.PUSH);
    wTest.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.TestConnection.Label"));
    PropsUi.setLook(wTest);
    FormData fdTest = new FormData();
    wTest.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.TestConnection.Tooltip"));
    fdTest.top = new FormAttachment(wPassword, margin);
    fdTest.right = new FormAttachment(100, 0);
    wTest.setLayoutData(fdTest);

    FormData fdConnectionGroup = new FormData();
    fdConnectionGroup.left = new FormAttachment(0, 0);
    fdConnectionGroup.right = new FormAttachment(100, 0);
    fdConnectionGroup.top = new FormAttachment(0, margin);
    wConnectionGroup.setLayoutData(fdConnectionGroup);

    // END CONNECTION GROUP
    // ////////////////////////

    // ////////////////////////
    // START SETTINGS GROUP

    Group wSettingsGroup = new Group(wFileComp, SWT.SHADOW_ETCHED_IN);
    wSettingsGroup.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.HttpAuthGroup.Label"));
    FormLayout fsettingsLayout = new FormLayout();
    fsettingsLayout.marginWidth = 3;
    fsettingsLayout.marginHeight = 3;
    wSettingsGroup.setLayout(fsettingsLayout);
    PropsUi.setLook(wSettingsGroup);

    Label wlSpecifyQuery = new Label(wSettingsGroup, SWT.RIGHT);
    wlSpecifyQuery.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.specifyQuery.Label"));
    PropsUi.setLook(wlSpecifyQuery);
    FormData fdlSpecifyQuery = new FormData();
    fdlSpecifyQuery.left = new FormAttachment(0, 0);
    fdlSpecifyQuery.top = new FormAttachment(wConnectionGroup, 2 * margin);
    fdlSpecifyQuery.right = new FormAttachment(middle, -margin);
    wlSpecifyQuery.setLayoutData(fdlSpecifyQuery);
    wSpecifyQuery = new Button(wSettingsGroup, SWT.CHECK);
    PropsUi.setLook(wSpecifyQuery);
    wSpecifyQuery.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.specifyQuery.Tooltip"));
    FormData fdSpecifyQuery = new FormData();
    fdSpecifyQuery.left = new FormAttachment(middle, 0);
    fdSpecifyQuery.top = new FormAttachment(wlSpecifyQuery, 0, SWT.CENTER);
    wSpecifyQuery.setLayoutData(fdSpecifyQuery);
    wSpecifyQuery.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setEnableQuery();
            input.setChanged();
          }
        });

    // Module
    wlModule = new Label(wSettingsGroup, SWT.RIGHT);
    wlModule.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.Module.Label"));
    PropsUi.setLook(wlModule);
    FormData fdlModule = new FormData();
    fdlModule.left = new FormAttachment(0, 0);
    fdlModule.top = new FormAttachment(wlSpecifyQuery, 2 * margin);
    fdlModule.right = new FormAttachment(middle, -margin);
    wlModule.setLayoutData(fdlModule);
    wModule = new ComboVar(variables, wSettingsGroup, SWT.BORDER | SWT.READ_ONLY);
    wModule.setEditable(true);
    PropsUi.setLook(wModule);
    wModule.addModifyListener(lsMod);
    FormData fdModule = new FormData();
    fdModule.left = new FormAttachment(middle, 0);
    fdModule.top = new FormAttachment(wlModule, 0, SWT.CENTER);
    fdModule.right = new FormAttachment(100, 0);
    wModule.setLayoutData(fdModule);
    wModule.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(FocusEvent e) {
            getModulesListError = false;
          }

          @Override
          public void focusGained(FocusEvent e) {
            // check if the URL and login credentials passed and not just had error
            if (Utils.isEmpty(wURL.getText())
                || Utils.isEmpty(wUserName.getText())
                || Utils.isEmpty(wPassword.getText())
                || (getModulesListError)) {
              return;
            }

            getModulesList();
          }
        });

    wlPosition = new Label(wSettingsGroup, SWT.NONE);
    PropsUi.setLook(wlPosition);
    FormData fdlPosition = new FormData();
    fdlPosition.left = new FormAttachment(middle, 0);
    fdlPosition.right = new FormAttachment(100, 0);
    fdlPosition.bottom = new FormAttachment(100, -margin);
    wlPosition.setLayoutData(fdlPosition);

    // condition
    wlCondition = new Label(wSettingsGroup, SWT.RIGHT);
    wlCondition.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.Condition.Label"));
    PropsUi.setLook(wlCondition);
    FormData fdlCondition = new FormData();
    fdlCondition.left = new FormAttachment(0, 0);
    fdlCondition.top = new FormAttachment(wModule, margin);
    fdlCondition.right = new FormAttachment(middle, -margin);
    wlCondition.setLayoutData(fdlCondition);

    wCondition =
        new StyledTextComp(
            variables,
            wSettingsGroup,
            SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    wCondition.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.Condition.Tooltip"));
    PropsUi.setLook(wCondition, Props.WIDGET_STYLE_FIXED);
    wCondition.addModifyListener(lsMod);
    FormData fdCondition = new FormData();
    fdCondition.left = new FormAttachment(middle, 0);
    fdCondition.top = new FormAttachment(wModule, margin);
    fdCondition.right = new FormAttachment(100, 0);
    fdCondition.bottom = new FormAttachment(wlPosition, -margin);
    wCondition.setLayoutData(fdCondition);
    wCondition.addModifyListener(
        e -> {
          setQueryToolTip();
          setPosition();
        });

    wCondition.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            setPosition();
          }

          @Override
          public void keyReleased(KeyEvent e) {
            setPosition();
          }
        });
    wCondition.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            setPosition();
          }

          @Override
          public void focusLost(FocusEvent e) {
            setPosition();
          }
        });
    wCondition.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseDoubleClick(MouseEvent e) {
            setPosition();
          }

          @Override
          public void mouseDown(MouseEvent e) {
            setPosition();
          }

          @Override
          public void mouseUp(MouseEvent e) {
            setPosition();
          }
        });

    // Query
    wlQuery = new Label(wSettingsGroup, SWT.RIGHT);
    wlQuery.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.Query.Label"));
    PropsUi.setLook(wlQuery);
    FormData fdlQuery = new FormData();
    fdlQuery.left = new FormAttachment(0, -margin);
    fdlQuery.top = new FormAttachment(wlSpecifyQuery, 2 * margin);
    fdlQuery.right = new FormAttachment(middle, -margin);
    wlQuery.setLayoutData(fdlQuery);

    wQuery =
        new StyledTextComp(
            variables,
            wSettingsGroup,
            SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    PropsUi.setLook(wQuery, Props.WIDGET_STYLE_FIXED);
    wQuery.addModifyListener(lsMod);
    FormData fdQuery = new FormData();
    fdQuery.left = new FormAttachment(middle, 0);
    fdQuery.top = new FormAttachment(wlSpecifyQuery, 2 * margin);
    fdQuery.right = new FormAttachment(100, -margin);
    fdQuery.bottom = new FormAttachment(wlPosition, -margin);
    wQuery.setLayoutData(fdQuery);
    wQuery.addModifyListener(arg0 -> setQueryToolTip());

    wQuery.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            setPosition();
          }

          @Override
          public void keyReleased(KeyEvent e) {
            setPosition();
          }
        });
    wQuery.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            setPosition();
          }

          @Override
          public void focusLost(FocusEvent e) {
            setPosition();
          }
        });
    wQuery.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseDoubleClick(MouseEvent e) {
            setPosition();
          }

          @Override
          public void mouseDown(MouseEvent e) {
            setPosition();
          }

          @Override
          public void mouseUp(MouseEvent e) {
            setPosition();
          }
        });

    FormData fdSettingsGroup = new FormData();
    fdSettingsGroup.left = new FormAttachment(0, 0);
    fdSettingsGroup.right = new FormAttachment(100, 0);
    fdSettingsGroup.bottom = new FormAttachment(100, 0);
    fdSettingsGroup.top = new FormAttachment(wConnectionGroup, margin);
    wSettingsGroup.setLayoutData(fdSettingsGroup);

    // END SETTINGS GROUP
    // ////////////////////////

    FormData fdFileComp = new FormData();
    fdFileComp.left = new FormAttachment(0, 0);
    fdFileComp.top = new FormAttachment(0, 0);
    fdFileComp.right = new FormAttachment(100, 0);
    fdFileComp.bottom = new FormAttachment(100, 0);
    wFileComp.setLayoutData(fdFileComp);

    wFileComp.layout();
    wFileTab.setControl(wFileComp);

    // ///////////////////////////////////////////////////////////
    // / END OF FILE TAB
    // ///////////////////////////////////////////////////////////

    // ////////////////////////
    // START OF CONTENT TAB///
    // /
    CTabItem wContentTab = new CTabItem(wTabFolder, SWT.NONE);
    wContentTab.setFont(GuiResource.getInstance().getFontDefault());
    wContentTab.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.Content.Tab"));

    FormLayout contentLayout = new FormLayout();
    contentLayout.marginWidth = 3;
    contentLayout.marginHeight = 3;

    Composite wContentComp = new Composite(wTabFolder, SWT.NONE);
    PropsUi.setLook(wContentComp);
    wContentComp.setLayout(contentLayout);

    // ///////////////////////////////
    // START OF Advanced GROUP //
    // ///////////////////////////////

    Group wAdvancedGroup = new Group(wContentComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wAdvancedGroup);
    wAdvancedGroup.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.AdvancedGroup.Label"));

    FormLayout advancedgroupLayout = new FormLayout();
    advancedgroupLayout.marginWidth = 10;
    advancedgroupLayout.marginHeight = 10;
    wAdvancedGroup.setLayout(advancedgroupLayout);

    // RecordsFilter
    wlRecordsFilter = new Label(wAdvancedGroup, SWT.RIGHT);
    wlRecordsFilter.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.RecordsFilter.Label"));
    PropsUi.setLook(wlRecordsFilter);
    FormData fdlRecordsFilter = new FormData();
    fdlRecordsFilter.left = new FormAttachment(0, 0);
    fdlRecordsFilter.right = new FormAttachment(middle, -margin);
    fdlRecordsFilter.top = new FormAttachment(0, 2 * margin);
    wlRecordsFilter.setLayoutData(fdlRecordsFilter);

    wRecordsFilter = new CCombo(wAdvancedGroup, SWT.BORDER | SWT.READ_ONLY);
    PropsUi.setLook(wRecordsFilter);
    wRecordsFilter.addModifyListener(lsMod);
    FormData fdRecordsFilter = new FormData();
    fdRecordsFilter.left = new FormAttachment(middle, 0);
    fdRecordsFilter.top = new FormAttachment(0, 2 * margin);
    fdRecordsFilter.right = new FormAttachment(100, -margin);
    wRecordsFilter.setLayoutData(fdRecordsFilter);
    wRecordsFilter.setItems(SalesforceConnectionUtils.recordsFilterDesc);
    wRecordsFilter.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            updateRecordsFilter();
          }
        });

    // Query All?
    wlQueryAll = new Label(wAdvancedGroup, SWT.RIGHT);
    wlQueryAll.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.QueryAll.Label"));
    PropsUi.setLook(wlQueryAll);
    FormData fdlQueryAll = new FormData();
    fdlQueryAll.left = new FormAttachment(0, 0);
    fdlQueryAll.top = new FormAttachment(wRecordsFilter, margin);
    fdlQueryAll.right = new FormAttachment(middle, -margin);
    wlQueryAll.setLayoutData(fdlQueryAll);
    wQueryAll = new Button(wAdvancedGroup, SWT.CHECK);
    wQueryAll.addSelectionListener(checkBoxModifyListener);
    PropsUi.setLook(wQueryAll);
    wQueryAll.setToolTipText(BaseMessages.getString(PKG, "SalesforceInputDialog.QueryAll.Tooltip"));
    FormData fdQueryAll = new FormData();
    fdQueryAll.left = new FormAttachment(middle, 0);
    fdQueryAll.top = new FormAttachment(wlQueryAll, 0, SWT.CENTER);
    wQueryAll.setLayoutData(fdQueryAll);
    wQueryAll.addSelectionListener(new ComponentSelectionListener(input));

    open = new Button(wAdvancedGroup, SWT.PUSH);
    open.setImage(GuiResource.getInstance().getImageCalendar());
    open.setToolTipText(BaseMessages.getString(PKG, "SalesforceInputDialog.OpenCalendar"));
    FormData fdlButton = new FormData();
    fdlButton.top = new FormAttachment(wQueryAll, margin);
    fdlButton.right = new FormAttachment(100, 0);
    open.setLayoutData(fdlButton);
    open.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            final Shell dialog = new Shell(shell, SWT.DIALOG_TRIM);
            dialog.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.SelectDate"));
            dialog.setImage(GuiResource.getInstance().getImageHop());
            dialog.setLayout(new GridLayout(3, false));

            final DateTime calendar = new DateTime(dialog, SWT.CALENDAR);
            final DateTime time = new DateTime(dialog, SWT.TIME | SWT.TIME);
            new Label(dialog, SWT.NONE);
            new Label(dialog, SWT.NONE);

            Button ok = new Button(dialog, SWT.PUSH);
            ok.setText(BaseMessages.getString(PKG, CONST_SYSTEM_BUTTON_OK));
            ok.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
            ok.addSelectionListener(
                new SelectionAdapter() {
                  @Override
                  public void widgetSelected(SelectionEvent e) {
                    wReadFrom.setText(
                        calendar.getYear()
                            + "-"
                            + ((calendar.getMonth() + 1) < 10
                                ? "0" + (calendar.getMonth() + 1)
                                : (calendar.getMonth() + 1))
                            + "-"
                            + (calendar.getDay() < 10 ? "0" + calendar.getDay() : calendar.getDay())
                            + " "
                            + (time.getHours() < 10 ? "0" + time.getHours() : time.getHours())
                            + ":"
                            + (time.getMinutes() < 10 ? "0" + time.getMinutes() : time.getMinutes())
                            + ":"
                            + (time.getMinutes() < 10
                                ? "0" + time.getMinutes()
                                : time.getMinutes()));

                    dialog.close();
                  }
                });
            dialog.setDefaultButton(ok);
            dialog.pack();
            dialog.open();
          }
        });

    wlReadFrom = new Label(wAdvancedGroup, SWT.RIGHT);
    wlReadFrom.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.ReadFrom.Label"));
    PropsUi.setLook(wlReadFrom);
    FormData fdlReadFrom = new FormData();
    fdlReadFrom.left = new FormAttachment(0, 0);
    fdlReadFrom.top = new FormAttachment(wQueryAll, margin);
    fdlReadFrom.right = new FormAttachment(middle, -margin);
    wlReadFrom.setLayoutData(fdlReadFrom);
    wReadFrom = new TextVar(variables, wAdvancedGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wReadFrom.setToolTipText(BaseMessages.getString(PKG, "SalesforceInputDialog.ReadFrom.Tooltip"));
    PropsUi.setLook(wReadFrom);
    wReadFrom.addModifyListener(lsMod);
    FormData fdReadFrom = new FormData();
    fdReadFrom.left = new FormAttachment(middle, 0);
    fdReadFrom.top = new FormAttachment(wQueryAll, margin);
    fdReadFrom.right = new FormAttachment(open, -margin);
    wReadFrom.setLayoutData(fdReadFrom);

    opento = new Button(wAdvancedGroup, SWT.PUSH);
    opento.setImage(GuiResource.getInstance().getImageCalendar());
    opento.setToolTipText(BaseMessages.getString(PKG, "SalesforceInputDialog.OpenCalendar"));
    FormData fdlButtonto = new FormData();
    fdlButtonto.top = new FormAttachment(wReadFrom, 2 * margin);
    fdlButtonto.right = new FormAttachment(100, 0);
    opento.setLayoutData(fdlButtonto);
    opento.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            final Shell dialogto = new Shell(shell, SWT.DIALOG_TRIM);
            dialogto.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.SelectDate"));
            dialogto.setImage(GuiResource.getInstance().getImageHop());
            dialogto.setLayout(new GridLayout(3, false));

            final DateTime calendarto = new DateTime(dialogto, SWT.CALENDAR | SWT.BORDER);
            final DateTime timeto = new DateTime(dialogto, SWT.TIME | SWT.TIME);
            new Label(dialogto, SWT.NONE);
            new Label(dialogto, SWT.NONE);
            Button okto = new Button(dialogto, SWT.PUSH);
            okto.setText(BaseMessages.getString(PKG, CONST_SYSTEM_BUTTON_OK));
            okto.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
            okto.addSelectionListener(
                new SelectionAdapter() {
                  @Override
                  public void widgetSelected(SelectionEvent e) {
                    wReadTo.setText(
                        calendarto.getYear()
                            + "-"
                            + ((calendarto.getMonth() + 1) < 10
                                ? "0" + (calendarto.getMonth() + 1)
                                : (calendarto.getMonth() + 1))
                            + "-"
                            + (calendarto.getDay() < 10
                                ? "0" + calendarto.getDay()
                                : calendarto.getDay())
                            + " "
                            + (timeto.getHours() < 10 ? "0" + timeto.getHours() : timeto.getHours())
                            + ":"
                            + (timeto.getMinutes() < 10
                                ? "0" + timeto.getMinutes()
                                : timeto.getMinutes())
                            + ":"
                            + (timeto.getSeconds() < 10
                                ? "0" + timeto.getSeconds()
                                : timeto.getSeconds()));
                    dialogto.close();
                  }
                });
            dialogto.setDefaultButton(okto);
            dialogto.pack();
            dialogto.open();
          }
        });

    wlReadTo = new Label(wAdvancedGroup, SWT.RIGHT);
    wlReadTo.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.ReadTo.Label"));
    PropsUi.setLook(wlReadTo);
    FormData fdlReadTo = new FormData();
    fdlReadTo.left = new FormAttachment(0, 0);
    fdlReadTo.top = new FormAttachment(wReadFrom, 2 * margin);
    fdlReadTo.right = new FormAttachment(middle, -margin);
    wlReadTo.setLayoutData(fdlReadTo);
    wReadTo = new TextVar(variables, wAdvancedGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wReadTo.setToolTipText(BaseMessages.getString(PKG, "SalesforceInputDialog.ReadTo.Tooltip"));
    PropsUi.setLook(wReadTo);
    wReadTo.addModifyListener(lsMod);
    FormData fdReadTo = new FormData();
    fdReadTo.left = new FormAttachment(middle, 0);
    fdReadTo.top = new FormAttachment(wReadFrom, 2 * margin);
    fdReadTo.right = new FormAttachment(opento, -margin);
    wReadTo.setLayoutData(fdReadTo);

    FormData fdAdvancedGroup = new FormData();
    fdAdvancedGroup.left = new FormAttachment(0, margin);
    fdAdvancedGroup.top = new FormAttachment(0, 2 * margin);
    fdAdvancedGroup.right = new FormAttachment(100, -margin);
    wAdvancedGroup.setLayoutData(fdAdvancedGroup);

    // ///////////////////////////////////////////////////////////
    // / END OF Advanced GROUP
    // ///////////////////////////////////////////////////////////

    // ///////////////////////////////
    // START OF Additional Fields GROUP //
    // ///////////////////////////////

    Group wAdditionalFields = new Group(wContentComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wAdditionalFields);
    wAdditionalFields.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.wAdditionalFields.Label"));

    FormLayout additionalFieldsgroupLayout = new FormLayout();
    additionalFieldsgroupLayout.marginWidth = 10;
    additionalFieldsgroupLayout.marginHeight = 10;
    wAdditionalFields.setLayout(additionalFieldsgroupLayout);

    // Add Salesforce URL in the output stream ?
    Label wlInclURL = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclURL.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.InclURL.Label"));
    PropsUi.setLook(wlInclURL);
    FormData fdlInclURL = new FormData();
    fdlInclURL.left = new FormAttachment(0, 0);
    fdlInclURL.top = new FormAttachment(wAdvancedGroup, margin);
    fdlInclURL.right = new FormAttachment(middle, -margin);
    wlInclURL.setLayoutData(fdlInclURL);
    wInclURL = new Button(wAdditionalFields, SWT.CHECK);
    PropsUi.setLook(wInclURL);
    wInclURL.setToolTipText(BaseMessages.getString(PKG, "SalesforceInputDialog.InclURL.Tooltip"));
    FormData fdInclURL = new FormData();
    fdInclURL.left = new FormAttachment(middle, 0);
    fdInclURL.top = new FormAttachment(wlInclURL, 0, SWT.CENTER);
    wInclURL.setLayoutData(fdInclURL);
    wInclURL.addSelectionListener(checkBoxModifyListener);
    wInclURL.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setEnableInclTargetURL();
            input.setChanged();
          }
        });

    wlInclURLField = new Label(wAdditionalFields, SWT.LEFT);
    wlInclURLField.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.InclURLField.Label"));
    PropsUi.setLook(wlInclURLField);
    FormData fdlInclURLField = new FormData();
    fdlInclURLField.left = new FormAttachment(wInclURL, margin);
    fdlInclURLField.top = new FormAttachment(wAdvancedGroup, margin);
    wlInclURLField.setLayoutData(fdlInclURLField);
    wInclURLField = new TextVar(variables, wAdditionalFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wlInclURLField);
    wInclURLField.addModifyListener(lsMod);
    FormData fdInclURLField = new FormData();
    fdInclURLField.left = new FormAttachment(wlInclURLField, margin);
    fdInclURLField.top = new FormAttachment(wAdvancedGroup, margin);
    fdInclURLField.right = new FormAttachment(100, 0);
    wInclURLField.setLayoutData(fdInclURLField);

    // Add module in the output stream ?
    wlInclModule = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclModule.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.InclModule.Label"));
    PropsUi.setLook(wlInclModule);
    FormData fdlInclModule = new FormData();
    fdlInclModule.left = new FormAttachment(0, 0);
    fdlInclModule.top = new FormAttachment(wInclURLField, margin);
    fdlInclModule.right = new FormAttachment(middle, -margin);
    wlInclModule.setLayoutData(fdlInclModule);
    wInclModule = new Button(wAdditionalFields, SWT.CHECK);
    PropsUi.setLook(wInclModule);
    wInclModule.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclModule.Tooltip"));
    fdModule = new FormData();
    fdModule.left = new FormAttachment(middle, 0);
    fdModule.top = new FormAttachment(wlInclModule, 0, SWT.CENTER);
    wInclModule.setLayoutData(fdModule);
    wInclModule.addSelectionListener(checkBoxModifyListener);

    wInclModule.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setEnableInclModule();
            input.setChanged();
          }
        });

    wlInclModuleField = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclModuleField.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclModuleField.Label"));
    PropsUi.setLook(wlInclModuleField);
    FormData fdlInclModuleField = new FormData();
    fdlInclModuleField.left = new FormAttachment(wInclModule, margin);
    fdlInclModuleField.top = new FormAttachment(wInclURLField, margin);
    wlInclModuleField.setLayoutData(fdlInclModuleField);
    wInclModuleField =
        new TextVar(variables, wAdditionalFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wInclModuleField);
    wInclModuleField.addModifyListener(lsMod);
    FormData fdInclModuleField = new FormData();
    fdInclModuleField.left = new FormAttachment(wlInclModuleField, margin);
    fdInclModuleField.top = new FormAttachment(wInclURLField, margin);
    fdInclModuleField.right = new FormAttachment(100, 0);
    wInclModuleField.setLayoutData(fdInclModuleField);

    // Add SQL in the output stream ?
    Label wlInclSQL = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclSQL.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.InclSQL.Label"));
    PropsUi.setLook(wlInclSQL);
    FormData fdlInclSQL = new FormData();
    fdlInclSQL.left = new FormAttachment(0, 0);
    fdlInclSQL.top = new FormAttachment(wInclModuleField, margin);
    fdlInclSQL.right = new FormAttachment(middle, -margin);
    wlInclSQL.setLayoutData(fdlInclSQL);
    wInclSQL = new Button(wAdditionalFields, SWT.CHECK);
    PropsUi.setLook(wInclSQL);
    wInclSQL.setToolTipText(BaseMessages.getString(PKG, "SalesforceInputDialog.InclSQL.Tooltip"));
    FormData fdInclSQL = new FormData();
    fdInclSQL.left = new FormAttachment(middle, 0);
    fdInclSQL.top = new FormAttachment(wlInclSQL, 0, SWT.CENTER);
    wInclSQL.setLayoutData(fdInclSQL);
    wInclSQL.addSelectionListener(checkBoxModifyListener);
    wInclSQL.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setEnableInclSQL();
            input.setChanged();
          }
        });

    wlInclSQLField = new Label(wAdditionalFields, SWT.LEFT);
    wlInclSQLField.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.InclSQLField.Label"));
    PropsUi.setLook(wlInclSQLField);
    FormData fdlInclSQLField = new FormData();
    fdlInclSQLField.left = new FormAttachment(wInclSQL, margin);
    fdlInclSQLField.top = new FormAttachment(wInclModuleField, margin);
    wlInclSQLField.setLayoutData(fdlInclSQLField);
    wInclSQLField = new TextVar(variables, wAdditionalFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wlInclSQLField);
    wInclSQLField.addModifyListener(lsMod);
    FormData fdInclSQLField = new FormData();
    fdInclSQLField.left = new FormAttachment(wlInclSQLField, margin);
    fdInclSQLField.top = new FormAttachment(wInclModuleField, margin);
    fdInclSQLField.right = new FormAttachment(100, 0);
    wInclSQLField.setLayoutData(fdInclSQLField);

    // Add Timestamp in the output stream ?
    Label wlInclTimestamp = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclTimestamp.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclTimestamp.Label"));
    PropsUi.setLook(wlInclTimestamp);
    FormData fdlInclTimestamp = new FormData();
    fdlInclTimestamp.left = new FormAttachment(0, 0);
    fdlInclTimestamp.top = new FormAttachment(wInclSQLField, margin);
    fdlInclTimestamp.right = new FormAttachment(middle, -margin);
    wlInclTimestamp.setLayoutData(fdlInclTimestamp);
    wInclTimestamp = new Button(wAdditionalFields, SWT.CHECK);
    PropsUi.setLook(wInclTimestamp);
    wInclTimestamp.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclTimestamp.Tooltip"));
    FormData fdInclTimestamp = new FormData();
    fdInclTimestamp.left = new FormAttachment(middle, 0);
    fdInclTimestamp.top = new FormAttachment(wlInclTimestamp, 0, SWT.CENTER);
    wInclTimestamp.setLayoutData(fdInclTimestamp);
    wInclTimestamp.addSelectionListener(checkBoxModifyListener);
    wInclTimestamp.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setEnableInclTimestamp();
            input.setChanged();
          }
        });

    wlInclTimestampField = new Label(wAdditionalFields, SWT.LEFT);
    wlInclTimestampField.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclTimestampField.Label"));
    PropsUi.setLook(wlInclTimestampField);
    FormData fdlInclTimestampField = new FormData();
    fdlInclTimestampField.left = new FormAttachment(wInclTimestamp, margin);
    fdlInclTimestampField.top = new FormAttachment(wInclSQLField, margin);
    wlInclTimestampField.setLayoutData(fdlInclTimestampField);
    wInclTimestampField =
        new TextVar(variables, wAdditionalFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wlInclTimestampField);
    wInclTimestampField.addModifyListener(lsMod);
    FormData fdInclTimestampField = new FormData();
    fdInclTimestampField.left = new FormAttachment(wlInclTimestampField, margin);
    fdInclTimestampField.top = new FormAttachment(wInclSQLField, margin);
    fdInclTimestampField.right = new FormAttachment(100, 0);
    wInclTimestampField.setLayoutData(fdInclTimestampField);

    // Include Rownum in output stream?
    Label wlInclRownum = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclRownum.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.InclRownum.Label"));
    PropsUi.setLook(wlInclRownum);
    FormData fdlInclRownum = new FormData();
    fdlInclRownum.left = new FormAttachment(0, 0);
    fdlInclRownum.top = new FormAttachment(wInclTimestampField, margin);
    fdlInclRownum.right = new FormAttachment(middle, -margin);
    wlInclRownum.setLayoutData(fdlInclRownum);
    wInclRownum = new Button(wAdditionalFields, SWT.CHECK);
    PropsUi.setLook(wInclRownum);
    wInclRownum.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclRownum.Tooltip"));
    FormData fdRownum = new FormData();
    fdRownum.left = new FormAttachment(middle, 0);
    fdRownum.top = new FormAttachment(wlInclRownum, 0, SWT.CENTER);
    wInclRownum.setLayoutData(fdRownum);
    wInclRownum.addSelectionListener(checkBoxModifyListener);

    wInclRownum.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setEnableInclRownum();
            input.setChanged();
          }
        });

    wlInclRownumField = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclRownumField.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclRownumField.Label"));
    PropsUi.setLook(wlInclRownumField);
    FormData fdlInclRownumField = new FormData();
    fdlInclRownumField.left = new FormAttachment(wInclRownum, margin);
    fdlInclRownumField.top = new FormAttachment(wInclTimestampField, margin);
    wlInclRownumField.setLayoutData(fdlInclRownumField);
    wInclRownumField =
        new TextVar(variables, wAdditionalFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wInclRownumField);
    wInclRownumField.addModifyListener(lsMod);
    FormData fdInclRownumField = new FormData();
    fdInclRownumField.left = new FormAttachment(wlInclRownumField, margin);
    fdInclRownumField.top = new FormAttachment(wInclTimestampField, margin);
    fdInclRownumField.right = new FormAttachment(100, 0);
    wInclRownumField.setLayoutData(fdInclRownumField);

    // Include DeletionDate in output stream?
    wlInclDeletionDate = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclDeletionDate.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclDeletionDate.Label"));
    PropsUi.setLook(wlInclDeletionDate);
    FormData fdlInclDeletionDate = new FormData();
    fdlInclDeletionDate.left = new FormAttachment(0, 0);
    fdlInclDeletionDate.top = new FormAttachment(wInclRownumField, margin);
    fdlInclDeletionDate.right = new FormAttachment(middle, -margin);
    wlInclDeletionDate.setLayoutData(fdlInclDeletionDate);
    wInclDeletionDate = new Button(wAdditionalFields, SWT.CHECK);
    PropsUi.setLook(wInclDeletionDate);
    wInclDeletionDate.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclDeletionDate.Tooltip"));
    FormData fdDeletionDate = new FormData();
    fdDeletionDate.left = new FormAttachment(middle, 0);
    fdDeletionDate.top = new FormAttachment(wlInclDeletionDate, 0, SWT.CENTER);
    wInclDeletionDate.setLayoutData(fdDeletionDate);
    wInclDeletionDate.addSelectionListener(checkBoxModifyListener);

    wInclDeletionDate.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setEnableInclDeletionDate();
            input.setChanged();
          }
        });

    wlInclDeletionDateField = new Label(wAdditionalFields, SWT.RIGHT);
    wlInclDeletionDateField.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.InclDeletionDateField.Label"));
    PropsUi.setLook(wlInclDeletionDateField);
    FormData fdlInclDeletionDateField = new FormData();
    fdlInclDeletionDateField.left = new FormAttachment(wInclDeletionDate, margin);
    fdlInclDeletionDateField.top = new FormAttachment(wInclRownumField, margin);
    wlInclDeletionDateField.setLayoutData(fdlInclDeletionDateField);
    wInclDeletionDateField =
        new TextVar(variables, wAdditionalFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wInclDeletionDateField);
    wInclDeletionDateField.addModifyListener(lsMod);
    FormData fdInclDeletionDateField = new FormData();
    fdInclDeletionDateField.left = new FormAttachment(wlInclDeletionDateField, margin);
    fdInclDeletionDateField.top = new FormAttachment(wInclRownumField, margin);
    fdInclDeletionDateField.right = new FormAttachment(100, 0);
    wInclDeletionDateField.setLayoutData(fdInclDeletionDateField);

    FormData fdAdditionalFields = new FormData();
    fdAdditionalFields.left = new FormAttachment(0, margin);
    fdAdditionalFields.top = new FormAttachment(wAdvancedGroup, margin);
    fdAdditionalFields.right = new FormAttachment(100, -margin);
    wAdditionalFields.setLayoutData(fdAdditionalFields);

    // ///////////////////////////////////////////////////////////
    // / END OF Additional Fields GROUP
    // ///////////////////////////////////////////////////////////

    // Timeout
    Label wlTimeOut = new Label(wContentComp, SWT.RIGHT);
    wlTimeOut.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.TimeOut.Label"));
    PropsUi.setLook(wlTimeOut);
    FormData fdlTimeOut = new FormData();
    fdlTimeOut.left = new FormAttachment(0, 0);
    fdlTimeOut.top = new FormAttachment(wAdditionalFields, 2 * margin);
    fdlTimeOut.right = new FormAttachment(middle, -margin);
    wlTimeOut.setLayoutData(fdlTimeOut);
    wTimeOut = new TextVar(variables, wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wTimeOut);
    wTimeOut.addModifyListener(lsMod);
    FormData fdTimeOut = new FormData();
    fdTimeOut.left = new FormAttachment(middle, 0);
    fdTimeOut.top = new FormAttachment(wAdditionalFields, 2 * margin);
    fdTimeOut.right = new FormAttachment(100, 0);
    wTimeOut.setLayoutData(fdTimeOut);

    // Use compression?
    Label wlUseCompression = new Label(wContentComp, SWT.RIGHT);
    wlUseCompression.setText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.UseCompression.Label"));
    PropsUi.setLook(wlUseCompression);
    FormData fdlUseCompression = new FormData();
    fdlUseCompression.left = new FormAttachment(0, 0);
    fdlUseCompression.top = new FormAttachment(wTimeOut, margin);
    fdlUseCompression.right = new FormAttachment(middle, -margin);
    wlUseCompression.setLayoutData(fdlUseCompression);
    wUseCompression = new Button(wContentComp, SWT.CHECK);
    wUseCompression.addSelectionListener(checkBoxModifyListener);
    PropsUi.setLook(wUseCompression);
    wUseCompression.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceInputDialog.UseCompression.Tooltip"));
    FormData fdUseCompression = new FormData();
    fdUseCompression.left = new FormAttachment(middle, 0);
    fdUseCompression.top = new FormAttachment(wlUseCompression, 0, SWT.CENTER);
    wUseCompression.setLayoutData(fdUseCompression);
    wUseCompression.addSelectionListener(new ComponentSelectionListener(input));

    // Limit rows
    Label wlLimit = new Label(wContentComp, SWT.RIGHT);
    wlLimit.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.Limit.Label"));
    PropsUi.setLook(wlLimit);
    FormData fdlLimit = new FormData();
    fdlLimit.left = new FormAttachment(0, 0);
    fdlLimit.top = new FormAttachment(wUseCompression, margin);
    fdlLimit.right = new FormAttachment(middle, -margin);
    wlLimit.setLayoutData(fdlLimit);
    wLimit = new TextVar(variables, wContentComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wLimit);
    wLimit.addModifyListener(lsMod);
    FormData fdLimit = new FormData();
    fdLimit.left = new FormAttachment(middle, 0);
    fdLimit.top = new FormAttachment(wUseCompression, margin);
    fdLimit.right = new FormAttachment(100, 0);
    wLimit.setLayoutData(fdLimit);

    FormData fdContentComp = new FormData();
    fdContentComp.left = new FormAttachment(0, 0);
    fdContentComp.top = new FormAttachment(0, 0);
    fdContentComp.right = new FormAttachment(100, 0);
    fdContentComp.bottom = new FormAttachment(100, 0);
    wContentComp.setLayoutData(fdContentComp);

    wContentComp.layout();
    wContentTab.setControl(wContentComp);

    // ///////////////////////////////////////////////////////////
    // / END OF CONTENT TAB
    // ///////////////////////////////////////////////////////////

    // Fields tab...
    //
    CTabItem wFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
    wFieldsTab.setFont(GuiResource.getInstance().getFontDefault());
    wFieldsTab.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.Fields.Tab"));

    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth = PropsUi.getFormMargin();
    fieldsLayout.marginHeight = PropsUi.getFormMargin();

    Composite wFieldsComp = new Composite(wTabFolder, SWT.NONE);
    wFieldsComp.setLayout(fieldsLayout);
    PropsUi.setLook(wFieldsComp);

    wGet = new Button(wFieldsComp, SWT.PUSH);
    wGet.setText(BaseMessages.getString(PKG, "SalesforceInputDialog.GetFields.Button"));
    fdGet = new FormData();
    fdGet.left = new FormAttachment(50, 0);
    fdGet.bottom = new FormAttachment(100, 0);
    wGet.setLayoutData(fdGet);

    final int FieldsRows = input.getInputFields().length;

    columns =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Name.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Field.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              new String[] {""},
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.IsIdLookup.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              new String[] {
                BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES),
                BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO)
              },
              true),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Type.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              ValueMetaFactory.getValueMetaNames(),
              true),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Format.Column"),
              ColumnInfo.COLUMN_TYPE_FORMAT,
              4),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Length.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Precision.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Currency.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Decimal.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Group.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.TrimType.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              SalesforceInputField.trimTypeDesc,
              true),
          new ColumnInfo(
              BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Repeat.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              new String[] {
                BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES),
                BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO)
              },
              true),
        };

    columns[0].setUsingVariables(true);
    columns[0].setToolTip(
        BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Name.Column.Tooltip"));
    columns[1].setUsingVariables(true);
    columns[1].setToolTip(
        BaseMessages.getString(PKG, "SalesforceInputDialog.FieldsTable.Field.Column.Tooltip"));
    columns[2].setReadOnly(true);
    wFields =
        new TableView(
            variables,
            wFieldsComp,
            SWT.FULL_SELECTION | SWT.MULTI,
            columns,
            FieldsRows,
            lsMod,
            props);

    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment(0, 0);
    fdFields.top = new FormAttachment(0, 0);
    fdFields.right = new FormAttachment(100, 0);
    fdFields.bottom = new FormAttachment(wGet, -margin);
    wFields.setLayoutData(fdFields);

    FormData fdFieldsComp = new FormData();
    fdFieldsComp.left = new FormAttachment(0, 0);
    fdFieldsComp.top = new FormAttachment(0, 0);
    fdFieldsComp.right = new FormAttachment(100, 0);
    fdFieldsComp.bottom = new FormAttachment(100, 0);
    wFieldsComp.setLayoutData(fdFieldsComp);

    wFieldsComp.layout();
    wFieldsTab.setControl(wFieldsComp);

    // Add listeners

    wGet.addListener(
        SWT.Selection,
        e -> {
          Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
          shell.setCursor(busy);
          get();
          shell.setCursor(null);
          busy.dispose();
          input.setChanged();
        });
    wTest.addListener(SWT.Selection, e -> test());

    wTabFolder.setSelection(0);

    getData(input);
    setEnableInclTargetURL();
    setEnableInclSQL();
    setEnableInclTimestamp();
    setEnableInclModule();
    setEnableInclRownum();
    setEnableInclDeletionDate();
    setEnableQuery();

    input.setChanged(changed);

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private void setEnableInclTargetURL() {
    wInclURLField.setEnabled(wInclURL.getSelection());
    wlInclURLField.setEnabled(wInclURL.getSelection());
  }

  private void setEnableQuery() {
    wlQuery.setVisible(wSpecifyQuery.getSelection());
    wQuery.setVisible(wSpecifyQuery.getSelection());
    wlCondition.setVisible(!wSpecifyQuery.getSelection());
    wCondition.setVisible(!wSpecifyQuery.getSelection());
    wlModule.setVisible(!wSpecifyQuery.getSelection());
    wModule.setVisible(!wSpecifyQuery.getSelection());

    if (wSpecifyQuery.getSelection()) {
      if (wInclModule.getSelection()) {
        wInclModule.setSelection(false);
      }
      wRecordsFilter.setText(
          SalesforceConnectionUtils.getRecordsFilterDesc(
              SalesforceConnectionUtils.RECORDS_FILTER_ALL));
    }
    wlInclModule.setEnabled(!wSpecifyQuery.getSelection());
    wInclModule.setEnabled(!wSpecifyQuery.getSelection());
    setEnableInclModule();
    wlRecordsFilter.setEnabled(!wSpecifyQuery.getSelection());
    wRecordsFilter.setEnabled(!wSpecifyQuery.getSelection());
    updateRecordsFilter();
    enableCondition();
    shell.layout(true, true);
  }

  private void setEnableInclSQL() {
    wInclSQLField.setEnabled(wInclSQL.getSelection());
    wlInclSQLField.setEnabled(wInclSQL.getSelection());
  }

  private void setEnableInclTimestamp() {
    wInclTimestampField.setEnabled(wInclTimestamp.getSelection());
    wlInclTimestampField.setEnabled(wInclTimestamp.getSelection());
  }

  private void setEnableInclModule() {
    wInclModuleField.setEnabled(wInclModule.getSelection() && !wSpecifyQuery.getSelection());
    wlInclModuleField.setEnabled(wInclModule.getSelection() && !wSpecifyQuery.getSelection());
  }

  private void setEnableInclRownum() {
    wInclRownumField.setEnabled(wInclRownum.getSelection());
    wlInclRownumField.setEnabled(wInclRownum.getSelection());
  }

  private void setEnableInclDeletionDate() {
    wInclDeletionDateField.setEnabled(wInclDeletionDate.getSelection());
    wlInclDeletionDateField.setEnabled(wInclDeletionDate.getSelection());
  }

  private void get() {
    SalesforceConnection connection = null;
    try {

      SalesforceInputMeta meta = new SalesforceInputMeta();
      getInfo(meta);

      // Clear Fields Grid
      wFields.removeAll();

      // get real values
      String realModule = variables.resolve(meta.getModule());
      String realURL = variables.resolve(meta.getTargetUrl());
      String realUsername = variables.resolve(meta.getUsername());
      String realPassword = Utils.resolvePassword(variables, meta.getPassword());
      int realTimeOut = Const.toInt(variables.resolve(meta.getTimeout()), 0);

      connection = new SalesforceConnection(log, realURL, realUsername, realPassword);
      connection.setTimeOut(realTimeOut);
      String[] fieldsName = null;
      if (meta.isSpecifyQuery()) {
        // Free hand SOQL
        String realQuery = variables.resolve(meta.getQuery());
        connection.setSQL(realQuery);
        connection.connect();
        // We are connected, so let's query
        XmlObject[] fields = connection.getElements();
        if (fields != null) {
          int nrFields = fields.length;
          Set<String> fieldNames = new HashSet<>();
          for (int i = 0; i < nrFields; i++) {
            addFields("", fieldNames, fields[i]);
          }
          fieldsName = fieldNames.toArray(new String[fieldNames.size()]);
        } else {
          MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
          mb.setMessage(
              BaseMessages.getString(
                  PKG, "SalesforceInputDialog.GetFields.SOQL.NoRecords.DialogMessage"));
          mb.setText(
              BaseMessages.getString(
                  PKG, "SalesforceInputDialog.GetFields.SOQL.NoRecords.DialogTitle"));
          mb.open();
        }
      } else {
        connection.connect();
        Field[] fields = connection.getObjectFields(realModule);
        if (fields != null) {
          fieldsName = new String[fields.length];
          for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            fieldsName[i] = field.getName();
            addField(field);
          }
        } else {
          MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
          mb.setMessage(
              BaseMessages.getString(
                  PKG, "SalesforceInputDialog.GetFields.Condition.NoRecords.DialogMessage"));
          mb.setText(
              BaseMessages.getString(
                  PKG, "SalesforceInputDialog.GetFields.Condition.NoRecords.DialogTitle"));
          mb.open();
        }
      }
      if (fieldsName != null) {
        columns[1].setComboValues(fieldsName);
        wFields.removeEmptyRows();
        wFields.setRowNums();
        wFields.optWidth(true);
      }
    } catch (HopException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "SalesforceInputMeta.ErrorRetrieveData.DialogTitle"),
          BaseMessages.getString(PKG, "SalesforceInputMeta.ErrorRetrieveData.DialogMessage"),
          e);
    } catch (Exception e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "SalesforceInputMeta.ErrorRetrieveData.DialogTitle"),
          BaseMessages.getString(PKG, "SalesforceInputMeta.ErrorRetrieveData.DialogMessage"),
          e);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (Exception e) {
          // Ignore errors
        }
      }
    }
  }

  void addFields(String prefix, Set<String> fieldNames, XmlObject field) {
    // Salesforce SOAP Api sends IDs always in the response, even if we don't request it in SOQL
    // query and
    // the id's value is null in this case. So, do not add this Id to the fields list
    if (isNullIdField(field)) {
      return;
    }
    String fieldname = prefix + field.getName().getLocalPart();
    if (field instanceof SObject sobject) {
      for (XmlObject element : SalesforceConnection.getChildren(sobject)) {
        addFields(fieldname + ".", fieldNames, element);
      }
    } else {
      addField(fieldname, fieldNames, (String) field.getValue());
    }
  }

  private boolean isNullIdField(XmlObject field) {
    return field.getName().getLocalPart().equalsIgnoreCase("ID") && field.getValue() == null;
  }

  private void addField(Field field) {
    String fieldType = field.getType().toString();

    String fieldLength = null;
    String fieldPrecision = null;
    if (!fieldType.equals(CONST_BOOLEAN)
        && !fieldType.equals(CONST_DATETIME)
        && !fieldType.equals("date")) {
      fieldLength = Integer.toString(field.getLength());
      fieldPrecision = Integer.toString(field.getPrecision());
    }

    addFieldToTable(
        field.getLabel(),
        field.getName(),
        field.isIdLookup(),
        field.getType().toString(),
        fieldLength,
        fieldPrecision);
  }

  private void addField(String fieldName, Set<String> fieldNames, String firstValue) {
    // no duplicates allowed
    if (!fieldNames.add(fieldName)) {
      return;
    }

    // Try to guess field type
    // I know it's not clean (see up)
    final String fieldType;
    String fieldLength = null;
    String fieldPrecision = null;
    if (Const.NVL(firstValue, "null").equals("null")) {
      fieldType = "string";
    } else {
      if (StringUtil.isDate(firstValue, DEFAULT_DATE_TIME_FORMAT)) {
        fieldType = CONST_DATETIME;
      } else if (StringUtil.isDate(firstValue, DEFAULT_DATE_FORMAT)) {
        fieldType = "date";
      } else if (StringUtil.isInteger(firstValue)) {
        fieldType = "int";
        fieldLength = Integer.toString(IValueMeta.DEFAULT_INTEGER_LENGTH);
      } else if (StringUtil.isNumber(firstValue)) {
        fieldType = "double";
      } else if (firstValue.equals("true") || firstValue.equals("false")) {
        fieldType = CONST_BOOLEAN;
      } else {
        fieldType = "string";
      }
    }
    addFieldToTable(fieldName, fieldName, false, fieldType, fieldLength, fieldPrecision);
  }

  void addFieldToTable(
      String fieldLabel,
      String fieldName,
      boolean fieldIdIsLookup,
      String fieldType,
      String fieldLength,
      String fieldPrecision) {
    TableItem item = new TableItem(wFields.table, SWT.NONE);
    item.setText(1, fieldLabel);
    item.setText(2, fieldName);
    item.setText(
        3,
        fieldIdIsLookup
            ? BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES)
            : BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO));

    // Try to get the Type
    if (fieldType.equals(CONST_BOOLEAN)) {
      item.setText(4, "Boolean");
    } else if (fieldType.equals("date")) {
      item.setText(4, "Date");
      item.setText(5, DEFAULT_DATE_FORMAT);
    } else if (fieldType.equals(CONST_DATETIME)) {
      item.setText(4, "Date");
      item.setText(5, DEFAULT_DATE_TIME_FORMAT);
    } else if (fieldType.equals("double")) {
      item.setText(4, "Number");
    } else if (fieldType.equals("int")) {
      item.setText(4, "Integer");
    } else if (fieldType.equals("base64")) {
      item.setText(4, "Binary");
    } else {
      item.setText(4, "String");
    }

    if (fieldLength != null) {
      item.setText(6, fieldLength);
    }
    // Get precision
    if (fieldPrecision != null) {
      item.setText(7, fieldPrecision);
    }
  }

  private void updateRecordsFilter() {
    boolean activeFilter =
        (!wSpecifyQuery.getSelection()
            && SalesforceConnectionUtils.getRecordsFilterByDesc(wRecordsFilter.getText())
                != SalesforceConnectionUtils.RECORDS_FILTER_ALL);

    wlReadFrom.setEnabled(activeFilter);
    wReadFrom.setEnabled(activeFilter);
    open.setEnabled(activeFilter);
    wlReadTo.setEnabled(activeFilter);
    wReadTo.setEnabled(activeFilter);
    opento.setEnabled(activeFilter);
    wlQueryAll.setEnabled(!activeFilter);
    wQueryAll.setEnabled(!activeFilter);
    enableCondition();
    boolean activateDeletionDate =
        SalesforceConnectionUtils.getRecordsFilterByDesc(wRecordsFilter.getText())
            == SalesforceConnectionUtils.RECORDS_FILTER_DELETED;
    if (!activateDeletionDate) {
      wInclDeletionDate.setSelection(false);
    }
    wlInclDeletionDate.setEnabled(activateDeletionDate);
    wInclDeletionDate.setEnabled(activateDeletionDate);
    wlInclDeletionDateField.setEnabled(activateDeletionDate);
    wInclDeletionDateField.setEnabled(activateDeletionDate);
  }

  /**
   * Read the data from the SalesforceInputMeta object and show it in this dialog.
   *
   * @param in The SalesforceInputMeta object to obtain the data from.
   */
  public void getData(SalesforceInputMeta in) {
    wURL.setText(Const.NVL(in.getTargetUrl(), ""));
    wUserName.setText(Const.NVL(in.getUsername(), ""));
    wPassword.setText(Const.NVL(in.getPassword(), ""));
    wModule.setText(Const.NVL(in.getModule(), "Account"));
    wCondition.setText(Const.NVL(in.getCondition(), ""));

    wSpecifyQuery.setSelection(in.isSpecifyQuery());
    wQuery.setText(Const.NVL(in.getQuery(), ""));
    wRecordsFilter.setText(
        SalesforceConnectionUtils.getRecordsFilterDesc(input.getRecordsFilter()));
    wInclURLField.setText(Const.NVL(in.getTargetURLField(), ""));
    wInclURL.setSelection(in.includeTargetURL());

    wInclSQLField.setText(Const.NVL(in.getSQLField(), ""));
    wInclSQL.setSelection(in.includeSQL());

    wInclTimestampField.setText(Const.NVL(in.getTimestampField(), ""));
    wInclTimestamp.setSelection(in.includeTimestamp());
    wInclDeletionDateField.setText(Const.NVL(in.getDeletionDateField(), ""));
    wInclDeletionDate.setSelection(in.includeDeletionDate());

    wInclModuleField.setText(Const.NVL(in.getModuleField(), ""));
    wInclModule.setSelection(in.includeModule());

    wInclRownumField.setText(Const.NVL(in.getRowNumberField(), ""));
    wInclRownum.setSelection(in.includeRowNumber());

    wTimeOut.setText(Const.NVL(in.getTimeout(), SalesforceConnectionUtils.DEFAULT_TIMEOUT));
    wUseCompression.setSelection(in.isCompression());
    wQueryAll.setSelection(in.isQueryAll());
    wLimit.setText("" + in.getRowLimit());

    wReadFrom.setText(Const.NVL(in.getReadFrom(), ""));
    wReadTo.setText(Const.NVL(in.getReadTo(), ""));

    if (log.isDebug()) {
      logDebug(BaseMessages.getString(PKG, "SalesforceInputDialog.Log.GettingFieldsInfo"));
    }
    for (int i = 0; i < in.getInputFields().length; i++) {
      SalesforceInputField field = in.getInputFields()[i];

      if (field != null) {
        TableItem item = wFields.table.getItem(i);
        String name = field.getName();
        String path = field.getField();
        String isidlookup =
            field.isIdLookup()
                ? BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES)
                : BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO);
        String type = field.getTypeDesc();
        String format = field.getFormat();
        String length = "" + field.getLength();
        String prec = "" + field.getPrecision();
        String curr = field.getCurrencySymbol();
        String group = field.getGroupSymbol();
        String decim = field.getDecimalSymbol();
        String trim = field.getTrimTypeDesc();
        String rep =
            field.isRepeated()
                ? BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES)
                : BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_NO);

        if (name != null) {
          item.setText(1, name);
        }
        if (path != null) {
          item.setText(2, path);
        }
        if (isidlookup != null) {
          item.setText(3, isidlookup);
        }
        if (type != null) {
          item.setText(4, type);
        }
        if (format != null) {
          item.setText(5, format);
        }
        if (length != null && !"-1".equals(length)) {
          item.setText(6, length);
        }
        if (prec != null && !"-1".equals(prec)) {
          item.setText(7, prec);
        }
        if (curr != null) {
          item.setText(8, curr);
        }
        if (decim != null) {
          item.setText(9, decim);
        }
        if (group != null) {
          item.setText(10, group);
        }
        if (trim != null) {
          item.setText(11, trim);
        }
        if (rep != null) {
          item.setText(12, rep);
        }
      }
    }

    wFields.removeEmptyRows();
    wFields.setRowNums();
    wFields.optWidth(true);

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    transformName = wTransformName.getText();
    try {
      getInfo(input);
    } catch (HopException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "SalesforceInputDialog.ErrorValidateData.DialogTitle"),
          BaseMessages.getString(PKG, "SalesforceInputDialog.ErrorValidateData.DialogMessage"),
          e);
    }
    dispose();
  }

  @Override
  protected void getInfo(SalesforceTransformMeta in) throws HopException {
    SalesforceInputMeta meta = (SalesforceInputMeta) in;

    transformName = wTransformName.getText(); // return value

    // copy info to SalesforceInputMeta class (input)
    meta.setTargetUrl(Const.NVL(wURL.getText(), SalesforceConnectionUtils.TARGET_DEFAULT_URL));
    meta.setUsername(Const.NVL(wUserName.getText(), ""));
    meta.setPassword(Const.NVL(wPassword.getText(), ""));
    meta.setModule(Const.NVL(wModule.getText(), "Account"));
    meta.setCondition(Const.NVL(wCondition.getText(), ""));

    meta.setSpecifyQuery(wSpecifyQuery.getSelection());
    meta.setQuery(Const.NVL(wQuery.getText(), ""));
    meta.setCompression(wUseCompression.getSelection());
    meta.setQueryAll(wQueryAll.getSelection());
    meta.setTimeout(Const.NVL(wTimeOut.getText(), "0"));
    meta.setRowLimit(Const.NVL(wLimit.getText(), "0"));
    meta.setTargetURLField(Const.NVL(wInclURLField.getText(), ""));
    meta.setSQLField(Const.NVL(wInclSQLField.getText(), ""));
    meta.setTimestampField(Const.NVL(wInclTimestampField.getText(), ""));
    meta.setModuleField(Const.NVL(wInclModuleField.getText(), ""));
    meta.setRowNumberField(Const.NVL(wInclRownumField.getText(), ""));
    meta.setRecordsFilter(
        SalesforceConnectionUtils.getRecordsFilterByDesc(wRecordsFilter.getText()));
    meta.setIncludeTargetURL(wInclURL.getSelection());
    meta.setIncludeSQL(wInclSQL.getSelection());
    meta.setIncludeTimestamp(wInclTimestamp.getSelection());
    meta.setIncludeModule(wInclModule.getSelection());
    meta.setIncludeRowNumber(wInclRownum.getSelection());
    meta.setReadFrom(wReadFrom.getText());
    meta.setReadTo(wReadTo.getText());
    meta.setDeletionDateField(Const.NVL(wInclDeletionDateField.getText(), ""));
    meta.setIncludeDeletionDate(wInclDeletionDate.getSelection());
    int nrFields = wFields.nrNonEmpty();

    meta.allocate(nrFields);

    for (int i = 0; i < nrFields; i++) {
      SalesforceInputField field = new SalesforceInputField();

      TableItem item = wFields.getNonEmpty(i);

      field.setName(item.getText(1));
      field.setField(item.getText(2));
      field.setIdLookup(
          BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES).equalsIgnoreCase(item.getText(3)));
      field.setType(ValueMetaFactory.getIdForValueMeta(item.getText(4)));
      field.setFormat(item.getText(5));
      field.setLength(Const.toInt(item.getText(6), -1));
      field.setPrecision(Const.toInt(item.getText(7), -1));
      field.setCurrencySymbol(item.getText(8));
      field.setDecimalSymbol(item.getText(9));
      field.setGroupSymbol(item.getText(10));
      field.setTrimType(SalesforceInputField.getTrimTypeByDesc(item.getText(11)));
      field.setRepeated(
          BaseMessages.getString(PKG, CONST_SYSTEM_COMBO_YES).equalsIgnoreCase(item.getText(12)));

      meta.getInputFields()[i] = field;
    }
  }

  // Preview the data
  private void preview() {
    try {
      SalesforceInputMeta oneMeta = new SalesforceInputMeta();
      getInfo(oneMeta);

      // check if the path is given

      EnterNumberDialog numberDialog =
          new EnterNumberDialog(
              shell,
              props.getDefaultPreviewSize(),
              BaseMessages.getString(PKG, "SalesforceInputDialog.NumberRows.DialogTitle"),
              BaseMessages.getString(PKG, "SalesforceInputDialog.NumberRows.DialogMessage"));
      int previewSize = numberDialog.open();
      if (previewSize > 0) {
        oneMeta.setRowLimit(Integer.toString(previewSize));
        PipelineMeta previewMeta =
            PipelinePreviewFactory.generatePreviewPipeline(
                pipelineMeta.getMetadataProvider(), oneMeta, wTransformName.getText());

        PipelinePreviewProgressDialog progressDialog =
            new PipelinePreviewProgressDialog(
                shell,
                variables,
                previewMeta,
                new String[] {wTransformName.getText()},
                new int[] {previewSize});
        progressDialog.open();

        if (!progressDialog.isCancelled()) {
          Pipeline pipeline = progressDialog.getPipeline();
          String loggingText = progressDialog.getLoggingText();

          if (pipeline.getResult() != null && pipeline.getResult().getNrErrors() > 0) {
            EnterTextDialog etd =
                new EnterTextDialog(
                    shell,
                    BaseMessages.getString(PKG, "System.Dialog.PreviewError.Title"),
                    BaseMessages.getString(PKG, "System.Dialog.PreviewError.Message"),
                    loggingText,
                    true);
            etd.setReadOnly();
            etd.open();
          }

          PreviewRowsDialog prd =
              new PreviewRowsDialog(
                  shell,
                  variables,
                  SWT.NONE,
                  wTransformName.getText(),
                  progressDialog.getPreviewRowsMeta(wTransformName.getText()),
                  progressDialog.getPreviewRows(wTransformName.getText()),
                  loggingText);
          prd.open();
        }
      }
    } catch (HopException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "SalesforceInputDialog.ErrorPreviewingData.DialogTitle"),
          BaseMessages.getString(PKG, "SalesforceInputDialog.ErrorPreviewingData.DialogMessage"),
          e);
    }
  }

  private void getModulesList() {
    if (!gotModule) {
      SalesforceConnection connection = null;
      String selectedField = wModule.getText();
      wModule.removeAll();

      try {
        SalesforceInputMeta meta = new SalesforceInputMeta();
        getInfo(meta);
        String url = variables.resolve(meta.getTargetUrl());

        // Define a new Salesforce connection
        connection =
            new SalesforceConnection(
                log,
                url,
                variables.resolve(meta.getUsername()),
                Utils.resolvePassword(variables, meta.getPassword()));
        // connect to Salesforce
        connection.connect();

        // retrieve modules list
        String[] modules = connection.getAllAvailableObjects(true);
        if (modules != null && modules.length > 0) {
          // populate Combo
          wModule.setItems(modules);
        }

        gotModule = true;
        getModulesListError = false;
      } catch (Exception e) {
        new ErrorDialog(
            shell,
            BaseMessages.getString(PKG, "SalesforceInputDialog.ErrorRetrieveModules.DialogTitle"),
            BaseMessages.getString(
                PKG, "SalesforceInputDialog.ErrorRetrieveData.ErrorRetrieveModules"),
            e);
        getModulesListError = true;
      } finally {
        if (!Utils.isEmpty(selectedField)) {
          wModule.setText(selectedField);
        }
        if (connection != null) {
          try {
            connection.close();
          } catch (Exception e) {
            /* Ignore */
          }
        }
      }
    }
  }

  protected void setQueryToolTip() {
    StyledTextComp control = wCondition;
    if (wSpecifyQuery.getSelection()) {
      control = wQuery;
    }
    control.setToolTipText(variables.resolve(control.getText()));
  }

  public void setPosition() {
    StyledTextComp control = wCondition;
    if (wSpecifyQuery.getSelection()) {
      control = wQuery;
    }

    int lineNumber = control.getLineNumber();
    int columnNumber = control.getColumnNumber();
    wlPosition.setText(
        BaseMessages.getString(
            PKG, "SalesforceInputDialog.Position.Label", "" + lineNumber, "" + columnNumber));
  }

  private void enableCondition() {
    boolean enableCondition =
        !wSpecifyQuery.getSelection()
            && SalesforceConnectionUtils.getRecordsFilterByDesc(wRecordsFilter.getText())
                == SalesforceConnectionUtils.RECORDS_FILTER_ALL;
    wlCondition.setVisible(enableCondition);
    wCondition.setVisible(enableCondition);
    wlPosition.setVisible(enableCondition);
  }
}
