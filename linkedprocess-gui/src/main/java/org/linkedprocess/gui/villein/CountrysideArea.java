package org.linkedprocess.gui.villein;

import org.jivesoftware.smack.packet.Presence;
import org.linkedprocess.LinkedProcess;
import org.linkedprocess.gui.*;
import org.linkedprocess.gui.villein.vmcontrol.VmControlFrame;
import org.linkedprocess.xmpp.villein.Handler;
import org.linkedprocess.xmpp.villein.PresenceHandler;
import org.linkedprocess.xmpp.villein.XmppVillein;
import org.linkedprocess.xmpp.villein.proxies.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

/**
 * User: marko
 * Date: Jul 7, 2009
 * Time: 11:13:22 PM
 */
public class CountrysideArea extends JPanel implements ActionListener, MouseListener, PresenceHandler {

    protected VilleinGui villeinGui;
    protected JTree tree;
    protected JPopupMenu popupMenu;
    protected Object popupTreeObject;
    protected DefaultMutableTreeNode treeRoot;
    protected Set<String> supportedVmSpeciesActionCommands = new HashSet<String>();

    protected final static String FARM_CONFIGURATION = "farm configuration";
    protected final static String REGISTRY_COUNTRYSIDES = "countrysides";
    protected final static String TERMINATE_VM = "terminate vm";
    protected final static String SPAWN_VM = "spawn vm";
    protected final static String ADD_COUNTRYSIDE = "add countryside";
    protected final static String SHUTDOWN = "shutdown";
    protected final static String VM_CONTROL = "vm control";
    protected final static String PROBE = "probe";

