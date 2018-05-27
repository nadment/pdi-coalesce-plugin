package org.pentaho.di.trans.steps.coalesce;

import java.util.Arrays;

import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;

/**
 * Contains the properties of the inputs fields, target field name, target value type and options.
 *
 * @author Nicolas ADMENT
 */
public class Coalesce implements Cloneable {



  /** The target field name */
  @Injection(name = "NAME", group = "FIELDS")
  private String name;

  private String[] inputFields = new String[MAX_INPUT_FIELD];

  private int type = ValueMetaInterface.TYPE_NONE;

  @Injection(name = "REMOVE_INPUT_FIELDS", group = "FIELDS")
  private boolean removeInputFields;

  /** Maximum number of input fields */
  public static final int MAX_INPUT_FIELD = 5;

  public Coalesce() {
    super();   
  }
  
  @Override
  public Object clone() {
    Coalesce clone;
    try {
      clone = (Coalesce) super.clone();
      clone.inputFields = Arrays.copyOf(inputFields, MAX_INPUT_FIELD);
      
    } catch (CloneNotSupportedException e) {
      return null;
    }
    return clone;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getInputFields() {
    return this.inputFields;
  }

  public String getInputField(int index) {
    return this.inputFields[index];
  }

  public void setInputField(final int index, final String fieldName) {
    this.inputFields[index] = fieldName;
  }

  public void setInputFields(final String... fieldNames) {
        
    for (int index=0; index<MAX_INPUT_FIELD; index++) {
        if ( index<fieldNames.length) {
          this.inputFields[index] = fieldNames[index];          
        }
        else 
          this.inputFields[index] = null;      
    }
  }

  
  public int getType() {
    return this.type;
  }

  public void setType(final int type) {
    this.type = type;
  }

  private String getTypeDesc() {
    return ValueMetaFactory.getValueMetaName(this.type);    
  }

  @Injection(name = "TYPE", group = "FIELDS")
  public void setType(final String name) {
    this.type = ValueMetaFactory.getIdForValueMeta(name);
  }

  public boolean isRemoveInputFields() {
    return this.removeInputFields;
  }

  public void setRemoveInputFields(boolean remove) {
    this.removeInputFields = remove;
  }

  @Override
  public String toString() {
    return name + ":" + getTypeDesc() ;
  }
}
