/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.trans.steps.coalesce;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.coalesce.Coalesce;
import org.pentaho.di.trans.steps.coalesce.CoalesceMeta;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.dialog.AbstractStepDialog;

public class CoalesceDialog extends AbstractStepDialog<CoalesceMeta> {

  private static Class<?> PKG = CoalesceMeta.class; // for i18n purposes

  private Button wEmptyStrings;

  private TableView wFields;

  private ColumnInfo[] columnInfos;

  private Map<String, Integer> inputFields;

  public static void main(String[] args) {
    try {
      CoalesceDialog dialog = new CoalesceDialog(null, new CoalesceMeta(), null, "noname");
      dialog.open();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Constructor that saves incoming meta object to a local variable, so it can conveniently read and write settings from/to it.
   *
   * @param parent
   *            the SWT shell to open the dialog in
   * @param in
   *            the meta object holding the step's settings
   * @param transMeta
   *            transformation description
   * @param sName
   *            the step name
   */
  public CoalesceDialog(Shell parent, Object in, TransMeta transMeta, String sName) {
    super(parent, in, transMeta, sName);

    setText(BaseMessages.getString(PKG, "CoalesceDialog.Shell.Title"));
    inputFields = new HashMap<String, Integer>();
  }

  @Override
  protected void loadMeta(final CoalesceMeta meta) {

    wEmptyStrings.setSelection(meta.isTreatEmptyStringsAsNulls());

    Coalesce[] coalesces = meta.getCoalesces();
    for (int i = 0; i < coalesces.length; i++) {

      Coalesce coalesce = coalesces[i];
      TableItem item = wFields.getTable().getItem(i);
      item.setText(1, Const.NVL(coalesce.getName(), ""));

      for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
        item.setText(2 + j, Const.NVL(coalesce.getInputField(j), ""));
      }
      item.setText(2 + Coalesce.MAX_INPUT_FIELD, ValueMetaBase.getTypeDesc(coalesce.getType()));
      item.setText(3 + Coalesce.MAX_INPUT_FIELD, CoalesceMeta.getStringFromBoolean(coalesce.isRemoveInputFields()));
    }

    wFields.setRowNums();
    wFields.optWidth(true);
  }

  @Override
  public Point getMinimumSize() {
    return new Point(600, 300);
  }

  @Override
  protected void saveMeta(final CoalesceMeta meta) {

    // save step name
    stepname = wStepname.getText();

    meta.setEmptyStringsAsNulls(wEmptyStrings.getSelection());

    Coalesce[] coalesces = new Coalesce[wFields.nrNonEmpty()];

    // CHECKSTYLE:Indentation:OFF
    for (int i = 0; i < coalesces.length; i++) {
      TableItem item = wFields.getNonEmpty(i);

      Coalesce coalesce = new Coalesce();
      coalesce.setName(item.getText(1));

      int noNonEmptyFields = 0;
      for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
        coalesce.setInputField(j, item.getText(2 + j));
        if (!Utils.isEmpty(coalesce.getInputField(j))) {
          noNonEmptyFields++;
        }
      }

      if (noNonEmptyFields < 2) {
        MessageDialogWithToggle md = new MessageDialogWithToggle(shell,
            BaseMessages.getString(PKG, "CoalesceDialog.Validations.DialogTitle"), null,
            BaseMessages.getString(PKG, "CoalesceDialog.Validations.DialogMessage", Const.CR, Const.CR),
            MessageDialog.WARNING, new String[] { BaseMessages.getString(PKG, "CoalesceDialog.Validations.Option.1") },
            0, BaseMessages.getString(PKG, "CoalesceDialog.Validations.Option.2"), false);
        Window.setDefaultImage(GUIResource.getInstance().getImageSpoon());
        md.open();
      }

      String typeValueText = item.getText(2 + Coalesce.MAX_INPUT_FIELD);
      coalesce
          .setType(Utils.isEmpty(typeValueText) ? ValueMetaInterface.TYPE_NONE : ValueMetaBase.getType(typeValueText));

      String isRemoveText = item.getText(3 + Coalesce.MAX_INPUT_FIELD);
      coalesce.setRemoveInputFields(CoalesceMeta.getBooleanFromString(isRemoveText));

      coalesces[i] = coalesce;
    }

    meta.setCoalesces(coalesces);
  }

