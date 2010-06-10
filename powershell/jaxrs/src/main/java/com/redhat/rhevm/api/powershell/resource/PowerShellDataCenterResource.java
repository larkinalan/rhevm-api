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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.rhevm.api.model.Attachments;
import com.redhat.rhevm.api.model.DataCenter;
import com.redhat.rhevm.api.model.Link;
import com.redhat.rhevm.api.resource.DataCenterResource;
import com.redhat.rhevm.api.resource.IsosResource;
import com.redhat.rhevm.api.common.resource.AbstractActionableResource;
import com.redhat.rhevm.api.powershell.model.PowerShellDataCenter;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;

public class PowerShellDataCenterResource extends AbstractActionableResource<DataCenter> implements DataCenterResource {

    public PowerShellDataCenterResource(String id, Executor executor) {
        super(id, executor);
    }

    public static ArrayList<DataCenter> runAndParse(String command) {
        return PowerShellDataCenter.parse(PowerShellCmd.runCommand(command));
    }

    public static DataCenter runAndParseSingle(String command) {
        ArrayList<DataCenter> dataCenters = runAndParse(command);

        return !dataCenters.isEmpty() ? dataCenters.get(0) : null;
    }

    public static DataCenter addLinks(DataCenter dataCenter, UriInfo uriInfo, UriBuilder uriBuilder) {
        dataCenter.setHref(uriBuilder.build().toString());

        Attachments attachments = PowerShellAttachmentsResource.getAttachmentsForDataCenter(uriInfo, dataCenter.getId());
        dataCenter.setAttachments(attachments);

        Link link = new Link();
        link.setRel("isos");
        link.setHref(uriBuilder.clone().path("isos").build().toString());
        dataCenter.getLinks().clear();
        dataCenter.getLinks().add(link);

        return dataCenter;
    }

    @Override
    public DataCenter get(UriInfo uriInfo) {
        return addLinks(runAndParseSingle("get-datacenter " + getId()), uriInfo, uriInfo.getRequestUriBuilder());
    }

    @Override
    public DataCenter update(HttpHeaders headers, UriInfo uriInfo, DataCenter dataCenter) {
        validateUpdate(dataCenter, headers);

        StringBuilder buf = new StringBuilder();

        buf.append("$h = get-datacenter " + getId() + "\n");

        if (dataCenter.getName() != null) {
            buf.append("$h.name = '" + dataCenter.getName() + "'");
        }
        if (dataCenter.getDescription() != null) {
            buf.append("$h.description = '" + dataCenter.getDescription() + "'");
        }

        buf.append("\n");
        buf.append("update-datacenter -datacenterobject $v");

        return addLinks(runAndParseSingle(buf.toString()), uriInfo, uriInfo.getRequestUriBuilder());
    }

    public IsosResource getIsosResource() {
        return new PowerShellIsosResource(getId());
    }
}
