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

import java.util.ArrayList;
import java.util.concurrent.Executor;

import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.model.CpuTopology;
import com.redhat.rhevm.api.model.Link;
import com.redhat.rhevm.api.model.Template;
import com.redhat.rhevm.api.resource.TemplateResource;
import com.redhat.rhevm.api.common.resource.AbstractActionableResource;
import com.redhat.rhevm.api.common.util.JAXBHelper;
import com.redhat.rhevm.api.common.util.LinkHelper;
import com.redhat.rhevm.api.powershell.model.PowerShellTemplate;
import com.redhat.rhevm.api.powershell.model.PowerShellVM;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;
import com.redhat.rhevm.api.powershell.util.PowerShellPoolMap;
import com.redhat.rhevm.api.powershell.util.PowerShellUtils;


public class PowerShellTemplateResource extends AbstractPowerShellActionableResource<Template> implements TemplateResource {

    public PowerShellTemplateResource(String id, Executor executor, PowerShellPoolMap shellPools) {
        super(id, executor, shellPools);
    }

    public static ArrayList<PowerShellTemplate> runAndParse(PowerShellCmd shell, String command) {
        return PowerShellTemplate.parse(PowerShellCmd.runCommand(shell, command));
    }

    public static PowerShellTemplate runAndParseSingle(PowerShellCmd shell, String command) {
        ArrayList<PowerShellTemplate> templates = runAndParse(shell, command);

        return !templates.isEmpty() ? templates.get(0) : null;
    }

    public static Template addLinks(PowerShellTemplate template) {
        Template ret = JAXBHelper.clone("template", Template.class, template);

        String [] deviceCollections = { "cdroms", "disks", "nics" };

        ret.getLinks().clear();

        for (String collection : deviceCollections) {
            Link link = new Link();
            link.setRel(collection);
            link.setHref(LinkHelper.getUriBuilder(ret).path(collection).build().toString());
            ret.getLinks().add(link);
        }

        return LinkHelper.addLinks(ret);
    }

    @Override
    public Template get(UriInfo uriInfo) {
        StringBuilder buf = new StringBuilder();

        buf.append("get-template -templateid " + PowerShellUtils.escape(getId()));

        return addLinks(runAndParseSingle(getShell(), buf.toString()));
    }

    @Override
    public Template update(UriInfo uriInfo, Template template) {
        validateUpdate(template);

        StringBuilder buf = new StringBuilder();

        buf.append("$t = get-template " + PowerShellUtils.escape(getId()) + "\n");

        if (template.getName() != null) {
            buf.append("$t.name = " + PowerShellUtils.escape(template.getName()) + "\n");
        }
        if (template.getDescription() != null) {
            buf.append("$t.description = " + PowerShellUtils.escape(template.getDescription()) + "\n");
        }
        if (template.isSetMemory()) {
            buf.append(" $t.memsizemb = " + Math.round((double)template.getMemory()/(1024*1024)) + "\n");
        }
        if (template.getCpu() != null && template.getCpu().getTopology() != null) {
            CpuTopology topology = template.getCpu().getTopology();
            buf.append(" $t.numofsockets = " + topology.getSockets() + "\n");
            buf.append(" $t.numofcpuspersocket = " + topology.getCores() + "\n");
        }
        String bootSequence = PowerShellVM.buildBootSequence(template.getOs());
        if (bootSequence != null) {
            buf.append(" $t.defaultbootsequence = '" + bootSequence + "'\n");
        }

        buf.append("update-template -templateobject $t");

        return addLinks(runAndParseSingle(getShell(), buf.toString()));
    }

    public static class CdRomQuery extends PowerShellCdRomsResource.CdRomQuery {
        public CdRomQuery(String id) {
            super(id);
        }
        @Override protected String getCdIsoPath(PowerShellCmd shell) {
            return runAndParseSingle(shell, "get-template " + PowerShellUtils.escape(id)).getCdIsoPath();
        }
    }

    @Override
    public PowerShellReadOnlyCdRomsResource getCdRomsResource() {
        return new PowerShellReadOnlyCdRomsResource(getId(), shellPools, new CdRomQuery(getId()));
    }

    @Override
    public PowerShellReadOnlyDisksResource getDisksResource() {
        return new PowerShellReadOnlyDisksResource(getId(), shellPools, "get-template");
    }

    @Override
    public PowerShellReadOnlyNicsResource getNicsResource() {
        return new PowerShellReadOnlyNicsResource(getId(), shellPools, "get-template");
    }
}