    public CountrysideArea(VilleinGui villeinGui) {
        super(new BorderLayout());
        this.villeinGui = villeinGui;
        CountrysideProxy countrysideStruct = new CountrysideProxy(LinkedProcess.generateBareJid(this.villeinGui.getXmppVillein().getFullJid()), this.villeinGui.getXmppVillein().getDispatcher());
        this.treeRoot = new DefaultMutableTreeNode(countrysideStruct);
        this.tree = new JTree(this.treeRoot);
        this.tree.setCellRenderer(new TreeRenderer());
        this.tree.setModel(new DefaultTreeModel(treeRoot));
        this.tree.addMouseListener(this);
        this.tree.setRootVisible(false);
        this.popupMenu = new JPopupMenu();
        this.popupMenu.setBorder(BorderFactory.createLineBorder(ImageHolder.GRAY_COLOR, 2));

        JScrollPane vmTreeScroll = new JScrollPane(this.tree);
        JButton shutdownButton = new JButton(SHUTDOWN);
        shutdownButton.addActionListener(this);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(shutdownButton);
        shutdownButton.addActionListener(this);
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.add(vmTreeScroll, BorderLayout.CENTER);
        treePanel.add(buttonPanel, BorderLayout.SOUTH);

        RosterPanel rosterPanel = new RosterPanel(this.villeinGui.getXmppVillein().getRoster());
        PacketSnifferPanel packetSnifferPanel = new PacketSnifferPanel(this.villeinGui.getXmppVillein().getFullJid());
        this.villeinGui.getXmppVillein().getConnection().addPacketListener(packetSnifferPanel, null);
        this.villeinGui.getXmppVillein().getConnection().addPacketWriterInterceptor(packetSnifferPanel, null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("countrysides", treePanel);
        tabbedPane.addTab("roster", rosterPanel);
        tabbedPane.addTab("packets", packetSnifferPanel);

        this.add(tabbedPane, BorderLayout.CENTER);

        this.villeinGui.getXmppVillein().createCountrysideProxiesFromRoster();
        this.createTree();
    }

    public void actionPerformed(ActionEvent event) {

        this.popupMenu.setVisible(false);

        if (event.getActionCommand().equals(TERMINATE_VM)) {
            if (this.popupTreeObject instanceof VmProxy) {
                VmProxy vmProxy = (VmProxy) this.popupTreeObject;
                vmProxy.terminateVm(null, new GenericErrorHandler());
                this.villeinGui.removeVmFrame(vmProxy);
            }
        } else if (event.getActionCommand().equals(VM_CONTROL)) {
            if (this.popupTreeObject instanceof VmProxy) {
                VmProxy vmProxy = (VmProxy) this.popupTreeObject;
                VmControlFrame vmControlFrame = this.villeinGui.getVmFrame(vmProxy.getFullJid());
                if (vmControlFrame == null) {
                    this.villeinGui.addVmFrame(vmProxy);
                } else {
                    vmControlFrame.setVisible(true);
                }
            }

        } else if (event.getActionCommand().equals(PROBE)) {
            if (this.popupTreeObject instanceof Proxy) {
                Proxy proxy = (Proxy) this.popupTreeObject;
                this.villeinGui.getXmppVillein().probeJid(proxy.getFullJid());
            }
        } else if (event.getActionCommand().equals(FARM_CONFIGURATION)) {
            if (this.popupTreeObject instanceof FarmProxy) {
                FarmProxy farmProxy = (FarmProxy) this.popupTreeObject;
                JFrame farmFrame = new JFrame(farmProxy.getFullJid());
                farmFrame.getContentPane().add(new ViewFarmConfigurationPanel(farmProxy, villeinGui));
                farmFrame.pack();
                farmFrame.setSize(600, 600);
                farmFrame.setVisible(true);
                farmFrame.setResizable(true);
            }
        } else if (event.getActionCommand().equals(REGISTRY_COUNTRYSIDES)) {
            if (this.popupTreeObject instanceof RegistryProxy) {
                RegistryProxy registryProxy = (RegistryProxy) this.popupTreeObject;
                JFrame farmFrame = new JFrame(registryProxy.getFullJid());
                farmFrame.getContentPane().add(new ViewRegistryCountrysidesPanel(registryProxy, villeinGui));
                farmFrame.pack();
                farmFrame.setVisible(true);
                farmFrame.setResizable(true);
            }
        } else if (event.getActionCommand().equals(SHUTDOWN)) {
            this.villeinGui.getXmppVillein().shutDown(null);
            this.villeinGui.loadLoginFrame();
        } else {
            for (String vmSpecies : this.supportedVmSpeciesActionCommands) {
                if (event.getActionCommand().equals(vmSpecies)) {
                    if (this.popupTreeObject instanceof FarmProxy) {
                        FarmProxy farmProxy = (FarmProxy) this.popupTreeObject;
                        Handler<VmProxy> resultHandler = new Handler<VmProxy>() {
                            public void handle(VmProxy vmProxy) {
                                villeinGui.updateHostAreaTree(vmProxy.getFullJid(), false);
                            }
                        };
                        farmProxy.spawnVm(vmSpecies, resultHandler, new GenericErrorHandler());
                        break;
                    }
                }
            }
        }
    }

    public void createTree() {
        treeRoot.removeAllChildren();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        for (CountrysideProxy countrysideProxy : this.villeinGui.getXmppVillein().getCountrysideProxies()) {
            DefaultMutableTreeNode countrysideNode = new DefaultMutableTreeNode(countrysideProxy);
            for (RegistryProxy registryProxy : countrysideProxy.getRegistryProxies()) {
                DefaultMutableTreeNode registryNode = new DefaultMutableTreeNode(registryProxy);
                model.insertNodeInto(registryNode, countrysideNode, countrysideNode.getChildCount());
                this.tree.scrollPathToVisible(new TreePath(registryNode.getPath()));
            }
            for (FarmProxy farmProxy : countrysideProxy.getFarmProxies()) {
                DefaultMutableTreeNode farmNode = new DefaultMutableTreeNode(farmProxy);
                for (VmProxy vmProxy : farmProxy.getVmProxies()) {
                    DefaultMutableTreeNode vmNode = new DefaultMutableTreeNode(vmProxy);
                    model.insertNodeInto(vmNode, farmNode, farmNode.getChildCount());
                    this.tree.scrollPathToVisible(new TreePath(vmNode.getPath()));
                    DefaultMutableTreeNode temp;

                    if (vmProxy.getPresence() != null) {
                        temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_status", vmProxy.getPresence().getType().toString()));
                        model.insertNodeInto(temp, vmNode, vmNode.getChildCount());
                    }
                    if (vmProxy.getVmSpecies() != null) {
                        temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_species", vmProxy.getVmSpecies()));
                        model.insertNodeInto(temp, vmNode, vmNode.getChildCount());
                    }
                    /*if (vmProxy.getVmPassword() != null) {
                        temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_password", vmProxy.getVmPassword()));
                        model.insertNodeInto(temp, vmNode, vmNode.getChildCount());
                    }*/
                }
                model.insertNodeInto(farmNode, countrysideNode, countrysideNode.getChildCount());
                this.tree.scrollPathToVisible(new TreePath(farmNode.getPath()));
            }

            model.insertNodeInto(countrysideNode, this.treeRoot, this.treeRoot.getChildCount());
            this.tree.scrollPathToVisible(new TreePath(countrysideNode.getPath()));
        }
        model.reload();
    }

    private DefaultMutableTreeNode getNode(DefaultMutableTreeNode root, String jid) {
        if (root.getUserObject() instanceof Proxy) {
            Proxy temp = (Proxy) root.getUserObject();
            if (temp.getFullJid().equals(jid)) {
                return root;
            }
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode node = getNode((DefaultMutableTreeNode) root.getChildAt(i), jid);
            if (node != null)
                return node;
        }
        return null;
    }


    public void updateTree(String jid, boolean remove) {
        DefaultMutableTreeNode node = this.getNode(this.treeRoot, jid);
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

        if (node != null) {
            if (remove) {
                node.removeAllChildren();
                model.removeNodeFromParent(node);
            } else {
                if (node.getUserObject() instanceof CountrysideProxy || node.getUserObject() instanceof RegistryProxy || node.getUserObject() instanceof FarmProxy) {
                    this.tree.scrollPathToVisible(new TreePath(node.getPath()));
                    model.reload(node);
                } else if (node.getUserObject() instanceof VmProxy) {
                    node.removeAllChildren();
                    VmProxy vmProxy = (VmProxy) node.getUserObject();
                    DefaultMutableTreeNode temp;

                    if (vmProxy.getPresence() != null) {
                        temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_status", vmProxy.getPresence().getType().toString()));
                        model.insertNodeInto(temp, node, node.getChildCount());
                    }
                    if (vmProxy.getVmSpecies() != null) {
                        temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_species", vmProxy.getVmSpecies()));
                        model.insertNodeInto(temp, node, node.getChildCount());
                    }
                    /*if (vmProxy.getVmPassword() != null) {
                        temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_password", vmProxy.getVmPassword()));
                        model.insertNodeInto(temp, node, node.getChildCount());
                    }*/
                    this.tree.scrollPathToVisible(new TreePath(node.getPath()));
                    model.reload(node);
                } else {
                    XmppVillein.LOGGER.severe("Unknown node/proxy object: " + node.getUserObject());
                }
            }
        } else {
            if (!remove) {
                Proxy parentProxy = this.villeinGui.getXmppVillein().getParentProxy(jid);
                DefaultMutableTreeNode parentNode = null;
                if (parentProxy != null) {
                    parentNode = this.getNode(this.treeRoot, parentProxy.getFullJid());
                }

                Proxy proxy = this.villeinGui.getXmppVillein().getProxy(jid);
                if (proxy instanceof CountrysideProxy) {
                    DefaultMutableTreeNode countrysideNode = new DefaultMutableTreeNode(proxy);
                    model.insertNodeInto(countrysideNode, this.treeRoot, this.treeRoot.getChildCount());
                    this.tree.scrollPathToVisible(new TreePath(countrysideNode.getPath()));
                    model.reload(countrysideNode);
                } else if (proxy instanceof RegistryProxy || proxy instanceof FarmProxy) {
                    if (parentNode != null) {
                        DefaultMutableTreeNode otherNode = new DefaultMutableTreeNode(proxy);
                        model.insertNodeInto(otherNode, parentNode, parentNode.getChildCount());
                        this.tree.scrollPathToVisible(new TreePath(otherNode.getPath()));
                        model.reload(otherNode);
                    } else {
                        parentProxy = this.villeinGui.getXmppVillein().getParentProxy(LinkedProcess.generateBareJid(jid));
                        parentNode = this.getNode(this.treeRoot, parentProxy.getFullJid());
                        if (parentNode != null) {
                            DefaultMutableTreeNode otherNode = new DefaultMutableTreeNode(proxy);
                            model.insertNodeInto(otherNode, parentNode, parentNode.getChildCount());
                            this.tree.scrollPathToVisible(new TreePath(otherNode.getPath()));
                            model.reload(otherNode);
                        }
                    }
                } else if (proxy instanceof VmProxy) {
                    if (parentNode != null) {
                        VmProxy vmProxy = (VmProxy) proxy;
                        DefaultMutableTreeNode vmNode = new DefaultMutableTreeNode(proxy);
                        DefaultMutableTreeNode temp;

                        if (vmProxy.getPresence() != null) {
                            temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_status", vmProxy.getPresence().getType().toString()));
                            model.insertNodeInto(temp, vmNode, vmNode.getChildCount());
                            this.tree.scrollPathToVisible(new TreePath(temp.getPath()));
                        }
                        if (vmProxy.getVmSpecies() != null) {
                            temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_species", vmProxy.getVmSpecies()));
                            model.insertNodeInto(temp, vmNode, vmNode.getChildCount());
                            this.tree.scrollPathToVisible(new TreePath(temp.getPath()));
                        }
                        /*if (vmProxy.getVmPassword() != null) {
                            temp = new DefaultMutableTreeNode(new TreeNodeProperty("vm_password", vmProxy.getVmPassword()));
                            model.insertNodeInto(temp, vmNode, vmNode.getChildCount());
                            this.tree.scrollPathToVisible(new TreePath(temp.getPath()));
                        }*/

                        model.insertNodeInto(vmNode, parentNode, parentNode.getChildCount());
                        this.tree.scrollPathToVisible(new TreePath(vmNode.getPath()));
                        model.reload(vmNode);

                    }
                }
            }
        }
    }

    public void mouseClicked(MouseEvent event) {
        int x = event.getX();
        int y = event.getY();

        int selectedRow = tree.getRowForLocation(x, y);
        if (selectedRow != -1) {

            TreePath selectedPath = tree.getPathForLocation(x, y);
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            this.popupTreeObject = selectedNode.getUserObject();

            if (event.getButton() == MouseEvent.BUTTON3 && event.getClickCount() == 1) {
                if (this.popupTreeObject instanceof CountrysideProxy) {
                    this.createCountrysidePopupMenu();
                } else if (this.popupTreeObject instanceof RegistryProxy) {
                    this.createRegistryPopupMenu();
                } else if (this.popupTreeObject instanceof FarmProxy) {
                    this.createFarmPopupMenu((FarmProxy) this.popupTreeObject);
                } else if (this.popupTreeObject instanceof VmProxy) {
                    this.createVmPopupMenu();
                }

                popupMenu.setLocation(x + villeinGui.getX(), y + villeinGui.getY());
                popupMenu.show(event.getComponent(), event.getX(), event.getY());

            } else if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() > 1) {
                if (this.popupTreeObject instanceof VmProxy) {
                    VmProxy vmProxy = (VmProxy) this.popupTreeObject;
                    VmControlFrame vmControlFrame = this.villeinGui.getVmFrame(vmProxy.getFullJid());
                    if (vmControlFrame == null) {
                        this.villeinGui.addVmFrame(vmProxy);
                    } else {
                        vmControlFrame.setVisible(true);
                    }

                }
            }

        }
    }

