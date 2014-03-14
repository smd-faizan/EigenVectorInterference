/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cytoscape.eigenvectorinterference.internal;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.eigenvectorinterference.internal.util.EigenvalueDecomposition;
import org.cytoscape.eigenvectorinterference.internal.util.Matrix;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;

/**
 *
 * @author Sony
 */
public class MenuAction extends AbstractCyAction {

    public CyApplicationManager cyApplicationManager;
    public CySwingApplication cyDesktopService;
    public CyActivator cyactivator;

    public MenuAction(CyApplicationManager cyApplicationManager, final String menuTitle, CyActivator cyactivator) {
        super(menuTitle, cyApplicationManager, null, null);
        setPreferredMenu("Apps");
        this.cyactivator = cyactivator;
        this.cyApplicationManager = cyApplicationManager;
        this.cyDesktopService = cyactivator.getcytoscapeDesktopService();
    }

//    public void actionPerformed(ActionEvent e) {
//        // Write your own function here.
//        System.out.println("Starting findDrgree in control panel");
//        FindDegreeCore myDegreecore = new FindDegreeCore(cyactivator);
//        
//    }
    @Override
    public void actionPerformed(ActionEvent arg0) {
        //Get the selected nodes
        List<CyNode> selectedNodes = CyTableUtil.getNodesInState(cyApplicationManager.getCurrentNetwork(), "selected", true);
        if (selectedNodes.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please Select atleast 1 node");
            return;
        }

        Thread timer = new Thread() {

            @Override
            public void run() {
                //Get the selected nodes
                List<CyNode> selectedNodes = CyTableUtil.getNodesInState(cyApplicationManager.getCurrentNetwork(), "selected", true);

                StringBuffer buffer = new StringBuffer();
                CyApplicationManager manager = cyApplicationManager;
                CyNetworkView networkView = manager.getCurrentNetworkView();
                networkView.updateView();
                CyNetwork network = networkView.getModel();

                List<CyNode> nodeList = network.getNodeList();
                int n = nodeList.size();
                double[][] M = new double[n][n];
                int i = 0;
                for (CyNode root : nodeList) {
                    List<CyNode> neighbors = network.getNeighborList(root,
                            CyEdge.Type.ANY);
                    for (CyNode neighbor : neighbors) {
                        M[i][nodeList.indexOf(neighbor)] = 1.0;
                        // rest all by default zeroes in the adjacency matrix
                    }
                    i++;
                }
                // Matirx xreated 
                printMatrix(M, "first");
                // execute for eigen values
                Matrix A = new Matrix(M);
                EigenvalueDecomposition e = A.eig();
                Matrix V = e.getV();
                // put values in hashmap
                Map<CyNode, Double> eigenValuesOfM = new HashMap<CyNode, Double>();
                int k = 0;
                for (CyNode root : nodeList) {
                    if (k < n) {
                        eigenValuesOfM.put(root, V.get(k, n - 1));
                        k++;
                    }
                }
                //make the new network
                int n2 = n - selectedNodes.size();
                double[][] M2 = new double[n2][n2];
                i = 0;
                int i2 = 0;
                for (CyNode root : nodeList) {
                    if (selectedNodes.contains(root)) {
                        // do not add
                        i2++;
                        continue;
                    }
                    int j = 0;
                    int j2 = 0;
                    for (CyNode root2 : nodeList) {
                        if (selectedNodes.contains(root2)) {
                            // do not add
                            j2++;
                            continue;
                        }
                        M2[i][j] = M[i + i2][j + j2];
                        // rest all by default zeroes in the adjacency matrix
                        j++;
                    }
                    i++;
                }
                printMatrix(M2, "second");
                // execute for eigen values
                Matrix A2 = new Matrix(M2);
                EigenvalueDecomposition e2 = A2.eig();
                Matrix V2 = e2.getV();
                // put values in hashmap
                Map<CyNode, Double> eigenValuesOfM2 = new HashMap<CyNode, Double>();
                k = 0;
                for (CyNode root : nodeList) {
                    if (k < n2) {
                        if (!(selectedNodes.contains(root))) {
                            eigenValuesOfM2.put(root, V2.get(k, n2 - 1));
                            k++;
                        }
                    }
                }
                Map<CyNode, Double> interferenceValues = calculateInterference(eigenValuesOfM, eigenValuesOfM2);
                // all eigen values and eigen vectors calculated
                // now print
                buffer.append("Eigenvector centralities of whole network (with all nodes): ");
                buffer.append("\n");
                k = 0;
                for (CyNode root : nodeList) {
                    if (k < n) {
                        buffer.append(network.getRow(root).get("name",
                                String.class));
                        buffer.append(": ");
                        buffer.append(V.get(k, n - 1));
                        buffer.append("\n");
                        k++;
                    }
                }
                buffer.append("Eigenvector centralities of network without the selected nodes): ");
                buffer.append("\n");
                k = 0;
                for (CyNode root : nodeList) {
                    if (k < n2) {
                        if (!(selectedNodes.contains(root))) {
                        buffer.append(network.getRow(root).get("name",
                                String.class));
                        buffer.append(": ");
                        buffer.append(V2.get(k, n2 - 1));
                        buffer.append("\n");
                        k++;
                        }
                    }
                }
                buffer.append("\n");
                buffer.append("Eigenvector interference in % : ");
                buffer.append("\n");
                for (Map.Entry entry : interferenceValues.entrySet()) {
                    buffer.append(network.getRow((CyNode) entry.getKey()).get("name",
                            String.class));
                    buffer.append(": ");
                    buffer.append((Double) entry.getValue());
                    buffer.append("\n");
                }
                // to verify in console
                // this is a sample code, actual code will have threads and will
                // be efficient

                /*
                 * System.out.print("A ="); A.print(1, 1); System.out.print("D
                 * ="); D.print(9, 6); System.out.print("V ="); V.print(9, 6);
                 */
                System.out.println(buffer);
                JOptionPane.showMessageDialog(null, buffer);
            }
        };
        timer.start();
    }

    public static Map<CyNode, Double> calculateInterference(Map<CyNode, Double> valuesOfM1, Map<CyNode, Double> valuesOfM2) {
        Double totalSum1 = 0.0;
        Double totalSum2 = 0.0;
        for (Map.Entry entry : valuesOfM1.entrySet()) {
            totalSum1 = totalSum1 + (Double) entry.getValue();
        }
        for (Map.Entry entry : valuesOfM2.entrySet()) {
            totalSum2 = totalSum2 + (Double) entry.getValue();
        }
        Map<CyNode, Double> interferenceValues = new HashMap<CyNode, Double>();

        for (Map.Entry entry : valuesOfM2.entrySet()) {
            CyNode currentNode = (CyNode) entry.getKey();
            Double interferenceValue = ((Double) valuesOfM1.get(currentNode)) / totalSum1;
            interferenceValue = interferenceValue - ((Double) entry.getValue()) / totalSum2;
            interferenceValues.put(currentNode, (interferenceValue) * 100);
        }
        return interferenceValues;
    }

    public static void printMatrix(double[][] matrix, String name) {
        System.out.println("The matrix " + name + " is :");
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }
}