  private void setComboBoxes() {
    // Something was changed in the row.
    final Map<String, Integer> fields = new TreeMap<String, Integer>(inputFields);

    String[] fieldNames = new String[inputFields.size()];
    fieldNames = fields.keySet().toArray(fieldNames);
    Const.sortStrings(fieldNames);

    for (int i = 0; i < Coalesce.MAX_INPUT_FIELD; i++) {
      columnInfos[1 + i].setComboValues(fieldNames);
    }
  }

  @Override
  protected Control createDialogArea(final Composite parent) {
  
    // Widget Spaces and Nulls
    wEmptyStrings = new Button(parent, SWT.CHECK);
    wEmptyStrings.setText(BaseMessages.getString(PKG, "CoalesceDialog.Shell.EmptyStringsAsNulls"));    
    wEmptyStrings.setLayoutData(new FormDataBuilder().left().top().result());  
    wEmptyStrings.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        baseStepMeta.setChanged();
      }
    });
    props.setLook(wEmptyStrings); 

    
    CoalesceMeta meta = this.getStepMeta();

    Label wlFields = new Label(parent, SWT.NONE);
    wlFields.setText(BaseMessages.getString(PKG, "CoalesceDialog.Fields.Label"));
    wlFields.setLayoutData(new FormDataBuilder().top(wEmptyStrings, 2*Const.MARGIN).fullWidth().result());
    props.setLook(wlFields);

    columnInfos = new ColumnInfo[3 + Coalesce.MAX_INPUT_FIELD];
    columnInfos[0] = new ColumnInfo(BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.OutField"),
        ColumnInfo.COLUMN_TYPE_TEXT, false);
    columnInfos[0].setUsingVariables(true);
    for (int i = 0; i < Coalesce.MAX_INPUT_FIELD; i++) {
      columnInfos[i + 1] = new ColumnInfo(
          BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.InputField",
              Character.valueOf((char) ('A' + i)).toString()),
          ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false);
    }
    columnInfos[1 + Coalesce.MAX_INPUT_FIELD] = new ColumnInfo(
        BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.ValueType"), ColumnInfo.COLUMN_TYPE_CCOMBO,
        ValueMetaBase.getTypes());
    columnInfos[2 + Coalesce.MAX_INPUT_FIELD] = new ColumnInfo(
        BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.RemoveInputColumns"), ColumnInfo.COLUMN_TYPE_CCOMBO,
        new String[] { BaseMessages.getString(PKG, "System.Combo.No"),
            BaseMessages.getString(PKG, "System.Combo.Yes") });

    columnInfos[2 + Coalesce.MAX_INPUT_FIELD]
        .setToolTip(BaseMessages.getString(PKG, "CoalesceDialog.ColumnInfo.RemoveInputColumns.Tooltip"));

    int noFieldRows = (meta.getCoalesces() != null ? meta.getCoalesces().length : 1);
    this.wFields = new TableView(transMeta, parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, columnInfos,
        noFieldRows, lsMod, props);
    this.wFields
        .setLayoutData(new FormDataBuilder().left().right(100, 0).top(wlFields, Const.MARGIN).bottom().result());

    this.wFields.getTable().addListener(SWT.Resize, new ColumnsResizer(3, 27, 10, 10, 10, 10, 10, 10, 10));
    
    // Search the fields in the background
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        StepMeta stepMeta = transMeta.findStep(stepname);
        if (stepMeta != null) {
          try {
            RowMetaInterface row = transMeta.getPrevStepFields(stepMeta);

            // Remember these fields...
            for (int i = 0; i < row.size(); i++) {
              inputFields.put(row.getValueMeta(i).getName(), i);
            }

            setComboBoxes();
          } catch (KettleException e) {
            logError(BaseMessages.getString(PKG, "CoalesceDialog.Log.UnableToFindInput"));
          }
        }
      }
    };
    new Thread(runnable).start();

    return parent;
  }

}