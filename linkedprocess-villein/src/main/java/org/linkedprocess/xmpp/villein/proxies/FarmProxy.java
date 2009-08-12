package org.linkedprocess.xmpp.villein.proxies;

import org.linkedprocess.xmpp.villein.proxies.Proxy;
import org.linkedprocess.xmpp.villein.proxies.VmProxy;
import org.linkedprocess.xmpp.villein.Handler;
import org.linkedprocess.xmpp.villein.Dispatcher;
import org.linkedprocess.xmpp.villein.XmppVillein;
import org.linkedprocess.xmpp.LopError;
import org.linkedprocess.LinkedProcess;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.*;
import java.io.IOException;

/**
 * User: marko
 * Date: Jul 8, 2009
 * Time: 9:53:17 AM
 */
public class FarmProxy extends Proxy {

    protected Map<String, VmProxy> vmProxies = new HashMap<String, VmProxy>();
    protected Collection<String> supportedVmSpecies = new HashSet<String>();
    protected String farmPassword;


    public FarmProxy(final String fullJid, final Dispatcher dispatcher) {
        super(fullJid, dispatcher);
    }

    public FarmProxy(final String fullJid, final Dispatcher dispatcher, final Document discoInfoDocument) {
        super(fullJid, dispatcher, discoInfoDocument);
    }

    public VmProxy getVmProxy(String vmJid) {
        return vmProxies.get(vmJid);
    }

    public void addVmProxy(VmProxy vmProxy) {
        this.vmProxies.put(vmProxy.getFullJid(), vmProxy);
    }

    public Collection<VmProxy> getVmProxies() {
        return this.vmProxies.values();
    }

    public void removeVmProxy(String vmJid) {
        this.vmProxies.remove(vmJid);
    }

    public Collection<String> getSupportedVmSpecies() {
        return this.supportedVmSpecies;
    }

    public void addSupportedVmSpecies(String supportedVmSpecies) {
        this.supportedVmSpecies.add(supportedVmSpecies);
    }

    public void setSupportedVmSpecies(Collection<String> supportedVmSpecies) {
        this.supportedVmSpecies = supportedVmSpecies;
    }

    public String getFarmPassword() {
        return this.farmPassword;
    }

    public void setFarmPassword(String farmPassword) {
        this.farmPassword = farmPassword;
    }

    public boolean isFarmPasswordRequired() {
        Field field = this.getField(LinkedProcess.FARM_PASSWORD_REQUIRED);
        if(null != field) {
            return field.getBooleanValue();
        }
        else {
            return false;
        }
    }

    public long getVmTimeToLive() {
        Field field = this.getField(LinkedProcess.VM_TIME_TO_LIVE);
        if(null != field) {
            return field.getLongValue();
        } else {
            return -1;
        }
    }

    public long getJobTimeout() {
        Field field = this.getField(LinkedProcess.JOB_TIMEOUT);
        if(null != field) {
            return field.getLongValue();
        } else {
            return -1;
        }
    }

    public void spawnVm(final String vmSpecies, final Handler<VmProxy> resultHandler, final Handler<LopError> errorHandler) {
        this.dispatcher.getSpawnVmCommand().send(this, vmSpecies, resultHandler, errorHandler);
    }
}
