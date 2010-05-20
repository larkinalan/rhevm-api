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

import com.redhat.rhevm.api.model.Cluster;
import com.redhat.rhevm.api.model.DataCenter;
import com.redhat.rhevm.api.resource.ClusterResource;
import com.redhat.rhevm.api.common.resource.AbstractActionableResource;
import com.redhat.rhevm.api.powershell.model.PowerShellCluster;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;

public class PowerShellClusterResource extends AbstractActionableResource<Cluster> implements ClusterResource {

    public PowerShellClusterResource(String id, Executor executor) {
        super(id, executor);
    }

    public PowerShellClusterResource(String id) {
        super(id);
    }

    public static ArrayList<Cluster> runAndParse(String command) {
        return PowerShellCluster.parse(PowerShellCmd.runCommand(command));
    }

    public static Cluster runAndParseSingle(String command) {
        ArrayList<Cluster> clusters = runAndParse(command);

        return !clusters.isEmpty() ? clusters.get(0) : null;
    }

    public static Cluster addLinks(Cluster cluster, UriInfo uriInfo, UriBuilder uriBuilder) {
        cluster.setHref(uriBuilder.build().toString());

        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        DataCenter dataCenter = cluster.getDataCenter();
        dataCenter.setHref(PowerShellDataCentersResource.getHref(baseUriBuilder, dataCenter.getId()));

        return cluster;
    }

    @Override
    public Cluster get(UriInfo uriInfo) {
        StringBuilder buf = new StringBuilder();

        buf.append("$l = select-cluster\n");
        buf.append("foreach ($c in $l) {\n");
        buf.append("  if ($c.clusterid -eq \"" + getId() + "\") {\n");
        buf.append("    echo $c\n");
        buf.append("  }\n");
        buf.append("}");

        return addLinks(runAndParseSingle(buf.toString()), uriInfo, uriInfo.getRequestUriBuilder());
    }

    @Override
    public Cluster update(HttpHeaders headers, UriInfo uriInfo, Cluster cluster) {
        validateUpdate(cluster, headers);

        StringBuilder buf = new StringBuilder();

        buf.append("$l = select-cluster\n");
        buf.append("foreach ($c in $l) {\n");
        buf.append("  if ($c.clusterid -eq \"" + getId() + "\") {\n");

        if (cluster.getName() != null) {
            buf.append("    $c.name = \"" + cluster.getName() + "\"\n");
        }
        if (cluster.getCpu() != null) {
            buf.append("    $c.cpuname = \"" + cluster.getCpu().getId() + "\"\n");
        }

        buf.append("    update-datacenter -datacenterobject $v");

        buf.append("  }\n");
        buf.append("}");

        return addLinks(runAndParseSingle(buf.toString()), uriInfo, uriInfo.getRequestUriBuilder());
    }
}
