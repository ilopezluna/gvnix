/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.gvnix.flex.roo.addon.as.classpath.details.metatag;

import org.gvnix.flex.roo.addon.as.model.ActionScriptSymbolName;

/**
 * String meta-tag attribute.
 * 
 * @author Jeremy Grelle
 */
public class StringAttributeValue extends AbstractMetaTagAttributeValue<String> {

    private final String value;

    public StringAttributeValue(ActionScriptSymbolName name, String value) {
        super(name);
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return getName() + " -> " + this.value;
    }

}