/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.gephi.graph.store;

import junit.framework.Assert;
import org.gephi.attribute.api.Column;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author hbruch
 */
public class GraphModelImplTest {
    
    
    @Test
    public void testDefaultColumnsHaveTable() {
        final GraphModelImpl gm = new GraphModelImpl();
        final Column[] nodeColumns = gm.nodeTable.getColumns();
        for(Column column: nodeColumns) {
            Assert.assertEquals("NodeTable not set for column "+column.getId(), gm.nodeTable, column.getTable());
        }
        final Column[] edgeColumns = gm.edgeTable.getColumns();
        for(Column column: edgeColumns) {
            Assert.assertEquals("EdgeTable not set for column "+column.getId(), gm.edgeTable, column.getTable());
        }
    }

   
}
