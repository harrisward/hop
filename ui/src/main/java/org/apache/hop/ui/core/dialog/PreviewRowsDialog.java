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

package org.apache.hop.ui.core.dialog;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.apache.hop.core.Const;
import org.apache.hop.core.config.HopConfig;
import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

/** Displays an ArrayList of rows in a TableView. */
public class PreviewRowsDialog {
  private static final Class<?> PKG = PreviewRowsDialog.class;

  public static final int MAX_BINARY_STRING_PREVIEW_SIZE = 1000000;
  public static final boolean PREVIEW_AVOID_BINARY_IN_HEX =
      Const.toBoolean(
          HopConfig.readStringVariable(Const.HOP_BINARY_FIELDS_AVOID_HEX_PREVIEW, "false"));

  private String transformName;

  private Label wlFields;

  private TableView wFields;

  private Shell shell;

  private final List<Object[]> buffer;

  private String title;
  private String message;

  private Rectangle bounds;

  private int hscroll;
  private int vscroll;
  private int hmax;
  private int vmax;

  private final String loggingText;

  private boolean proposingToGetMoreRows;

  private boolean proposingToStop;

  private boolean askingForMoreRows;

  private boolean askingToStop;

  private IRowMeta rowMeta;

  private final IVariables variables;

  private final ILogChannel log;

  private boolean dynamic;

  private boolean waitingForRows;

  protected int lineNr;

  private int style = SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN;

  private final Shell parentShell;

  private final List<IDialogClosedListener> dialogClosedListeners;

  private Control bottomButton;

  public PreviewRowsDialog(
      Shell parent,
      IVariables variables,
      int style,
      String transformName,
      IRowMeta rowMeta,
      List<Object[]> rowBuffer) {
    this(parent, variables, style, transformName, rowMeta, rowBuffer, null);
  }

  public PreviewRowsDialog(
      Shell parent,
      IVariables variables,
      int style,
      String transformName,
      IRowMeta rowMeta,
      List<Object[]> rowBuffer,
      String loggingText) {
    this.transformName = transformName;
    this.buffer = rowBuffer;
    this.loggingText = loggingText;
    this.rowMeta = rowMeta;
    this.variables = variables;
    this.parentShell = parent;
    this.style = (style != SWT.None) ? style : this.style;
    this.dialogClosedListeners = new ArrayList<>();

    bounds = null;
    hscroll = -1;
    vscroll = -1;
    title = null;
    message = null;

    this.log = new LogChannel("Row Preview");
  }

  public void setTitleMessage(String title, String message) {
    this.title = title;
    this.message = message;
  }

