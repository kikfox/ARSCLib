package com.reandroid.lib.arsc.value.style;

import com.reandroid.lib.arsc.chunk.PackageBlock;
import com.reandroid.lib.arsc.chunk.TableBlock;
import com.reandroid.lib.arsc.item.SpecString;
import com.reandroid.lib.arsc.item.TableString;
import com.reandroid.lib.arsc.pool.SpecStringPool;
import com.reandroid.lib.arsc.pool.TableStringPool;
import com.reandroid.lib.arsc.value.EntryBlock;
import com.reandroid.lib.arsc.value.ResValueBagItem;
import com.reandroid.lib.arsc.value.ValueType;

import java.util.ArrayList;
import java.util.List;

public class StyleBagItem {
    private final ResValueBagItem mBagItem;
    public StyleBagItem(ResValueBagItem bagItem){
        this.mBagItem=bagItem;
    }
    public ResValueBagItem getBagItem() {
        return mBagItem;
    }

    public String getName(){
        EntryBlock block=getBagItem().getEntryBlock();
        if(block==null){
            return null;
        }
        char prefix=0;
        return block.buildResourceName(getNameId(), prefix, false);
    }
    public int getNameId(){
        return getBagItem().getId();
    }
    public boolean hasStringValue(){
        return getValueType()== ValueType.STRING;
    }
    public boolean hasReferenceValue(){
        return getValueType()==ValueType.REFERENCE;
    }
    public boolean hasAttributeValue(){
        return getValueType()==ValueType.REFERENCE;
    }
    public String getValueAsReference(){
        ValueType valueType=getValueType();
        if(valueType!=ValueType.REFERENCE && valueType!=ValueType.ATTRIBUTE){
            throw new IllegalArgumentException("Not REF ValueType="+valueType);
        }
        EntryBlock entryBlock=getBagItem().getEntryBlock();
        if(entryBlock==null){
            return null;
        }
        char prefix='@';
        boolean includeType=true;
        if(valueType==ValueType.ATTRIBUTE){
            prefix='?';
            includeType=false;
        }
        int id=getValue();
        return entryBlock.buildResourceName(id, prefix, includeType);
    }
    public String getStringValue(){
        ValueType valueType=getValueType();
        if(valueType!=ValueType.STRING){
            throw new IllegalArgumentException("Not STRING ValueType="+valueType);
        }
        TableStringPool stringPool=getStringPool();
        if(stringPool==null){
            return null;
        }
        int ref=getValue();
        TableString tableString = stringPool.get(ref);
        if(tableString==null){
            return null;
        }
        return tableString.getHtml();
    }
    public ValueType getValueType(){
        return getBagItem().getValueType();
    }
    public int getValue(){
        return getBagItem().getData();
    }
    private TableStringPool getStringPool(){
        EntryBlock entryBlock=getBagItem().getEntryBlock();
        if(entryBlock==null){
            return null;
        }
        PackageBlock pkg = entryBlock.getPackageBlock();
        if(pkg==null){
            return null;
        }
        TableBlock tableBlock= pkg.getTableBlock();
        if(tableBlock==null){
            return null;
        }
        return tableBlock.getTableStringPool();
    }
    @Override
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append("<item name=\"");
        String name=getName();
        if(name==null){
            name=String.format("@0x%08x", getNameId());
        }
        builder.append(name);
        builder.append("\">");
        if(hasStringValue()){
            builder.append(getStringValue());
        }
        String val=null;
        if(hasReferenceValue()||hasAttributeValue()) {
            val=getValueAsReference();
        }
        if(val==null) {
            val=String.format("0x%08x", getValue());
        }
        builder.append(val);
        builder.append("</item>");
        return builder.toString();
    }
    public static StyleBagItem[] create(ResValueBagItem[] resValueBagItems){
        if(resValueBagItems==null){
            return null;
        }
        int len=resValueBagItems.length;
        if(len==0){
            return null;
        }
        List<StyleBagItem> results=new ArrayList<>();
        for(int i=0;i<len;i++){
            StyleBagItem item=create(resValueBagItems[i]);
            if(item==null){
                return null;
            }
            results.add(item);
        }
        return results.toArray(new StyleBagItem[0]);
    }
    public static StyleBagItem create(ResValueBagItem resValueBagItem){
        if(resValueBagItem==null){
            return null;
        }
        StyleBagItem item=new StyleBagItem(resValueBagItem);
        return item;
    }
}