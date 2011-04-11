/*
 * Copyright © 2010 Red Hat, Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.redhat.rhevm.api.powershell.resource;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.redhat.rhevm.api.common.util.ReflectionHelper;
import com.redhat.rhevm.api.common.util.TimeZoneMapping;
import com.redhat.rhevm.api.model.CpuTopology;
import com.redhat.rhevm.api.model.Display;
import com.redhat.rhevm.api.model.DisplayType;
import com.redhat.rhevm.api.model.VM;
import com.redhat.rhevm.api.model.VMs;
import com.redhat.rhevm.api.model.VmType;
import com.redhat.rhevm.api.resource.VmResource;
import com.redhat.rhevm.api.resource.VmsResource;
import com.redhat.rhevm.api.powershell.model.PowerShellVM;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;
import com.redhat.rhevm.api.powershell.util.PowerShellUtils;

import static com.redhat.rhevm.api.common.util.CompletenessAssertor.validateParameters;
import static com.redhat.rhevm.api.common.util.DetailHelper.include;

public class PowerShellVmsResource
    extends AbstractPowerShellCollectionResource<VM, PowerShellVmResource>
    implements VmsResource {

    private static final String PROCESS_VMS;
    static {
        StringBuilder buf = new StringBuilder();
        buf.append(" | ");
        buf.append("foreach '{ ");
        buf.append(PowerShellUtils.getDateHack("creationdate"));
        buf.append("'{0} ");
        buf.append(" {1} ");
        buf.append("if ($_.runningonhost -ne ''-1'') '{");
        buf.append("  $h = get-host $_.runningonhost;");
        buf.append("  $nics = $h.getnetworkadapters();");
        buf.append("  $nets = get-networks -clusterid $h.hostclusterid;");
        buf.append("  foreach ($net in $nets) {");
        buf.append("    if ($net.isdisplay) {");
        buf.append("      $display_net = $net.name;");
        buf.append("      break;");
        buf.append("    }");
        buf.append("  }");
        buf.append("  if (!$display_net) {");
        buf.append("    $display_net = "+ PowerShellUtils.escape("rhevm") + ";");
        buf.append("  }");
        buf.append("  $display_nic = $nics[0];");
        buf.append("  foreach ($nic in $nics) {");
        buf.append("    if ($nic.network -eq $display_net) {");
        buf.append("      $display_nic = $nic;");
        buf.append("    }");
        buf.append("  }");
        buf.append("  $display_nic;");
        buf.append("}");
        buf.append("}'");
        PROCESS_VMS = buf.toString();
    }

    public enum Method { GET, ADD };

    public enum Detail {
        DISKS("$_.getdiskimages(); "),
        NICS("$_.getnetworkadapters(); "),
        STATISTICS("$_.getmemorystatistics(); $_.getcpustatistics(); "),
        TAGS("get-tags -vmid $_.vmid; ");

        public final String powershell;

        Detail(String powershell) {
            this.powershell = powershell;
        }
    }

    public List<PowerShellVM> runAndParse(String command, Set<Detail> details) {
        return PowerShellVmResource.runAndParse(getPool(), getParser(), command, details);
    }

    public PowerShellVM runAndParseSingle(String command, Set<Detail> details) {
        return PowerShellVmResource.runAndParseSingle(getPool(), getParser(), command, details);
    }

    @Override
    public VMs list() {
        VMs ret = new VMs();
        Set<Detail> details = getDetails();
        for (PowerShellVM vm : runAndParse(getSelectCommand("select-vm", getUriInfo(), VM.class) + getProcess(Method.GET, details), details)) {
            ret.getVMs().add(PowerShellVmResource.addLinks(getUriInfo(), vm));
        }
        return ret;
    }

    @Override
    public Response add(VM vm) {
        validateParameters(vm, "name", "template.id|name", "cluster.id|name");
        StringBuilder buf = new StringBuilder();
        Response response = null;

        String templateArg = null;
        if (vm.getTemplate().isSetId()) {
            templateArg = PowerShellUtils.escape(vm.getTemplate().getId());
        } else {
            buf.append("$t = select-template -searchtext ");
            buf.append(PowerShellUtils.escape("name=" + vm.getTemplate().getName()));
            buf.append(";");
            templateArg = "$t.TemplateId";
        }

        String clusterArg = null;
        if (vm.getCluster().isSetId()) {
            clusterArg = PowerShellUtils.escape(vm.getCluster().getId());
        } else {
            buf.append("$c = select-cluster -searchtext ");
            buf.append(PowerShellUtils.escape("name=" +  vm.getCluster().getName()));
            buf.append(";");
            clusterArg = "$c.ClusterId";
        }

        buf.append("$templ = get-template -templateid " + templateArg + ";");

        buf.append("$v = add-vm");

        buf.append(" -name " + PowerShellUtils.escape(vm.getName()) + "");
        if (vm.getDescription() != null) {
            buf.append(" -description " + PowerShellUtils.escape(vm.getDescription()));
        }
        if (vm.isSetStateless() && vm.isStateless()) {
            buf.append(" -stateless");
        }
        if (vm.isSetTimezone()) {
            String windowsTz = TimeZoneMapping.getWindows(vm.getTimezone());
            if (windowsTz != null) {
                buf.append(" -timezone " + PowerShellUtils.escape(windowsTz));
            }
        }
        if (vm.isSetHighAvailability() &&
            vm.getHighAvailability().isSetEnabled() &&
            vm.getHighAvailability().isEnabled()) {
            buf.append(" -highlyavailable");
            if (vm.getHighAvailability().isSetPriority()) {
                buf.append(" -priority " + Integer.toString(vm.getHighAvailability().getPriority()));
            }
        }
        if (vm.isSetDisplay()) {
            Display display = vm.getDisplay();
            if (display.isSetMonitors()) {
                buf.append(" -numofmonitors " + display.getMonitors());
            }
            if (display.isSetType()) {
                DisplayType displayType = DisplayType.fromValue(display.getType());
                if (displayType != null) {
                    buf.append(" -displaytype " + PowerShellVM.asString(displayType));
                }
            }
            // display port cannot be specified on creation, but the value
            // provided may in fact be correct (we won't know until we create
            // the VM) so for now we silently ignore a client-specified value
        }
        buf.append(" -templateobject $templ");
        buf.append(" -hostclusterid " + clusterArg);
        if (vm.isSetType()) {
            VmType vmType = VmType.fromValue(vm.getType());
            if (vmType != null) {
                buf.append(" -vmtype " + ReflectionHelper.capitalize(vmType.value()));
            }
        }
        if (vm.isSetMemory()) {
            buf.append(" -memorysize " + Math.round((double)vm.getMemory()/(1024*1024)));
        }
        if (vm.getCpu() != null && vm.getCpu().getTopology() != null) {
            CpuTopology topology = vm.getCpu().getTopology();
            buf.append(" -numofsockets " + topology.getSockets());
            buf.append(" -numofcpuspersocket " + topology.getCores());
        }
        String bootSequence = PowerShellVM.buildBootSequence(vm.getOs());
        if (bootSequence != null) {
            buf.append(" -defaultbootsequence " + bootSequence);
        }
        if (vm.isSetOs() && vm.getOs().isSetType()) {
            buf.append(" -os " + PowerShellUtils.escape(vm.getOs().getType()));
        }

        boolean expectBlocking = expectBlocking();
        final String displayVm = ";$v;";
        if (expectBlocking) {
            buf.append(displayVm);
        } else {
            buf.append(ASYNC_OPTION).append(displayVm).append(ASYNC_TASKS);
        }

        Set<Detail> details = getDetails();

        buf.append("$v").append(getProcess(Method.ADD, details));

        PowerShellVM created = runAndParseSingle(buf.toString(), details);

        if (expectBlocking || created.getTaskIds() == null) {
            vm = PowerShellVmResource.addLinks(getUriInfo(), created);
            UriBuilder uriBuilder = getUriInfo().getAbsolutePathBuilder().path(vm.getId());
            response = Response.created(uriBuilder.build()).entity(vm).build();
        } else {
            vm = addStatus(getUriInfo(), PowerShellVmResource.addLinks(getUriInfo(), created), created.getTaskIds());
            response = Response.status(202).entity(vm).build();
        }

        return response;
    }

    @Override
    public void remove(String id) {
        PowerShellCmd.runCommand(getPool(), "remove-vm -vmid " + PowerShellUtils.escape(id));
        removeSubResource(id);
    }

    @Override
    public VmResource getVmSubResource(String id) {
        return getSubResource(id);
    }

    @Override
    protected PowerShellVmResource createSubResource(String id) {
        return new PowerShellVmResource(id, getExecutor(), this, shellPools, getParser(), getHttpHeaders());
    }

    static String getProcess(Method method, Set<Detail> details) {
        StringBuilder buf = new StringBuilder();
        if (details != null) {
            for (Detail detail : details) {
                buf.append(detail.powershell);
            }
        }
        return MessageFormat.format(PROCESS_VMS, method == Method.ADD ? " " : "$_; ", buf);
    }

    private Set<Detail> getDetails() {
        Set<Detail> details = EnumSet.noneOf(Detail.class);
        for (Detail detail : Detail.class.getEnumConstants()) {
            if (include(getHttpHeaders(), detail.name().toLowerCase())) {
                details.add(detail);
            }
        }
        return details;
    }
}
