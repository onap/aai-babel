/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.babel.xml.generator.model;

import java.util.List;
import org.onap.aai.babel.xml.generator.types.Cardinality;
import org.onap.aai.babel.xml.generator.types.Model;

@Model(widget = Widget.Type.VFMODULE, cardinality = Cardinality.UNBOUNDED, dataDeleteFlag = true)
public class VfModule extends Resource {

    Widget vserver = null;
    boolean addlintf = false;
    boolean addvolume = false;

    List<String> members;

    public void setMembers(List<String> members) {
        this.members = members;
    }

    /**
     * Adds Widget.
     *
     * @param widget the widget
     * @return the boolean
     */
    @Override
    public boolean addWidget(Widget widget) {
        if (widget.memberOf(members)) {
            if (vserver == null && widget.getId().equals(new VServerWidget().getId())) {
                addVserverWidget(widget);
            } else if (widget.getId().equals(new LIntfWidget().getId())) {
                return addLIntfWidget(widget);
            } else if (widget.getId().equals(new VolumeWidget().getId())) {
                addVolumeWidget(widget);
                return true;
            }
            if (widget.getId().equals(new OamNetwork().getId())) {
                return false;
            }
            return widgets.add(widget);
        }
        return false;
    }

    private void addVolumeWidget(Widget widget) {
        if (vserver != null) {
            vserver.addWidget(widget);
        } else {
            addvolume = true;
        }
    }

    /**
     * @param widget
     * @return
     */
    private boolean addLIntfWidget(Widget widget) {
        if (vserver != null) {
            vserver.addWidget(widget);
            return true;
        } else {
            addlintf = true;
            return false;
        }
    }

    private void addVserverWidget(Widget widget) {
        vserver = widget;
        if (addlintf) {
            vserver.addWidget(new LIntfWidget());
        }
        if (addvolume) {
            vserver.addWidget(new VolumeWidget());
        }
    }

    @Override
    public int hashCode() {
        return getModelNameVersionId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            return getModelNameVersionId().equals(((Resource) obj).getModelNameVersionId());
        }
        return false;
    }
}
