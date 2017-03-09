package org.cytosm.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.cytosm.common.gtop.implementation.graphmetadata.BackendSystem;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cytosm.common.gtop.GTop;
import org.cytosm.common.gtop.GTopInterfaceImpl;
import org.cytosm.common.gtop.RelationalGTopInterface;
import org.cytosm.common.gtop.abstraction.AbstractionEdge;
import org.cytosm.common.gtop.abstraction.AbstractionLevelGtop;
import org.cytosm.common.gtop.abstraction.AbstractionNode;
import org.cytosm.common.gtop.implementation.graphmetadata.GraphMetadata;
import org.cytosm.common.gtop.implementation.graphmetadata.StorageLayout;
import org.cytosm.common.gtop.implementation.relational.Attribute;
import org.cytosm.common.gtop.implementation.relational.EdgeAttribute;
import org.cytosm.common.gtop.implementation.relational.ImplementationEdge;
import org.cytosm.common.gtop.implementation.relational.ImplementationLevelGtop;
import org.cytosm.common.gtop.implementation.relational.ImplementationNode;
import org.cytosm.common.gtop.implementation.relational.NodeIdImplementation;
import org.cytosm.common.gtop.implementation.relational.RestrictionClauses;
import org.cytosm.common.gtop.implementation.relational.TraversalHop;
import org.cytosm.common.gtop.implementation.relational.TraversalPath;
import org.cytosm.common.gtop.io.SerializationInterface;

public class GtopTest {

    @Test
    public void emptyGTopSerializationProcess() throws IOException {

        // produce abstraction section;
        List<AbstractionNode> aNodes = new ArrayList<>();
        List<AbstractionEdge> aEdges = new ArrayList<>();
        AbstractionLevelGtop abslevel = new AbstractionLevelGtop(aNodes, aEdges);

        // produce implementation section
        List<ImplementationNode> iNodes = new ArrayList<>();
        List<ImplementationEdge> iEdges = new ArrayList<>();
        GraphMetadata gdata = new GraphMetadata(StorageLayout.IGNORETIME, BackendSystem.RELATIONAL);
        ImplementationLevelGtop implevel = new ImplementationLevelGtop(gdata, iNodes, iEdges);

        GTop gTop = new GTop(abslevel, implevel);

        ObjectMapper mapper = new ObjectMapper();

        // Object to JSON in file
        mapper.writeValue(new File("emptytemplate.gtop"), gTop);

        // Object to JSON in String
        String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(gTop);

        System.out.println(jsonInString);

        GTop gTop2 = mapper.readValue(jsonInString, GTop.class);

        // assert they are the same
        Assert.assertEquals(gTop.getVersion(), gTop2.getVersion());
        Assert.assertEquals(gTop.getAbstractionLevel(), gTop2.getAbstractionLevel());
        Assert.assertEquals(gTop.getImplementationLevel(), gTop2.getImplementationLevel());
    }

