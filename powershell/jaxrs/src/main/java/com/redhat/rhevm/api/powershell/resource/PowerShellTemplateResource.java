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

import com.redhat.rhevm.api.model.Template;
import com.redhat.rhevm.api.resource.TemplateResource;
import com.redhat.rhevm.api.common.resource.AbstractActionableResource;
import com.redhat.rhevm.api.common.util.LinkHelper;
import com.redhat.rhevm.api.powershell.model.PowerShellTemplate;
import com.redhat.rhevm.api.powershell.util.PowerShellCmd;
import com.redhat.rhevm.api.powershell.util.PowerShellPoolMap;
import com.redhat.rhevm.api.powershell.util.PowerShellUtils;


public class PowerShellTemplateResource extends AbstractPowerShellActionableResource<Template> implements TemplateResource {

    public PowerShellTemplateResource(String id, Executor executor, PowerShellPoolMap powerShellPoolMap) {
        super(id, executor, powerShellPoolMap);
    }

    public static ArrayList<Template> runAndParse(PowerShellCmd shell, String command) {
        return PowerShellTemplate.parse(PowerShellCmd.runCommand(shell, command));
    }

    public static Template runAndParseSingle(PowerShellCmd shell, String command) {
        ArrayList<Template> templates = runAndParse(shell, command);

        return !templates.isEmpty() ? templates.get(0) : null;
    }

    @Override
    public Template get(UriInfo uriInfo) {
        StringBuilder buf = new StringBuilder();

        buf.append("get-template -templateid " + PowerShellUtils.escape(getId()));

        return LinkHelper.addLinks(runAndParseSingle(getShell(), buf.toString()));
    }
}
