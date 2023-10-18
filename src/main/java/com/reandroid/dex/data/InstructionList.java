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
package com.reandroid.dex.data;

import com.reandroid.arsc.base.BlockArray;
import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.container.FixedBlockContainer;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.dex.base.CreatorArray;
import com.reandroid.arsc.item.IntegerVisitor;
import com.reandroid.arsc.item.VisitableInteger;
import com.reandroid.dex.base.DexPositionAlign;
import com.reandroid.dex.debug.DebugElement;
import com.reandroid.dex.debug.DebugSequence;
import com.reandroid.dex.ins.*;
import com.reandroid.dex.writer.SmaliFormat;
import com.reandroid.dex.writer.SmaliWriter;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.EmptyIterator;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class InstructionList extends FixedBlockContainer implements
        Iterable<Ins>, SmaliFormat, VisitableInteger {

    private final CodeItem codeItem;
    private final BlockArray<Ins> insArray;
    private final DexPositionAlign blockAlign;

    public InstructionList(CodeItem codeItem){
        super(2);
        this.codeItem = codeItem;

        this.insArray = new CreatorArray<>(CREATOR);
        this.blockAlign = new DexPositionAlign();

        addChild(0, insArray);
        addChild(1, blockAlign);
    }

    public RegistersEditor editRegisters(){
        return RegistersEditor.fromIns(getCodeItem(), iterator());
    }

    public MethodDef getMethodDef() {
        return getCodeItem().getMethodDef();
    }
    public DebugSequence getDebugSequence(){
        DebugInfo debugInfo = getDebugInfo();
        if(debugInfo != null){
            return debugInfo.getDebugSequence();
        }
        return null;
    }
    public DebugSequence getOrCreateDebugSequence(){
        return getOrCreateDebugInfo().getDebugSequence();
    }
    public DebugInfo getDebugInfo(){
        return getCodeItem().getDebugInfo();
    }
    public DebugInfo getOrCreateDebugInfo(){
        return getCodeItem().getOrCreateDebugInfo();
    }
    public CodeItem getCodeItem() {
        return codeItem;
    }

    public<T1 extends Ins> Iterator<T1> iterator(Opcode<T1> opcode){
        return iterator(opcode, null);
    }
    @SuppressWarnings("unchecked")
    public<T1 extends Ins> Iterator<T1> iterator(Opcode<T1> opcode, Predicate<? super T1> filter){
        return ComputeIterator.of(iterator(), ins -> {
            T1 result = null;
            if(ins != null && ins.getOpcode() == opcode){
                result = (T1) ins;
                if(filter != null && !filter.test(result)){
                    result = null;
                }
            }
            return result;
        });
    }
    @Override
    public Iterator<Ins> iterator() {
        return getInsArray().iterator();
    }
    public Iterator<Ins> clonedIterator() {
        return getInsArray().clonedIterator();
    }
    public Iterator<Ins> arrayIterator() {
        return getInsArray().arrayIterator();
    }
    public Iterator<Ins> iterator(int start, int size) {
        return getInsArray().iterator(start, size);
    }
    public Iterator<Ins> iteratorByAddress(int startAddress, int codeUnits) {
        Ins insStart = getAtAddress(startAddress);
        if(insStart == null){
            return EmptyIterator.of();
        }
        Ins insEnd = getAtAddress(startAddress + codeUnits);
        int count = insEnd.getIndex() - insStart.getIndex();
        return this.iterator(insStart.getIndex(), count);
    }

    private BlockArray<Ins> getInsArray() {
        return insArray;
    }

    public Ins get(int i){
        return getInsArray().get(i);
    }
    public int getCount(){
        return getInsArray().getCount();
    }
    public void add(Ins ins){
        BlockArray<Ins> array = getInsArray();
        array.add(ins);
        Ins previous = array.get(ins.getIndex() - 1);
        int address;
        if(previous != null){
            address = previous.getAddress() + previous.getCodeUnits();
        }else {
            address = 0;
        }
        ins.setAddress(address);
    }
    public void add(int index, Ins item) {
        reBuildExtraLines();
        getInsArray().insertItem(index, item);
        updateAddresses();
        updateLabelAddress();
        reBuildExtraLines();
    }
    public void add(int index, Ins[] insArray) {
        reBuildExtraLines();
        getInsArray().insertItem(index, insArray);
        updateAddresses();
        updateLabelAddress();
        reBuildExtraLines();
    }
    public<T1 extends Ins> T1 createAt(int index, Opcode<T1> opcode) {
        T1 item = opcode.newInstance();
        add(index, item);
        return item;
    }
    public Ins[] createAt(int index, Opcode<?>[] opcodeArray) {
        int length = opcodeArray.length;
        Ins[] results = new Ins[length];
        for(int i = 0; i < length; i++){
            results[i] = opcodeArray[i].newInstance();
        }
        add(index, results);
        return results;
    }
    public<T1 extends Ins> T1 createNext(Opcode<T1> opcode) {
        T1 item = opcode.newInstance();
        add(item);
        return item;
    }
    public boolean isLonelyInTryCatch(Ins ins){
        buildExtraLines();
        int codeUnits = ins.getCodeUnits();
        Iterator<ExceptionHandler.TryStartLabel> iterator = ins.getExtraLines(
                ExceptionHandler.TryStartLabel.class);
        while (iterator.hasNext()){
            ExceptionHandler.TryStartLabel startLabel = iterator.next();
            int handlerCodeUnits = startLabel.getHandler().getCodeUnit();
            if(handlerCodeUnits <= codeUnits){
                return true;
            }
        }
        return false;
    }
    public boolean contains(Ins item){
        return getInsArray().contains(item);
    }
    public boolean remove(Ins item){
        return remove(item, false);
    }
    public boolean remove(Ins item, boolean force) {
        if(!contains(item)){
            return false;
        }
        if(!force && isLonelyInTryCatch(item)){
            return replaceWithNop(item) != null;
        }
        reBuildExtraLines();
        if(item.hasExtraLines()){
            Ins next = get(item.getIndex() + 1);
            if(next != null) {
                item.transferExtraLines(next);
            }
        }
        getInsArray().remove(item);
        item.setParent(null);
        item.setIndex(-1);
        updateAddresses();
        updateLabelAddress();
        return true;
    }
    public InsNop replaceWithNop(Ins ins){
        return replace(ins, Opcode.NOP);
    }
    public<T1 extends Ins> T1 replace(Ins old, Opcode<T1> opcode){
        if(!contains(old)){
            return null;
        }
        T1 item = opcode.newInstance();
        replace(old, item);
        return item;
    }
    public void replace(Ins old, Ins item) {
        if(old == item){
            return;
        }
        reBuildExtraLines();
        int index = old.getIndex();
        old.transferExtraLines(item);
        item.setAddress(old.getAddress());
        getInsArray().setItem(index, item);
        old.setParent(null);
        old.setIndex(-1);
        updateAddresses();
        updateLabelAddress();
    }
    private void updateLabelAddress() {
        for(Ins ins : this) {
            ins.updateLabelAddress();
        }
    }
    private void updateAddresses() {
        int outSize = 0;
        int address = 0;
        for(Ins ins : this) {
            ins.setAddress(address);
            address += ins.getCodeUnits();
            int out = ins.getOutSize();
            if(out > outSize){
                outSize = out;
            }
        }
        this.codeItem.getInstructionCodeUnitsReference().set(address);
        this.codeItem.getInstructionOutsReference().set(outSize);

    }

    @Override
    public void visitIntegers(IntegerVisitor visitor) {
        List<Ins> insList = CollectionUtil.toList(this.iterator());
        for(Ins ins : insList) {
            if(ins instanceof VisitableInteger){
                ((VisitableInteger)ins).visitIntegers(visitor);
            }
        }
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        this.blockAlign.align(this);
    }
    public int getCodeUnits() {
        int result = 0;
        for (Ins ins : this) {
            result += ins.getCodeUnits();
        }
        return result;
    }
    public int getOutSize() {
        int result = 0;
        for (Ins ins : this) {
            int count = ins.getOutSize();
            if(count > result){
                result = count;
            }
        }
        return result;
    }

    public DexPositionAlign getBlockAlign() {
        return blockAlign;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        buildExtraLines();
        for (Ins ins : this) {
            writer.newLine();
            ins.append(writer);
        }
    }
    @Override
    public void onReadBytes(BlockReader reader) throws IOException {

        int insCodeUnits = codeItem.getInstructionCodeUnitsReference().get();
        int position = reader.getPosition() + insCodeUnits * 2;
        int zeroPosition = reader.getPosition();

        int count = (insCodeUnits + 1) / 2;
        BlockArray<Ins> insBlockArray = getInsArray();
        insBlockArray.setChildesCount(count);
        int index = 0;

        while (reader.getPosition() < position){
            Opcode<?> opcode = Opcode.read(reader);
            Ins ins = opcode.newInstance();
            ins.setAddress((reader.getPosition() - zeroPosition) / 2);
            insBlockArray.setItem(index, ins);
            index ++;
            ins.readBytes(reader);
        }
        insBlockArray.setChildesCount(index);
        if(position != reader.getPosition()){
            // should not reach here
            reader.seek(position);
        }
        int totalRead = reader.getPosition() - zeroPosition;
        blockAlign.align(totalRead);
        reader.offset(blockAlign.size());
    }

    private void clearExtraLines() {
        for (Ins ins : this) {
            ins.clearExtraLines();
        }
    }
    private boolean haveExtraLines() {
        for (Ins ins : this) {
            if(ins.hasExtraLines()){
                return true;
            }
        }
        return false;
    }
    public void buildExtraLines(){
        if(haveExtraLines()){
            return;
        }
        buildLabels();
        buildTryBlock();
        buildDebugInfo();
    }
    public void reBuildExtraLines(){
        clearExtraLines();
        buildExtraLines();
    }
    public Ins getAtAddress(int address){
        for (Ins ins : this) {
            if(ins.getAddress() == address){
                return ins;
            }
        }
        return null;
    }
    private void addLabel(Label label){
        Ins target = getAtAddress(label.getTargetAddress());
        if(target != null){
            target.addExtraLine(label);
        }
    }
    private void addLabels(Iterator<? extends Label> iterator){
        while (iterator.hasNext()){
            addLabel(iterator.next());
        }
    }
    private void buildLabels(){
        for(Ins ins : this){
            if(ins instanceof Label){
                addLabel((Label) ins);
            }else if(ins instanceof LabelsSet){
                addLabels(((LabelsSet) ins).getLabels());
            }
            ins.trimExtraLines();
        }
    }
    private void buildTryBlock(){
        TryBlock tryBlock = this.codeItem.getTryBlock();
        if(tryBlock == null || tryBlock.isNull()){
            return;
        }
        addLabels(tryBlock.getLabels());
    }
    private void buildDebugInfo(){
        DebugInfo debugInfo = codeItem.getDebugInfo();
        if(debugInfo == null){
            return;
        }
        Iterator<DebugElement> iterator = debugInfo.getExtraLines();
        while (iterator.hasNext()){
            DebugElement element = iterator.next();
            Ins target = getAtAddress(element.getTargetAddress());
            if(target != null){
                target.addExtraLine(element);
            }
        }
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        SmaliWriter smaliWriter = new SmaliWriter(writer);
        smaliWriter.indentPlus();
        try {
            append(smaliWriter);
            smaliWriter.close();
        } catch (IOException exception) {
            return exception.toString();
        }
        return writer.toString();
    }
    private static final Creator<Ins> CREATOR = new Creator<Ins>() {
        @Override
        public Ins[] newInstance(int length) {
            return new Ins[length];
        }
        @Override
        public Ins newInstance() {
            return PLACE_HOLDER;
        }
    };

    private static final Ins PLACE_HOLDER = Opcode.NOP.newInstance();
}