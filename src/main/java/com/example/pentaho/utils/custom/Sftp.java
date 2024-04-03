package com.example.pentaho.utils.custom;

import com.jcraft.jsch.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public interface Sftp {

    public void connect();
    /**
     * 關閉連線
     */
    public void disconnect();


    /**
     * 下載單筆檔案
     *
     * @param remotePath：遠端下載目錄(以路徑符號结束)
     * @param remoteFileName：下載文件名
     * @return
     */
    public boolean downloadFile(String localPath,String remotePath, String remoteFileName);

    /**
     *
     * @param targertDir
     * @param file
     * @param newFileName
     * @return
     */
    public boolean uploadFile(String targertDir, MultipartFile file, String newFileName);


    /**
     * 創建目錄
     *
     * @param createpath
     * @return
     */
    public boolean createDir(String createpath);
    /**
     * 判断目錄是否存在
     *
     * @param directory
     * @return
     */
    public boolean isDirExist(String directory);
    /**
     * 删除stfp文件
     *
     * @param directory：要删除文件所在目錄
     * @param deleteFile：要删除的文件
     */
    public void deleteSFTP(String directory, String deleteFile);

    /**
     * 如果目錄不存在就創建目錄
     *
     * @param path
     */
    public void mkdirs(String path);

    /**
     * 列出目錄下的文件
     *
     * @param ：要列出的目錄
     * @return
     * @throws SftpException
     */
    public boolean listFiles(String directory,String fileName) throws SftpException;
}