    public void createCountrysidePopupMenu() {
        this.popupMenu = new JPopupMenu();
        this.popupMenu.setBorder(new BevelBorder(6));
        JLabel menuLabel = new JLabel("Countryside");
        JMenuItem probeItem = new JMenuItem(PROBE);
        menuLabel.setHorizontalTextPosition(JLabel.CENTER);
        this.popupMenu.add(menuLabel);
        this.popupMenu.addSeparator();
        this.popupMenu.add(probeItem);
        probeItem.addActionListener(this);
    }

    public void createRegistryPopupMenu() {
        this.popupMenu = new JPopupMenu();
        this.popupMenu.setBorder(new BevelBorder(6));
        JLabel menuLabel = new JLabel("Registry");
        JMenuItem probeResource = new JMenuItem(PROBE);
        JMenuItem discoItems = new JMenuItem(REGISTRY_COUNTRYSIDES);

        menuLabel.setHorizontalTextPosition(JLabel.CENTER);
        this.popupMenu.add(menuLabel);
        this.popupMenu.addSeparator();
        this.popupMenu.add(probeResource);
        this.popupMenu.add(discoItems);
        discoItems.addActionListener(this);
        probeResource.addActionListener(this);
    }

    public void createFarmPopupMenu(FarmProxy farmProxy) {
        this.popupMenu = new JPopupMenu();
        this.popupMenu.setBorder(new BevelBorder(6));
        JLabel menuLabel = new JLabel("Farm");
        JMenuItem probeResource = new JMenuItem(PROBE);
        JMenuItem discoInfo = new JMenuItem(FARM_CONFIGURATION);
        JMenu spawnMenu = new JMenu(SPAWN_VM);

        for (String vmSpecies : farmProxy.getSupportedVmSpecies()) {
            JMenuItem speciesItem = new JMenuItem(vmSpecies);
            speciesItem.addActionListener(this);
            this.supportedVmSpeciesActionCommands.add(vmSpecies);
            spawnMenu.add(speciesItem);
        }

        menuLabel.setHorizontalTextPosition(JLabel.CENTER);
        this.popupMenu.add(menuLabel);
        this.popupMenu.addSeparator();
        this.popupMenu.add(probeResource);
        this.popupMenu.add(discoInfo);
        this.popupMenu.add(spawnMenu);
        discoInfo.addActionListener(this);
        probeResource.addActionListener(this);
    }

