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

package org.apache.hop.pipeline.transforms.tableoutput;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.database.IDatabase;
import org.apache.hop.core.plugins.PluginRegistry;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.serializer.memory.MemoryMetadataProvider;
import org.apache.hop.pipeline.transforms.loadsave.LoadSaveTester;
import org.apache.hop.pipeline.transforms.loadsave.validator.IFieldLoadSaveValidator;
import org.apache.hop.pipeline.transforms.loadsave.validator.IFieldLoadSaveValidatorFactory;
import org.apache.hop.pipeline.transforms.loadsave.validator.ListLoadSaveValidator;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TableOutputMetaTest {

  private IVariables variables;
  private List<DatabaseMeta> databases;
  private IHopMetadataProvider metadataProvider;
  Class<TableOutputMeta> testMetaClass = TableOutputMeta.class;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    variables = mock(IVariables.class);
    databases = mock(List.class);
    metadataProvider = new MemoryMetadataProvider();
  }

  @Test
  public void testIsReturningGeneratedKeys() throws Exception {
    TableOutputMeta tableOutputMeta = new TableOutputMeta(),
        tableOutputMetaSpy = spy(tableOutputMeta);

    DatabaseMeta databaseMeta = mock(DatabaseMeta.class);
    doReturn(true).when(databaseMeta).supportsAutoGeneratedKeys();
    doReturn(databaseMeta).when(tableOutputMetaSpy).getDatabaseMeta();

    tableOutputMetaSpy.setReturningGeneratedKeys(true);
    assertTrue(tableOutputMetaSpy.isReturningGeneratedKeys());

    tableOutputMetaSpy.setReturningGeneratedKeys(false);
    assertFalse(tableOutputMetaSpy.isReturningGeneratedKeys());
  }

  @Test
  public void testProvidesModeler() throws Exception {
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    tableOutputMeta.getFields().add(new TableOutputField("f1", "s4"));
    tableOutputMeta.getFields().add(new TableOutputField("f2", "s5"));
    tableOutputMeta.getFields().add(new TableOutputField("f3", "s6"));

    TableOutputData tableOutputData = new TableOutputData();
    tableOutputData.insertRowMeta = mock(RowMeta.class);
    assertEquals(
        tableOutputData.insertRowMeta, tableOutputMeta.getRowMeta(variables, tableOutputData));

    tableOutputMeta.setSpecifyFields(false);
    assertEquals(0, tableOutputMeta.getDatabaseFields().size());
    assertEquals(0, tableOutputMeta.getStreamFields().size());

    tableOutputMeta.setSpecifyFields(true);
    assertEquals(3, tableOutputMeta.getDatabaseFields().size());
    assertEquals("f1", tableOutputMeta.getDatabaseFields().get(0));
    assertEquals("f2", tableOutputMeta.getDatabaseFields().get(1));
    assertEquals("f3", tableOutputMeta.getDatabaseFields().get(2));
    assertEquals(3, tableOutputMeta.getStreamFields().size());
    assertEquals("s4", tableOutputMeta.getStreamFields().get(0));
    assertEquals("s5", tableOutputMeta.getStreamFields().get(1));
    assertEquals("s6", tableOutputMeta.getStreamFields().get(2));
  }

  @Test
  public void testSetupDefault() throws Exception {
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    tableOutputMeta.setDefault();
    assertEquals("", tableOutputMeta.getTableName());
    assertEquals("1000", tableOutputMeta.getCommitSize());
    assertFalse(tableOutputMeta.isPartitioningEnabled());
    assertTrue(tableOutputMeta.isPartitioningMonthly());
    assertEquals("", tableOutputMeta.getPartitioningField());
    assertTrue(tableOutputMeta.isTableNameInTable());
    assertEquals("", tableOutputMeta.getTableNameField());
    assertFalse(tableOutputMeta.isSpecifyFields());
  }

  @Test
  public void testClone() throws Exception {
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    tableOutputMeta.setDefault();
    tableOutputMeta.getFields().add(new TableOutputField("d1", "1"));
    tableOutputMeta.getFields().add(new TableOutputField("d2", "2"));
    tableOutputMeta.getFields().add(new TableOutputField("d3", "3"));
    TableOutputMeta clone = (TableOutputMeta) tableOutputMeta.clone();
    assertNotSame(clone, tableOutputMeta);
    assertEquals(clone.getXml(), tableOutputMeta.getXml());
  }

  @Test
  public void testSupportsErrorHandling() throws Exception {
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    DatabaseMeta dbMeta = mock(DatabaseMeta.class);
    IDatabase iDatabase = mock(IDatabase.class);
    when(dbMeta.getIDatabase()).thenReturn(iDatabase);
    when(iDatabase.isSupportsErrorHandling()).thenReturn(true, false);
    assertTrue(tableOutputMeta.supportsErrorHandling());
  }

  @Before
  public void setUpLoadSave() throws Exception {
    HopEnvironment.init();
    PluginRegistry.init();
  }

  @Test
  public void testSerialization() throws Exception {

    LoadSaveTester<TableOutputMeta> tester = new LoadSaveTester<>(testMetaClass);

    IFieldLoadSaveValidatorFactory factory = tester.getFieldLoadSaveValidatorFactory();

    factory.registerValidator(
        TableOutputMeta.class.getDeclaredField("fields").getGenericType().toString(),
        new ListLoadSaveValidator<>(new TableOutputFieldInputFieldLoadSaveValidator()));

    tester.testSerialization();
  }

  public class TableOutputFieldInputFieldLoadSaveValidator
      implements IFieldLoadSaveValidator<TableOutputField> {
    final Random rand = new Random();

    @Override
    public TableOutputField getTestObject() {
      String[] types = ValueMetaFactory.getAllValueMetaNames();

      TableOutputField field =
          new TableOutputField(UUID.randomUUID().toString(), UUID.randomUUID().toString());

      return field;
    }

    @Override
    public boolean validateTestObject(TableOutputField testObject, Object actual) {
      if (!(actual instanceof TableOutputField)) {
        return false;
      }
      TableOutputField another = (TableOutputField) actual;
      return new EqualsBuilder()
          .append(testObject.getFieldStream(), another.getFieldStream())
          .append(testObject.getFieldDatabase(), another.getFieldDatabase())
          .isEquals();
    }
  }
}
