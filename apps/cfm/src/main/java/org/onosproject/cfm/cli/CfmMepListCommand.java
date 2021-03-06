/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.cfm.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepKeyId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.slf4j.Logger;

import static org.onosproject.cfm.cli.CfmMdListMdCommand.parseMaName;
import static org.onosproject.cfm.cli.CfmMdListMdCommand.parseMdName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Lists a particular Maintenance Domain.
 */
@Command(scope = "onos", name = "cfm-mep-list",
        description = "Lists a filtered set of MEPs or all if no parameters specified.")
public class CfmMepListCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());
    @Argument(index = 0, name = "md",
            description = "Maintenance Domain name and type (in brackets) - will use all MDs if not specified",
            required = false, multiValued = false)
    String mdStr = null;
    @Argument(index = 1, name = "ma",
            description = "Maintenance Association name and type (in brackets) - requires MD",
            required = false, multiValued = false)
    String maStr = null;
    @Argument(index = 2, name = "mep",
            description = "MEP identifier - requires MD and MA",
            required = false, multiValued = false)
    String mepStr = null;

    @Override
    protected void execute() {
        CfmMepService mepService = get(CfmMepService.class);
        CfmMdService mdService = get(CfmMdService.class);

        if (mdStr != null && !mdStr.isEmpty()) {


            MdId mdId = parseMdName(mdStr);
            print(printMdId(mdId));

            if (maStr != null && !maStr.isEmpty()) {
                MaIdShort maId = parseMaName(maStr);
                print(printMaId(maId));

                if (mepStr != null && !mepStr.isEmpty()) {
                    MepId mepId = MepId.valueOf(Short.parseShort(mepStr));
                    try {
                        MepEntry mep = mepService.getMep(mdId, maId, mepId);
                        if (mep != null) {
                            print(printMepEntry(mep));
                        }
                    } catch (CfmConfigException e) {
                        log.error("Error retrieving Mep details {}",
                                new MepKeyId(mdId, maId, mepId), e);
                    }

                    //MD, MA and MEP given
                } else {
                    //MD and MA given but no MEP given
                    try {
                        mepService.getAllMeps(mdId, maId).forEach(mep -> {
                            print(printMepEntry(mep));
                        });
                    } catch (CfmConfigException e) {
                        log.error("Error retrieving Meps for {}/{}",
                                mdId.mdName(), maId.maName(), e);
                    }
                }
            } else {
                //MD given but no MA given
                mdService.getAllMaintenanceAssociation(mdId).forEach(ma -> {
                    print(printMaId(ma.maId()));
                    try {
                        mepService.getAllMeps(mdId, ma.maId()).forEach(mep -> {
                            print(printMepEntry(mep));
                        });
                    } catch (CfmConfigException e) {
                        log.error("Error retrieving Meps for {}/{}",
                                mdId.mdName(), ma.maId().maName(), e);
                    }
                });

            }
        } else {
            mdService.getAllMaintenanceDomain().forEach(md -> {
                print(printMdId(md.mdId()));

                mdService.getAllMaintenanceAssociation(md.mdId()).forEach(ma -> {
                    print(printMaId(ma.maId()));
                    try {
                        mepService.getAllMeps(md.mdId(), ma.maId()).forEach(mep -> {
                            print(printMepEntry(mep));
                        });
                    } catch (CfmConfigException e) {
                        log.error("Error retrieving Meps for {}/{}",
                                md.mdId().mdName(), ma.maId().maName(), e);
                    }
                });
            });
        }
    }

    /**
     * Print the whole MEP Entry (config and status).
     * @param mep The MEPEntry to print
     * @return A string with MepEntry details
     */
    public static String printMepEntry(MepEntry mep) {
        StringBuffer sb = new StringBuffer("MEP: ");
        sb.append(mep.mepId());
        sb.append(" Device:" + mep.deviceId());
        sb.append(", Port: " + mep.port());
        sb.append(", Vlan: " + mep.primaryVid());
        sb.append(", AdminSt: " + mep.administrativeState());
        sb.append(", CciEnabled: " + mep.cciEnabled());
        sb.append(", Priority: " + mep.ccmLtmPriority());
        sb.append("\n"); //The following are state
        sb.append(", Total CCMs: " + mep.totalCcmsTransmitted());
        sb.append(", MAC: " + mep.macAddress());
        sb.append(", Fault: " + mep.fngState());

        mep.activeRemoteMepList().forEach(rmep -> {
            sb.append("\n\tRmep: " + rmep.remoteMepId());
            sb.append(", Mac: " + rmep.macAddress());
            sb.append(", State: " + rmep.state());
            sb.append(", Failed Time: " + rmep.failedOrOkTime());

        });


        return sb.toString();
    }

    public static String printMdId(MdId mdId) {
        StringBuffer sb = new StringBuffer("MD: ");
        sb.append(mdId.mdName());
        sb.append("(" + mdId.nameType() + ")");
        return sb.toString();
    }

    public static String printMaId(MaIdShort maId) {
        StringBuffer sb = new StringBuffer("MA: ");
        sb.append(maId.maName());
        sb.append("(" + maId.nameType() + ")");
        return sb.toString();
    }

}