    @Test
    public void filledGtop() throws IOException {
        // produce abstraction section;
        List<AbstractionNode> aNodes = new ArrayList<>();
        List<AbstractionEdge> aEdges = new ArrayList<>();
        AbstractionNode aNode1 = new AbstractionNode(Arrays.asList("node1"), Arrays.asList("nodeAttribute1"));
        AbstractionNode aNode2 = new AbstractionNode(Arrays.asList("node2"), Arrays.asList("nodeAttribute2"));
        AbstractionEdge aEdge =
                new AbstractionEdge(Arrays.asList("edge1"), Arrays.asList("edgeAttribute1"), Arrays.asList("node1"),
                        Arrays.asList("node2"), true);
        aNodes.add(aNode1);
        aNodes.add(aNode2);
        aEdges.add(aEdge);

        AbstractionLevelGtop abslevel = new AbstractionLevelGtop(aNodes, aEdges);

        // produce implementation section
        List<ImplementationNode> iNodes = new ArrayList<>();
        List<ImplementationEdge> iEdges = new ArrayList<>();

        List<Attribute> node1Attributes = Arrays.asList(new Attribute("attributeColumn", "kibe", "VARCHAR(10)"));
        List<RestrictionClauses> node1Restrictions = Arrays.asList(new RestrictionClauses());
        ImplementationNode iNode1 =
                new ImplementationNode(Arrays.asList("node1"), "node1Table", Arrays.asList(new NodeIdImplementation(
                        "node1IdColumnName", "VARCHAR(100)", 1)), node1Attributes, node1Restrictions);

        List<Attribute> node2Attributes = Arrays.asList(new Attribute("attributeColumn", "pao", "VARCHAR(10)"));
        ImplementationNode iNode2 =
                new ImplementationNode(Arrays.asList("node2"), "node2Table", Arrays.asList(new NodeIdImplementation(
                        "node2IdColumnName", "VARCHAR(100)", 1)), node2Attributes, node1Restrictions);

        iNodes.add(iNode1);
        iNodes.add(iNode2);

        List<EdgeAttribute> eAttributes = Arrays.asList(new EdgeAttribute());
        List<TraversalPath> paths =
                Arrays.asList(new TraversalPath(Arrays.asList(new TraversalHop("edgeTable", "edgeSourceTableColumn",
                        "edgejoinTableSourceColumn", "edgeJoinTableName", "edgeJoinTableDestinationColumn",
                        "edgeDestinationTableColumn", "edgeDestionationTableName", eAttributes, 1, null))));
        ImplementationEdge iEdge = new ImplementationEdge(Arrays.asList("edge1"), paths);

        iEdges.add(iEdge);

        GraphMetadata gdata = new GraphMetadata(StorageLayout.IGNORETIME, BackendSystem.RELATIONAL);
        ImplementationLevelGtop implevel = new ImplementationLevelGtop(gdata, iNodes, iEdges);

        /*
         * List<Attribute> attributes = new ArrayList<>(); Attribute field = new Attribute();
         * field.setColumnName("tableName.colName"); field.setDataType("dataType");
         * field.setFieldName("fieldName"); attributes.add(field);
         *
         * List<String> labels = new ArrayList<>(); labels.add("label1"); labels.add("label2");
         *
         * List<Restriction> restrictions = new ArrayList<>(); Restriction restriction = new
         * Restriction("tableName.columnName", "pattern"); restrictions.add(restriction);
         *
         * node.setAttributes(attributes); node.setTableName("tableName");
         * node.setIdColumn("idColumn"); node.setTypes(labels);
         * node.setRestrictions(restrictions); nodes.add(node);
         *
         * Edge edge = new Edge(); edge.setEdgeMappedTable("edgeTable");
         * edge.setDestinationTableColumn("destination"); edge.setSourceTableName("tableName");
         * edge.setSourceTableColumn("columnName"); List<String> types = new ArrayList<>();
         * types.add("myEdgeTable");
         *//*
           *
           * edge.setTypes(types);
           *
           * edges.add(edge);
           */

        GTop gTop = new GTop(abslevel, implevel);

        GTopInterfaceImpl gInterface = new RelationalGTopInterface(gTop);

        // Object to JSON in String - pretty-printed
        String jsonInString = SerializationInterface.toPrettyString(gTop);

        // Object to JSON in file
        FileUtils.writeStringToFile(new File("filledtemplate.gtop"), jsonInString);

        // Dump to console ...
        System.out.println(jsonInString);

        // Read value from screen
        GTopInterfaceImpl gInterfaceFromString = new RelationalGTopInterface(jsonInString);

        // Check the integrity of reading value back ...

        // whole levels:
        //Assert.assertEquals(gTop.getAbstractionLevel(), gtopFromString.getAbstractionLevel());

        //Assert.assertEquals(gTop.getImplementationLevel(), gtopFromString.getImplementationLevel());

        // abstract edges:

        Assert.assertEquals(gInterface.getAbstractionEdgesByTypes("edge1").get(0).getDestinationType().get(0),
                gInterfaceFromString.getAbstractionEdgesByTypes("edge1").get(0).getDestinationType().get(0));

        Assert.assertEquals(gInterface.getAbstractionEdgesByTypes("edge1").get(0).getSourceType().get(0),
                gInterfaceFromString.getAbstractionEdgesByTypes("edge1").get(0).getSourceType().get(0));

        Assert.assertEquals(gInterface.getAbstractionEdgesByTypes("edge1").get(0).isDirected(), gInterfaceFromString
                .getAbstractionEdgesByTypes("edge1").get(0).isDirected());

        Assert.assertEquals(gInterface.getAbstractionEdgesByTypes("edge1").get(0).getTypes().get(0), gInterfaceFromString
                .getAbstractionEdgesByTypes("edge1").get(0).getTypes().get(0));

        AbstractionNode nod1 = gInterface.getAbstractionNodesByTypes("node1").get(0);
        AbstractionNode nod2 = gInterface.getAbstractionNodesByTypes("node2").get(0);

        List<AbstractionNode> allNodes = gInterface.getAbstractionNodes();
        List<AbstractionNode> sourceNodes =
                gInterface.getSourceNodesForEdge(gInterface.getAbstractionEdgesByTypes("edge1").get(0));
        List<AbstractionNode> destinationNodes =
                gInterface.getDestinationNodesForEdge(gInterface.getAbstractionEdgesByTypes("edge1").get(0));
        List<AbstractionNode> allNodesFromEdge =
                gInterface.getNodesForEdge(gInterface.getAbstractionEdgesByTypes("edge1").get(0));

        Assert.assertTrue(allNodes.contains(nod1) && allNodes.contains(nod2));
        Assert.assertTrue(sourceNodes.contains(nod1) && sourceNodes.size() == 1);
        Assert.assertTrue(destinationNodes.contains(nod2) && destinationNodes.size() == 1);
        Assert.assertTrue(allNodesFromEdge.size() == 2 && allNodesFromEdge.contains(nod1)
                && allNodesFromEdge.contains(nod2));

        //Assert.assertEquals(impedg1, impedg2);

        // abstract Nodes:
        Assert.assertEquals(gInterface.getAbstractionNodesByTypes("node1"),
                gInterfaceFromString.getAbstractionNodesByTypes("node1"));

        Assert.assertNotEquals(gInterface.getAbstractionNodesByTypes("node1"),
                gInterfaceFromString.getAbstractionNodesByTypes("node2"));

        //Assert.assertEquals(gTop.getAbstractionNodes(), gtopFromString.getAbstractionNodes());

        //Assert.assertEquals(impnd1.get(0), impnd2.get(0));

        // Graph metadata
        Assert.assertEquals(gTop.getImplementationLevel().getGraphMetadata(), gInterfaceFromString
                .getImplementationLevel().getGraphMetadata());

    }

    @After
    public void cleanup() {
        // remove files:

        try {
            Files.deleteIfExists((new File("filledtemplate.gtop")).toPath());
            Files.deleteIfExists((new File("emptytemplate.gtop")).toPath());
        } catch (IOException x) {
            // File permission problems are caught here.
            System.err.println(x);
        }
    }
}
