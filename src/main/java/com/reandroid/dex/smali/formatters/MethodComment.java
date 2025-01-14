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
package com.reandroid.dex.smali.formatters;

import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.model.DexClassRepository;
import com.reandroid.dex.model.DexMethod;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.SingleIterator;

import java.io.IOException;

public interface MethodComment extends SmaliComment{

    void writeComment(SmaliWriter writer, MethodKey methodKey) throws IOException;

    class MethodOverrideComment implements MethodComment {

        private final DexClassRepository classRepository;

        public MethodOverrideComment(DexClassRepository classRepository){
            this.classRepository = classRepository;
        }

        @Override
        public void writeComment(SmaliWriter writer, MethodKey methodKey) throws IOException {
            DexMethod dexMethod = classRepository.getDeclaredMethod(methodKey);
            if (dexMethod != null && !dexMethod.isDirect()) {
                DexMethod superMethod = dexMethod.getSuperMethod();
                if (superMethod != null) {
                    SmaliComment.writeDeclarationComment(
                            writer,
                            "overrides:",
                            SingleIterator.of(superMethod));
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodOverrideComment)) {
                return false;
            }
            MethodOverrideComment that = (MethodOverrideComment) obj;
            return classRepository == that.classRepository;
        }
        @Override
        public int hashCode() {
            return ObjectsUtil.hash(classRepository, getClass());
        }
    }
    class MethodImplementComment implements MethodComment {

        private final DexClassRepository classRepository;

        public MethodImplementComment(DexClassRepository classRepository){
            this.classRepository = classRepository;
        }

        @Override
        public void writeComment(SmaliWriter writer, MethodKey methodKey) throws IOException {
            DexMethod dexMethod = classRepository.getDeclaredMethod(methodKey);
            if (dexMethod != null && !dexMethod.isDirect() && !dexMethod.getDexClass().isFinal()) {
                SmaliComment.writeDeclarationComment(
                        writer,
                        "implemented-by:",
                        dexMethod.getOverriding());
            }
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodImplementComment)) {
                return false;
            }
            MethodImplementComment that = (MethodImplementComment) obj;
            return classRepository == that.classRepository;
        }
        @Override
        public int hashCode() {
            return ObjectsUtil.hash(classRepository, getClass());
        }
    }
}
