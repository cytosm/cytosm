package org.cytosm.cypher2sql.lowering;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.RelationalGTopInterface;
import org.cytosm.cypher2sql.PassAvailables;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Put here any tests that stress a particular feature or
 * that should work unconditionally.
 *
 */
public class StressTests {

    private GTopInterfaceImpl getGTopInterface() throws IOException {
        String path = "src" + File.separatorChar + "test" + File.separatorChar + "resources";
        String jsonInString = FileUtils.readFileToString(new File(path + "/northwind.gtop"));
        return new RelationalGTopInterface(jsonInString);
    }


    @Test
    public void testMatchWithWithWithWithWith() throws Exception {
        GTopInterfaceImpl gTopInterface = getGTopInterface();
        String query =
                "MATCH (person:Employees) " +
                        "WITH person " +
                        "WITH person " +
                        "WITH person " +
                        "WITH person " +
                        "WITH person " +
                        "RETURN person.firstName";
        PassAvailables.cypher2sqlOnExpandedPaths(gTopInterface, query);
    }

    @Test
    public void testMatchWithMatchWithWithWith() throws Exception {
        GTopInterfaceImpl gTopInterface = getGTopInterface();
        String query =
                "MATCH (person:Employees) " +
                        "WITH person " +
                        "MATCH (person) " +
                        "WITH person " +
                        "WITH person " +
                        "WITH person " +
                        "RETURN person.firstName";
        PassAvailables.cypher2sqlOnExpandedPaths(gTopInterface, query);
    }
}
