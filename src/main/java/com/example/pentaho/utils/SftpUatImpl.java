package com.example.pentaho.utils;

import com.example.pentaho.utils.custom.Sftp;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Properties;
import java.util.Vector;


@Component
@ConfigurationProperties(prefix = "sftputils")
@Profile("uat || another")
public class SftpUatImpl implements Sftp {
    private static final Logger log = LoggerFactory.getLogger(SftpUatImpl.class);
    private String host;
    private String username;
    private int port;
    private String password;

    private Session session;

    private ChannelSftp sftp;

    /**
     * 建立SFTP連線
     */
    public void connect() {
        log.info("in uat");
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            if (log.isInfoEnabled()) {
                log.info("Session created.");
            }
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sshConfig);
            session.setPassword(password);
            session.connect();
            if (log.isInfoEnabled()) {
                log.info("Session connected.");
            }
            Channel channel = session.openChannel("sftp");
            channel.connect();
            if (log.isInfoEnabled()) {
                log.info("Opening Channel.");
            }
            sftp = (ChannelSftp) channel;
            if (log.isInfoEnabled()) {
                log.info("Connected to " + host + ".");
            }
        } catch (Exception e) {
            log.error("connect error ", e);
        }
    }

    /**
     * 關閉連線
     */
    public void disconnect() {
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
                if (log.isInfoEnabled()) {
                    log.info("sftp is closed already");
                }
            }
        }
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
                if (log.isInfoEnabled()) {
                    log.info("sshSession is closed already");
                }
            }
        }
    }


    /**
     * 下載單筆檔案
     *
     * @param remotePath：遠端下載目錄(以路徑符號结束)
     * @param remoteFileName：下載文件名
     * @return
     */
    public boolean downloadFile(String localPath,String remotePath, String remoteFileName) {
        log.info("remotePath:{}",remotePath);
        log.info("remoteFileName:{}",remoteFileName);
        FileOutputStream fieloutput = null;
        try {
            File file = new File(localPath + remoteFileName);
            fieloutput = new FileOutputStream(file);
            sftp.get(remotePath + remoteFileName, fieloutput);
            if (log.isInfoEnabled()) {
                log.info("===DownloadFile:" + remoteFileName + " success from sftp.");
            }
            return true;
        } catch (FileNotFoundException e) {
            log.error("FileNotFound error ", e);
        } catch (SftpException e) {
            log.error("Sftp error ", e);
        } finally {
            if (null != fieloutput) {
                try {
                    fieloutput.close();
                } catch (IOException e) {
                    log.error("IO error ", e);
                }
            }
        }
        return false;
    }

    /**
     *
     * @param targertDir
     * @param file
     * @param newFileName
     * @return
     */
    public boolean uploadFile(String targertDir,MultipartFile file, String newFileName) {
//        connect();
        FileInputStream in = null;
        try {
            createDir(targertDir);
            sftp.put(file.getInputStream(), newFileName);
//            disconnect();
            return true;
        } catch (SftpException | IOException e) {
            log.error("sftp error:{} ", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("io error ", e);
                }
            }
        }
//        disconnect();
        return false;
    }


    /**
     * 創建目錄
     *
     * @param createpath
     * @return
     */
    public boolean createDir(String createpath) {
        try {
            if (isDirExist(createpath)) {
                this.sftp.cd(createpath);
                return true;
            }
            String pathArry[] = createpath.split("/");
            StringBuilder filePath = new StringBuilder("/");
            for (String path : pathArry) {
                if (path.equals("")) {
                    continue;
                }
                filePath.append(path + "/");
                if (isDirExist(filePath.toString())) {
                    sftp.cd(filePath.toString());
                } else {
                    sftp.mkdir(filePath.toString());
                    sftp.cd(filePath.toString());
                }

            }
            this.sftp.cd(createpath);
            return true;
        } catch (SftpException e) {
            log.error("Sftp error ", e);
        }
        return false;
    }

    /**
     * 判断目錄是否存在
     *
     * @param directory
     * @return
     */
    public boolean isDirExist(String directory) {
        boolean isDirExistFlag = false;
        try {
            SftpATTRS sftpATTRS = sftp.lstat(directory);
            isDirExistFlag = true;
            return sftpATTRS.isDir();
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().equals("no such file")) {
                isDirExistFlag = false;
            }
        }
        return isDirExistFlag;
    }

    /**
     * 删除stfp文件
     *
     * @param directory：要删除文件所在目錄
     * @param deleteFile：要删除的文件
     */
    public void deleteSFTP(String directory, String deleteFile) {
        try {
            sftp.rm(directory + deleteFile);
            if (log.isInfoEnabled()) {
                log.info("delete file success from sftp.");
            }
        } catch (Exception e) {
            log.error("deleteSFTP error ", e);
        }
    }

    /**
     * 如果目錄不存在就創建目錄
     *
     * @param path
     */
    public void mkdirs(String path) {
        File f = new File(path);

        String fs = f.getParent();

        f = new File(fs);

        if (!f.exists()) {
            f.mkdirs();
        }
    }

    /**
     * 列出目錄下的文件
     *
     * @param ：要列出的目錄
     * @return
     * @throws SftpException
     */
    public boolean listFiles(String directory,String fileName) throws SftpException {
        try {
            log.info("比對目錄:{}", directory);
            log.info("比對檔名:{}", fileName);
            Vector<LsEntry> entries = sftp.ls(directory);
            for (LsEntry ls : entries) {
                log.info("檔案:{}", ls.getFilename());
                if (fileName.equals(ls.getFilename())) {
                    return true;
                }
            }
            return false;
        }catch (Exception e){
            log.info("e:{}",e.toString());
            return false;
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
