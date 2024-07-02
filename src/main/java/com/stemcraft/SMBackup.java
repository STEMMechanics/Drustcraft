package com.stemcraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import org.bukkit.Bukkit;

import static com.backblaze.b2.util.B2ExecutorUtils.createThreadFactory;

public class SMBackup {

    public static void backup() {
        STEMCraft.info("Saving player data");
        Bukkit.getServer().savePlayers();
        Bukkit.getServer().getWorlds().forEach(world -> {
            STEMCraft.info("Saving world " + world.getName());
            world.save();
        });

        // Your backup logic using Backblaze B2 SDK
        Bukkit.getScheduler().runTaskAsynchronously(STEMCraft.getPlugin(), () -> {
            String applicationKeyId = SMConfig.getString("config.backup.applicationId");
            String applicationKey = SMConfig.getString("config.backup.applicationKey");
            String bucketId = SMConfig.getString("config.backup.bucketId");
            File serverDirectory = new File(".");
            File[] serverFiles = serverDirectory.listFiles();

            File zipFile = new File("backup.zip");

            if(zipFile.exists()) {
                zipFile.delete();
            }

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                    recurseZip(serverFiles, zos, "");

            } catch (IOException e) {
                STEMCraft.error(e);
            }

            B2StorageClient client = B2StorageClientFactory
                    .createDefaultFactory()
                    .create(applicationKeyId, applicationKey, "STEMCraft Java Plugin");

            ConcurrentHashMap<Integer, Long> threadProgress = new ConcurrentHashMap<>();
            long totalFileLength = zipFile.length();

            final B2UploadListener uploadListener = (progress) -> {
                threadProgress.put(progress.getPartIndex(), progress.getBytesSoFar());

                long totalBytesUploaded = threadProgress.values().stream().mapToLong(Long::valueOf).sum();
                double percent = (100.0 * totalBytesUploaded / totalFileLength);
                STEMCraft.info(String.format("Total Upload Progress: %.2f%%", percent));
            };

            final ExecutorService executor = Executors.newSingleThreadExecutor();
            final B2FileVersion file1;
            try {
                final File fileOnDisk = new File("backup.zip");
                final B2ContentSource source = B2FileContentSource.build(fileOnDisk);
                final String fileName = "backup-text.zip";
                B2UploadFileRequest request = B2UploadFileRequest
                        .builder(bucketId, fileName, B2ContentTypes.B2_AUTO, source)
                        .setListener(uploadListener)
                        .build();
                file1 = client.uploadLargeFile(request, executor);
                STEMCraft.info("uploaded " + file1);
            } catch(Exception e) {
                STEMCraft.error(e);
            }
        });
    }

    private static void recurseZip(File[] fileList, ZipOutputStream zos, String parentDir) throws IOException {
        for (File file : fileList) {
            if(file.isDirectory()) {
                recurseZip(Objects.requireNonNull(file.listFiles()), zos, parentDir + file.getName() + "/");
                continue;
            } else if(file.getName().equalsIgnoreCase("backup.zip")) {
                continue;
            }

            STEMCraft.info("Zipping " + parentDir + file.getName());
            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(parentDir + file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
        }
    }
}
