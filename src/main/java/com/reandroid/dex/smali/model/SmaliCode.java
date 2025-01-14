/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.dex.smali.model;

import com.reandroid.common.Origin;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.smali.SmaliReader;

import java.io.IOException;

public class SmaliCode extends Smali {

    public SmaliCode(){
        super();
    }

    public SmaliCodeSet getCodeSet(){
        return getParentInstance(SmaliCodeSet.class);
    }
    @Override
    public void parse(SmaliReader reader) throws IOException {

    }
    public String buildOrigin() {
        StringBuilder builder = new StringBuilder();
        Origin origin = getOrigin();
        if (origin != null) {
            builder.append('\n');
            builder.append(origin);
        } else {
            SmaliMethod method = getParentInstance(SmaliMethod.class);
            if (method != null) {
                MethodKey key = method.getKey();
                if (key != null) {
                    builder.append(" on method: ");
                    builder.append(key);
                }
            }
        }
        return builder.toString();
    }
}