    public void createVmPopupMenu() {
        this.popupMenu = new JPopupMenu();
        this.popupMenu.setBorder(new BevelBorder(6));
        JLabel menuLabel = new JLabel("Virtual Machine");
        JMenuItem probeResource = new JMenuItem(PROBE);
        JMenuItem vmControlItem = new JMenuItem(VM_CONTROL);
        JMenuItem terminateVmItem = new JMenuItem(TERMINATE_VM);
        menuLabel.setHorizontalTextPosition(JLabel.CENTER);
        this.popupMenu.add(menuLabel);
        this.popupMenu.addSeparator();
        this.popupMenu.add(probeResource);
        this.popupMenu.add(vmControlItem);
        this.popupMenu.add(terminateVmItem);
        terminateVmItem.addActionListener(this);
        vmControlItem.addActionListener(this);
        probeResource.addActionListener(this);
    }

    public void mouseReleased(MouseEvent e) {
        this.popupMenu.setVisible(false);
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mousePressed(MouseEvent event) {

    }

    public void handlePresenceUpdate(Proxy proxy, Presence.Type presenceType) {
        if (presenceType == Presence.Type.unavailable || presenceType == Presence.Type.unsubscribe || presenceType == Presence.Type.unsubscribed) {
            this.villeinGui.updateHostAreaTree(proxy.getFullJid(), true);
        } else {
            this.villeinGui.updateHostAreaTree(proxy.getFullJid(), false);
        }
    }
}
