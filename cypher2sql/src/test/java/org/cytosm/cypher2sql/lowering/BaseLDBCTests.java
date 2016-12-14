package org.cytosm.cypher2sql.lowering;

import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.RelationalGTopInterface;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 */
public class BaseLDBCTests {

    protected GTopInterfaceImpl getGTopInterface() throws IOException {
        String path = "src" + File.separatorChar + "test" + File.separatorChar + "resources";
        String jsonInString = FileUtils.readFileToString(new File(path + "/ldbc.gtop"));
        return new RelationalGTopInterface(jsonInString);
    }

}
