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
package com.reandroid.dex.item;

import com.reandroid.dex.base.Ule128Item;
import com.reandroid.dex.common.AccessFlag;
import com.reandroid.dex.index.ClassId;
import com.reandroid.dex.index.ItemId;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.writer.SmaliFormat;
import com.reandroid.dex.writer.SmaliWriter;

import java.io.IOException;
import java.util.Iterator;

public class Def<T extends ItemId> extends DexContainerItem implements SmaliFormat {
    private final SectionUle128Item<T> id;
    private final Ule128Item accessFlags;
    private ClassId classId;
    public Def(int childesCount, SectionType<T> sectionType) {
        super(childesCount + 2);
        this.id = new SectionUle128Item<>(sectionType, true);
        this.accessFlags = new Ule128Item();
        addChild(0, id);
        addChild(1, accessFlags);
    }
    public Iterator<AnnotationSet> getAnnotations(){
        return null;
    }
    public AnnotationsDirectory getAnnotationsDirectory(){
        ClassId classId = getClassId();
        if(classId != null){
            return classId.getAnnotationsDirectory();
        }
        return null;
    }
    public boolean appendAnnotations(SmaliWriter writer) throws IOException {
        boolean appendOnce = false;
        Iterator<AnnotationSet> iterator = getAnnotations();
        while (iterator.hasNext()){
            iterator.next().append(writer);
            appendOnce = true;
        }
        return appendOnce;
    }
    public ClassId getClassId() {
        return classId;
    }
    public void setClassId(ClassId classId) {
        this.classId = classId;
    }
    public int getIdValue() {
        return id.get();
    }
    public int getAccessFlagsValue() {
        return accessFlags.get();
    }
    public boolean isStatic(){
        return AccessFlag.STATIC.isSet(getAccessFlagsValue());
    }
    public int getDefIndexId() {
        DefArray<?> parentArray = getParentInstance(DefArray.class);
        if(parentArray != null){
            Def previous = parentArray.get(getIndex() - 1);
            if(previous != null){
                return getIdValue() + previous.getDefIndexId();
            }
        }
        return id.get();
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {

    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        id.refresh();
    }
}
