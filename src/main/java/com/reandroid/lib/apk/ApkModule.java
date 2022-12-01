package com.reandroid.lib.apk;

import com.reandroid.archive.APKArchive;
import com.reandroid.archive.InputSource;
import com.reandroid.lib.arsc.array.PackageArray;
import com.reandroid.lib.arsc.chunk.PackageBlock;
import com.reandroid.lib.arsc.chunk.TableBlock;
import com.reandroid.lib.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.lib.arsc.group.StringGroup;
import com.reandroid.lib.arsc.item.TableString;
import com.reandroid.lib.arsc.pool.TableStringPool;
import com.reandroid.lib.arsc.value.EntryBlock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ApkModule {
    private final APKArchive apkArchive;
    private boolean loadDefaultFramework = true;
    private TableBlock mTableBlock;
    private AndroidManifestBlock mManifestBlock;
    private ApkModule(APKArchive apkArchive){
        this.apkArchive=apkArchive;
    }
    public void writeTo(File file) throws IOException {
        APKArchive archive=getApkArchive();
        archive.writeApk(file);
    }
    public void removeDir(String dirName){
        getApkArchive().removeDir(dirName);
    }
    public void setResourcesRootDir(String dirName) throws IOException {
        List<ResFile> resFileList = listResFiles();
        Set<String> existPaths=new HashSet<>();
        List<InputSource> sourceList = getApkArchive().listInputSources();
        for(InputSource inputSource:sourceList){
            existPaths.add(inputSource.getAlias());
        }
        for(ResFile resFile:resFileList){
            String path=resFile.getFilePath();
            String pathNew=ApkUtil.replaceRootDir(path, dirName);
            if(existPaths.contains(pathNew)){
                continue;
            }
            existPaths.remove(path);
            existPaths.add(pathNew);
            resFile.setFilePath(pathNew);
        }
        TableStringPool stringPool= getTableBlock().getTableStringPool();
        stringPool.refreshUniqueIdMap();
    }
    public List<ResFile> listResFiles() throws IOException {
        List<ResFile> results=new ArrayList<>();
        TableBlock tableBlock=getTableBlock();
        TableStringPool stringPool= tableBlock.getTableStringPool();
        for(InputSource inputSource:getApkArchive().listInputSources()){
            String name=inputSource.getAlias();
            StringGroup<TableString> groupTableString = stringPool.get(name);
            if(groupTableString==null){
                continue;
            }
            for(TableString tableString:groupTableString.listItems()){
                List<EntryBlock> entryBlockList = tableString.listReferencedEntries();
                ResFile resFile=new ResFile(inputSource, entryBlockList);
                results.add(resFile);
            }
        }
        return results;
    }
    public String getPackageName() throws IOException {
        if(hasAndroidManifestBlock()){
            return getAndroidManifestBlock().getPackageName();
        }
        if(!hasTableBlock()){
            return null;
        }
        TableBlock tableBlock=getTableBlock();
        PackageArray pkgArray = tableBlock.getPackageArray();
        PackageBlock pkg = pkgArray.get(0);
        if(pkg==null){
            return null;
        }
        return pkg.getPackageName();
    }
    public void setPackageName(String name) throws IOException {
        String old=getPackageName();
        if(hasAndroidManifestBlock()){
            getAndroidManifestBlock().setPackageName(name);
        }
        if(!hasTableBlock()){
            return;
        }
        TableBlock tableBlock=getTableBlock();
        PackageArray pkgArray = tableBlock.getPackageArray();
        for(PackageBlock pkg:pkgArray.listItems()){
            if(pkgArray.childesCount()==1){
                pkg.setPackageName(name);
                continue;
            }
            String pkgName=pkg.getPackageName();
            if(pkgName.startsWith(old)){
                pkgName=pkgName.replace(old, name);
                pkg.setPackageName(pkgName);
            }
        }
    }
    public boolean hasAndroidManifestBlock(){
        return mManifestBlock!=null
                || getApkArchive().getInputSource(AndroidManifestBlock.FILE_NAME)!=null;
    }
    public AndroidManifestBlock getAndroidManifestBlock() throws IOException {
        if(mManifestBlock!=null){
            return mManifestBlock;
        }
        APKArchive archive=getApkArchive();
        InputSource inputSource = archive.getInputSource(AndroidManifestBlock.FILE_NAME);
        if(inputSource==null){
            throw new IOException("Entry not found: "+AndroidManifestBlock.FILE_NAME);
        }
        InputStream inputStream = inputSource.openStream();
        AndroidManifestBlock manifestBlock=AndroidManifestBlock.load(inputStream);
        inputStream.close();
        BlockInputSource<AndroidManifestBlock> blockInputSource=new BlockInputSource<>(inputSource.getName(),manifestBlock);
        blockInputSource.setSort(inputSource.getSort());
        blockInputSource.setMethod(inputSource.getMethod());
        archive.add(blockInputSource);
        mManifestBlock=manifestBlock;
        return mManifestBlock;
    }
    public boolean hasTableBlock(){
        return mTableBlock!=null
                || getApkArchive().getInputSource(TableBlock.FILE_NAME)!=null;
    }
    public TableBlock getTableBlock() throws IOException {
        if(mTableBlock!=null){
            return mTableBlock;
        }
        APKArchive archive=getApkArchive();
        InputSource inputSource = archive.getInputSource(TableBlock.FILE_NAME);
        if(inputSource==null){
            throw new IOException("Entry not found: "+TableBlock.FILE_NAME);
        }
        TableBlock tableBlock;
        InputStream inputStream = inputSource.openStream();
        if(loadDefaultFramework){
            tableBlock=TableBlock.loadWithAndroidFramework(inputStream);
        }else {
            tableBlock=TableBlock.load(inputStream);
        }
        inputStream.close();
        mTableBlock=tableBlock;
        BlockInputSource<TableBlock> blockInputSource=new BlockInputSource<>(inputSource.getName(),tableBlock);
        blockInputSource.setMethod(inputSource.getMethod());
        blockInputSource.setSort(inputSource.getSort());
        archive.add(blockInputSource);
        return mTableBlock;
    }
    public APKArchive getApkArchive() {
        return apkArchive;
    }
    public void setLoadDefaultFramework(boolean loadDefaultFramework) {
        this.loadDefaultFramework = loadDefaultFramework;
    }
    public void convertToJson(File outDir) throws IOException {
        Set<String> convertedFiles=new HashSet<>();
        if(hasAndroidManifestBlock()){
            AndroidManifestBlock manifestBlock=getAndroidManifestBlock();
            String fileName=AndroidManifestBlock.FILE_NAME+ApkUtil.JSON_FILE_EXTENSION;
            File file=new File(outDir, fileName);
            manifestBlock.toJson().write(file);
            convertedFiles.add(AndroidManifestBlock.FILE_NAME);
        }
        if(hasTableBlock()){
            TableBlock tableBlock=getTableBlock();
            String fileName=TableBlock.FILE_NAME+ApkUtil.JSON_FILE_EXTENSION;
            File file=new File(outDir, fileName);
            tableBlock.toJson().write(file);
            convertedFiles.add(TableBlock.FILE_NAME);
        }
        List<ResFile> resFileList=listResFiles();
        for(ResFile resFile:resFileList){
            boolean convertOk=resFile.dumpToJson(outDir);
            if(convertOk){
                convertedFiles.add(resFile.getFilePath());
            }
        }
        List<InputSource> allSources = getApkArchive().listInputSources();
        for(InputSource inputSource:allSources){
            String path=inputSource.getAlias();
            if(convertedFiles.contains(path)){
                continue;
            }
            path=path.replace('/', File.separatorChar);
            File file=new File(outDir, path);
            File dir=file.getParentFile();
            if(dir!=null && !dir.exists()){
                dir.mkdirs();
            }
            FileOutputStream outputStream=new FileOutputStream(file);
            inputSource.write(outputStream);
            outputStream.close();
        }
    }
    public static ApkModule loadApkFile(File apkFile) throws IOException {
        APKArchive archive=APKArchive.loadZippedApk(apkFile);
        return new ApkModule(archive);
    }
}
