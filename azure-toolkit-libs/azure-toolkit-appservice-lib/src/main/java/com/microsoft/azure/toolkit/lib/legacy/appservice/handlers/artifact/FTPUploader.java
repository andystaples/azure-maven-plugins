/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Utility class to upload directory to FTP server
 */
@Slf4j
public class FTPUploader {
    public static final String UPLOAD_START = "Uploading files to FTP server: ";
    public static final String UPLOAD_SUCCESS = "Successfully uploaded files to FTP server: ";
    public static final String UPLOAD_FAILURE = "Failed to upload files to FTP server, retrying immediately (%d/%d)";
    public static final String UPLOAD_RETRY_FAILURE = "Failed to upload files to FTP server after %d retries...";
    public static final String UPLOAD_DIR_START = "Uploading directory: %s --> %s";
    public static final String UPLOAD_DIR_FINISH = "Successfully uploaded directory: %s --> %s";
    public static final String UPLOAD_DIR_FAILURE = "Failed to upload directory: %s --> %s";
    public static final String UPLOAD_DIR = "%s[DIR] %s --> %s";
    public static final String UPLOAD_FILE = "%s[FILE] %s --> %s";
    public static final String UPLOAD_FILE_REPLY = "%s.......Reply Message : %s";

    /**
     * Upload directory to specified FTP server with retries.
     */
    public void uploadDirectoryWithRetries(final String ftpServer, final String username, final String password,
                                           final String sourceDirectory, final String targetDirectory,
                                           final int maxRetryCount) throws AzureExecutionException {
        int retryCount = 0;
        while (retryCount < maxRetryCount) {
            retryCount++;
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(UPLOAD_START + ftpServer);
            if (uploadDirectory(ftpServer, username, password, sourceDirectory, targetDirectory)) {
                messager.success(UPLOAD_SUCCESS + ftpServer);
                return;
            } else {
                messager.warning(String.format(UPLOAD_FAILURE, retryCount, maxRetryCount));
            }
        }
        // Reaching here means all retries failed.
        throw new AzureExecutionException(String.format(UPLOAD_RETRY_FAILURE, maxRetryCount));
    }

    /**
     * Upload directory to specified FTP server without retries.
     * @return Boolean to indicate whether uploading is successful.
     */
    protected boolean uploadDirectory(final String ftpServer, final String username, final String password,
                                      final String sourceDirectoryPath, final String targetDirectoryPath) {
        final IAzureMessager messager = AzureMessager.getMessager();
        log.debug("FTP username: " + username);
        try {
            final FTPClient ftpClient = getFTPClient(ftpServer, username, password);

            messager.info(String.format(UPLOAD_DIR_START, sourceDirectoryPath, targetDirectoryPath));
            uploadDirectory(ftpClient, sourceDirectoryPath, targetDirectoryPath, "");
            messager.success(String.format(UPLOAD_DIR_FINISH, sourceDirectoryPath, targetDirectoryPath));

            ftpClient.disconnect();
            return true;
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            messager.error(String.format(UPLOAD_DIR_FAILURE, sourceDirectoryPath, targetDirectoryPath));
        }

        return false;
    }

    /**
     * Recursively upload a directory to FTP server with the provided FTP client object.
     */
    protected void uploadDirectory(final FTPClient ftpClient, final String sourceDirectoryPath,
                                   final String targetDirectoryPath, final String logPrefix) throws IOException {
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(String.format(UPLOAD_DIR, logPrefix, sourceDirectoryPath, targetDirectoryPath));
        final File sourceDirectory = new File(sourceDirectoryPath);
        final File[] files = sourceDirectory.listFiles();
        if (files == null || files.length == 0) {
            messager.info(String.format("%sEmpty directory at %s", logPrefix, sourceDirectoryPath));
            return;
        }

        // Make sure target directory exists
        final boolean isTargetDirectoryExist = ftpClient.changeWorkingDirectory(targetDirectoryPath);
        if (!isTargetDirectoryExist) {
            ftpClient.makeDirectory(targetDirectoryPath);
        }

        final String nextLevelPrefix = logPrefix + "..";
        for (final File file : files) {
            if (file.isFile()) {
                uploadFile(ftpClient, file.getAbsolutePath(), targetDirectoryPath, nextLevelPrefix);
            } else {
                uploadDirectory(ftpClient, Paths.get(sourceDirectoryPath, file.getName()).toString(),
                        targetDirectoryPath + "/" + file.getName(), nextLevelPrefix);
            }
        }
    }

    /**
     * Upload a single file to FTP server with the provided FTP client object.
     */
    protected void uploadFile(final FTPClient ftpClient, final String sourceFilePath, final String targetFilePath,
                              final String logPrefix) throws IOException {
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(String.format(UPLOAD_FILE, logPrefix, sourceFilePath, targetFilePath));
        final File sourceFile = new File(sourceFilePath);
        try (final InputStream is = new FileInputStream(sourceFile)) {
            ftpClient.changeWorkingDirectory(targetFilePath);
            ftpClient.storeFile(sourceFile.getName(), is);

            final int replyCode = ftpClient.getReplyCode();
            final String replyMessage = ftpClient.getReplyString();
            if (isCommandFailed(replyCode)) {
                messager.error(String.format(UPLOAD_FILE_REPLY, logPrefix, replyMessage));
                throw new IOException("Failed to upload file: " + sourceFilePath);
            } else {
                messager.info(String.format(UPLOAD_FILE_REPLY, logPrefix, replyMessage));
            }
        }
    }

    protected FTPClient getFTPClient(final String ftpServer, final String username, final String password)
            throws IOException {
        final FTPClient ftpClient = new FTPClient();
        ftpClient.connect(ftpServer);
        ftpClient.login(username, password);
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        return ftpClient;
    }

    private boolean isCommandFailed(final int replyCode) {
        // https://en.wikipedia.org/wiki/List_of_FTP_server_return_codes
        // 2xx means command has been successfully completed
        return replyCode >= 300;
    }
}