  public void open() {
    shell = new Shell(parentShell, style);
    PropsUi props = PropsUi.getInstance();

    PropsUi.setLook(shell);
    shell.setImage(GuiResource.getInstance().getImageHopUi());

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    if (title == null) {
      title = BaseMessages.getString(PKG, "PreviewRowsDialog.Title");
    }
    if (message == null) {
      message = BaseMessages.getString(PKG, "PreviewRowsDialog.Header", transformName);
    }

    if (buffer != null) {
      message += " " + BaseMessages.getString(PKG, "PreviewRowsDialog.NrRows", "" + buffer.size());
    }

    shell.setLayout(formLayout);
    shell.setText(title);

    List<Button> buttons = new ArrayList<>();

    Button wClose = new Button(shell, SWT.PUSH);
    wClose.setText(BaseMessages.getString(PKG, "System.Button.Close"));
    wClose.addListener(SWT.Selection, e -> cancel());
    buttons.add(wClose);

    if (!Utils.isEmpty(loggingText)) {
      Button wLog = new Button(shell, SWT.PUSH);
      wLog.setText(BaseMessages.getString(PKG, "PreviewRowsDialog.Button.ShowLog"));
      wLog.addListener(SWT.Selection, e -> log());
      buttons.add(wLog);
    }

    if (proposingToStop) {
      Button wStop = new Button(shell, SWT.PUSH);
      wStop.setText(BaseMessages.getString(PKG, "PreviewRowsDialog.Button.Stop.Label"));
      wStop.setToolTipText(BaseMessages.getString(PKG, "PreviewRowsDialog.Button.Stop.ToolTip"));
      wStop.addListener(SWT.Selection, e -> cancel());
      buttons.add(wStop);
    }

    if (proposingToGetMoreRows) {
      Button wNext = new Button(shell, SWT.PUSH);
      wNext.setText(BaseMessages.getString(PKG, "PreviewRowsDialog.Button.Next.Label"));
      wNext.setToolTipText(BaseMessages.getString(PKG, "PreviewRowsDialog.Button.Next.ToolTip"));
      wNext.addListener(
          SWT.Selection,
          e -> {
            askingForMoreRows = true;
            close();
          });
      buttons.add(wNext);
    }

    if (proposingToGetMoreRows || proposingToStop) {
      wClose.setText(BaseMessages.getString(PKG, "PreviewRowsDialog.Button.Close.Label"));
      wClose.setToolTipText(BaseMessages.getString(PKG, "PreviewRowsDialog.Button.Close.ToolTip"));
    }

    // Position the buttons...
    //
    bottomButton = buttons.get(0);
    BaseTransformDialog.positionBottomButtons(
        shell, buttons.toArray(new Button[buttons.size()]), PropsUi.getMargin(), null);

    if (addFields()) {
      return;
    }

    KeyListener escapeListener =
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.keyCode == SWT.ESC) {
              cancel();
            }
          }
        };

    shell.addKeyListener(escapeListener);
    wFields.addKeyListener(escapeListener);
    wFields.table.addKeyListener(escapeListener);
    buttons.stream().forEach(b -> b.addKeyListener(escapeListener));

    getData();

    BaseDialog.defaultShellHandling(shell, c -> close(), c -> cancel());
  }

  private void cancel() {
    askingToStop = true;
    close();
  }

  private boolean addFields() {
    PropsUi props = PropsUi.getInstance();
    int margin = PropsUi.getMargin();

    if (wlFields == null) {
      wlFields = new Label(shell, SWT.LEFT);
      wlFields.setText(message);
      PropsUi.setLook(wlFields);
      FormData fdlFields = new FormData();
      fdlFields.left = new FormAttachment(0, 0);
      fdlFields.right = new FormAttachment(100, 0);
      fdlFields.top = new FormAttachment(0, margin);
      wlFields.setLayoutData(fdlFields);
    } else {
      wFields.dispose();
    }

    if (dynamic && rowMeta == null) {
      rowMeta = new RowMeta();
      rowMeta.addValueMeta(new ValueMetaString("<waiting for rows>"));
      waitingForRows = true;
    }
    if (!dynamic) {
      // Mmm, if we don't get any rows in the buffer: show a dialog box.
      if (buffer == null || buffer.isEmpty()) {
        ShowMessageDialog dialog =
            new ShowMessageDialog(
                shell,
                SWT.OK | SWT.ICON_WARNING,
                BaseMessages.getString(PKG, "PreviewRowsDialog.NoRows.Text"),
                BaseMessages.getString(PKG, "PreviewRowsDialog.NoRows.Message"));
        dialog.open();
        shell.dispose();
        return true;
      }
    }

    ColumnInfo[] columns = new ColumnInfo[rowMeta.size()];
    for (int i = 0; i < rowMeta.size(); i++) {
      IValueMeta valueMeta = rowMeta.getValueMeta(i);
      columns[i] =
          new ColumnInfo(valueMeta.getName(), ColumnInfo.COLUMN_TYPE_TEXT, valueMeta.isNumeric());
      columns[i].setToolTip(valueMeta.toStringMeta());
      columns[i].setValueMeta(valueMeta);
      columns[i].setImage(GuiResource.getInstance().getImage(valueMeta));
    }

    wFields =
        new TableView(
            variables, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, columns, 0, null, props);
    wFields.setShowingBlueNullValues(true);

    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment(0, 0);
    fdFields.top = new FormAttachment(wlFields, margin);
    fdFields.right = new FormAttachment(100, 0);
    fdFields.bottom = new FormAttachment(bottomButton, -2 * margin);
    wFields.setLayoutData(fdFields);

    if (dynamic) {
      shell.layout(true, true);
    }

    return false;
  }

  public void dispose() {
    PropsUi.getInstance().setScreen(new WindowProperty(shell));
    bounds = shell.getBounds();
    hscroll = wFields.getHorizontalBar().getSelection();
    vscroll = wFields.getVerticalBar().getSelection();
    shell.dispose();
  }

  /** Copy information from the meta-data input to the dialog fields. */
  private void getData() {
    synchronized (buffer) {
      shell
          .getDisplay()
          .asyncExec(
              () -> {
                lineNr = 0;
                for (int i = 0; i < buffer.size(); i++) {
                  TableItem item;
                  if (i == 0) {
                    item = wFields.table.getItem(i);
                  } else {
                    item = new TableItem(wFields.table, SWT.NONE);
                  }

                  Object[] row = buffer.get(i);

                  getDataForRow(item, row);
                }
                if (!wFields.isDisposed()) {
                  wFields.optWidth(true, 200);
                }
              });
    }
  }

  protected int getDataForRow(TableItem item, Object[] row) {
    int nrErrors = 0;

    if (row == null) { // no row to process
      return nrErrors;
    }

    // Display the correct line item...
    //
    String strNr;
    lineNr++;
    try {
      strNr = wFields.getNumberColumn().getValueMeta().getString(Long.valueOf(lineNr));
    } catch (Exception e) {
      strNr = Integer.toString(lineNr);
    }
    item.setText(0, strNr);

    for (int c = 0; c < rowMeta.size(); c++) {
      IValueMeta v = rowMeta.getValueMeta(c);
      String show;
      try {
        if (v.isBinary()) {
          byte[] bytes = v.getBinary(row[c]);
          if (bytes == null) {
            show = null;
          } else {
            if (PREVIEW_AVOID_BINARY_IN_HEX) {
              show = v.getString(bytes);
            } else {
              show = Hex.encodeHexString(bytes);
            }
            if (show.length() > MAX_BINARY_STRING_PREVIEW_SIZE) {
              // We want to limit the size of the strings during preview to keep all SWT widgets
              // happy.
              //
              show = show.substring(0, MAX_BINARY_STRING_PREVIEW_SIZE);
            }
          }
        } else {
          show = v.getString(row[c]);
        }

      } catch (HopValueException | ArrayIndexOutOfBoundsException e) {
        nrErrors++;
        if (nrErrors < 25) {
          log.logError(Const.getStackTracker(e));
        }
        show = null;
      }

      if (show != null) {
        item.setText(c + 1, show);
        item.setForeground(c + 1, GuiResource.getInstance().getColorBlack());
      } else {
        // Set null value
        item.setText(c + 1, "<null>");
        item.setForeground(c + 1, GuiResource.getInstance().getColorBlue());
      }
    }

    return nrErrors;
  }

  private void close() {
    transformName = null;
    dispose();
  }

  /** Show the logging of the preview (in case errors occurred */
  private void log() {
    if (loggingText != null) {
      EnterTextDialog etd =
          new EnterTextDialog(
              shell,
              BaseMessages.getString(PKG, "PreviewRowsDialog.ShowLogging.Title"),
              BaseMessages.getString(PKG, "PreviewRowsDialog.ShowLogging.Message"),
              loggingText);
      etd.open();
    }
  }

  public boolean isDisposed() {
    return shell.isDisposed();
  }

  public Rectangle getBounds() {
    return bounds;
  }

  public void setBounds(Rectangle b) {
    bounds = b;
  }

  public int getHScroll() {
    return hscroll;
  }

  public void setHScroll(int s) {
    hscroll = s;
  }

  public int getVScroll() {
    return vscroll;
  }

  public void setVScroll(int s) {
    vscroll = s;
  }

  public int getHMax() {
    return hmax;
  }

  public void setHMax(int m) {
    hmax = m;
  }

  public int getVMax() {
    return vmax;
  }

  public void setVMax(int m) {
    vmax = m;
  }

  /**
   * @return true if the user is asking to grab the next rows with preview
   */
  public boolean isAskingForMoreRows() {
    return askingForMoreRows;
  }

  /**
   * @return true if the dialog is proposing to ask for more rows
   */
  public boolean isProposingToGetMoreRows() {
    return proposingToGetMoreRows;
  }

  /**
   * @param proposingToGetMoreRows Set to true if you want to display a button asking for more
   *     preview rows.
   */
  public void setProposingToGetMoreRows(boolean proposingToGetMoreRows) {
    this.proposingToGetMoreRows = proposingToGetMoreRows;
  }

  /**
   * @return the askingToStop
   */
  public boolean isAskingToStop() {
    return askingToStop;
  }

  /**
   * @return the proposingToStop
   */
  public boolean isProposingToStop() {
    return proposingToStop;
  }

  /**
   * @param proposingToStop the proposingToStop to set
   */
  public void setProposingToStop(boolean proposingToStop) {
    this.proposingToStop = proposingToStop;
  }

  public void setDynamic(boolean dynamic) {
    this.dynamic = dynamic;
  }

  public synchronized void addDataRow(final IRowMeta rowMeta, final Object[] rowData) {

    if (shell == null || shell.isDisposed()) {
      return;
    }

    HopGui.getInstance()
        .getDisplay()
        .syncExec(
            () -> {
              if (wFields.isDisposed()) {
                return;
              }

              if (waitingForRows) {
                PreviewRowsDialog.this.rowMeta = rowMeta;
                addFields();
              }

              TableItem item = new TableItem(wFields.table, SWT.NONE);
              getDataForRow(item, rowData);
              if (waitingForRows) {
                waitingForRows = false;
                wFields.removeEmptyRows();
                PreviewRowsDialog.this.rowMeta = rowMeta;
                if (wFields.table.getItemCount() < 10) {
                  wFields.optWidth(true);
                }
              }

              if (wFields.table.getItemCount() > PropsUi.getInstance().getDefaultPreviewSize()) {
                wFields.table.remove(0);
              }

              wFields.table.setTopIndex(wFields.table.getItemCount() - 1);
            });
  }

  public void addDialogClosedListener(IDialogClosedListener listener) {
    dialogClosedListeners.add(listener);
  }
}
