package com.stemcraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;


public class SMBackup {
    private static boolean inProgress = false;
    private static List<PathMatcher> exclusionMatchers = new ArrayList<>();

    /**
     * Initialize the backup system
     */
    public static void initialize() {
        String backupTimeStr = SMConfig.getString("config.backup.time");
        if (backupTimeStr == null) {
            STEMCraft.warning("Backup time not set in config.");
            return;
        }

        LocalTime backupTime = parseTime(backupTimeStr);
        if (backupTime == null) {
            STEMCraft.warning("Invalid backup time format: " + backupTimeStr);
            return;
        }

        List<String> exclusionPatterns = SMConfig.getStringList("config.backup.exclude");
        if(exclusionPatterns == null) {
            exclusionPatterns = new ArrayList<>();
        }

        exclusionPatterns.add("backups/*");
        exclusionMatchers = exclusionPatterns.stream()
                .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
                .collect(Collectors.toList());

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

        scheduler.scheduleSyncRepeatingTask(STEMCraft.getPlugin(), () -> {
            LocalTime now = LocalTime.now();
            long minutesUntilBackup = now.until(backupTime, ChronoUnit.MINUTES);

            if (minutesUntilBackup == 0) {
                SMBackup.backup();
            } else if (minutesUntilBackup > 0 && minutesUntilBackup <= 10) {
                STEMCraft.broadcast("Backup in " + minutesUntilBackup + " minute" + (minutesUntilBackup > 1 ? "s" : ""));
            }
        }, 0L, 20L * 60); // Run every minute
    }

    /**
     * Return if a backup is currently in progress.
     *
     * @return If a backup is in progress.
     */
    public static boolean inProgress() {
        return inProgress;
    }

    /**
     * Backup the server
     * @param upload Upload archive
     */
    public static void backup(boolean upload) {
        if(inProgress) {
            return;
        }

        String applicationKeyId = SMConfig.getString("config.backup.applicationId");
        String applicationKey = SMConfig.getString("config.backup.applicationKey");
        String bucketId = SMConfig.getString("config.backup.bucketId");

        if(applicationKeyId == null || applicationKey == null || bucketId == null) {
            STEMCraft.warning("Cannot perform backup as B2 data is missing");
            return;
        }

        inProgress = true;
        STEMCraft.broadcast("Backup has started...");
        STEMCraft.info("Saving player data");
        Bukkit.getServer().savePlayers();
        Bukkit.getServer().getWorlds().forEach(world -> {
            STEMCraft.info("Saving world " + world.getName());
            world.save();
        });

        Bukkit.getScheduler().runTaskAsynchronously(STEMCraft.getPlugin(), () -> {
            File serverDirectory = new File(".");
            File[] serverFiles = serverDirectory.listFiles();
            String backupDir = "backups";
            final String fileNamePattern = "\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}_STEMCraft-Backup\\.zip";
            int keepLast = SMConfig.getInt("config.backup.keepLast", 5);
            final String fileName = backupDir + File.separator +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) +
                    "_STEMCraft-Backup.zip";

            // Create the directory if it doesn't exist
            File directory = new File(backupDir);
            if (!directory.exists()) {
                //noinspection ResultOfMethodCallIgnored
                directory.mkdirs();
            }

            // Get all matching files and sort them by last modified time (newest first)
            if(keepLast > 0) {
                File[] backupFiles = directory.listFiles((dir, name) -> name.matches(fileNamePattern));
                if (backupFiles != null) {
                    Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified).reversed());

                    // Delete older files
                    for (int i = (keepLast - 1); i < backupFiles.length; i++) {
                        //noinspection ResultOfMethodCallIgnored
                        backupFiles[i].delete();
                    }
                }
            }

            File backupFile = new File(fileName);
            if(backupFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                backupFile.delete();
            }

            if(serverFiles != null) {
                try (FileOutputStream fos = new FileOutputStream(backupFile);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {
                    STEMCraft.info("Processing files...");
                    int count = recurseZip(serverFiles, zos, "", 0);
                    STEMCraft.info("Processed " + count + " files");

                } catch (IOException e) {
                    STEMCraft.error(e);
                }
            }

            try {
                if(upload) {
                    try (B2StorageClient client = B2StorageClientFactory
                            .createDefaultFactory()
                            .create(applicationKeyId, applicationKey, "STEMCraft Java Plugin")) {

                        ConcurrentHashMap<Integer, Long> threadProgress = new ConcurrentHashMap<>();
                        final AtomicInteger lastReportedPercent = new AtomicInteger(-1);
                        final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
                        long totalFileLength = backupFile.length();

                        final B2UploadListener uploadListener = (progress) -> {
                            threadProgress.put(progress.getPartIndex(), progress.getBytesSoFar());

                            long totalBytesUploaded = threadProgress.values().stream().mapToLong(Long::valueOf).sum();
                            int percent = (int) (100.0 * totalBytesUploaded / totalFileLength);

                            if (percent > lastReportedPercent.get() && lastReportedPercent.compareAndSet(lastReportedPercent.get(), percent)) {
                                long currentTime = System.currentTimeMillis();
                                long totalElapsedTime = currentTime - startTime.get();

                                if (percent == 0) {
                                    STEMCraft.info("Upload Backup: 0%%");
                                } else {
                                    // Calculate average upload speed
                                    double avgUploadSpeedMBps = (totalBytesUploaded / 1024.0 / 1024.0) / (totalElapsedTime / 1000.0);

                                    // Calculate estimated time remaining
                                    long remainingBytes = totalFileLength - totalBytesUploaded;
                                    long estimatedSecondsRemaining = (long) (remainingBytes / (totalBytesUploaded / (totalElapsedTime / 1000.0)));

                                    STEMCraft.info(String.format("Upload Backup: %d%% (%.2f MB/s, ETA: %02d:%02d)",
                                            percent, avgUploadSpeedMBps,
                                            estimatedSecondsRemaining / 60, estimatedSecondsRemaining % 60));
                                }
                            }
                        };

                        final ExecutorService executor = Executors.newSingleThreadExecutor();
                        final B2FileVersion file1;
                        final B2ContentSource source = B2FileContentSource.build(backupFile);
                        B2UploadFileRequest request = B2UploadFileRequest
                                .builder(bucketId, fileName, B2ContentTypes.B2_AUTO, source)
                                .setListener(uploadListener)
                                .build();
                        client.uploadLargeFile(request, executor);
                    } catch(Exception e) {
                        STEMCraft.error(e);
                    }
                }
            } catch(Exception e) {
                STEMCraft.error(e);
            } finally {
                inProgress = false;
                STEMCraft.broadcast("Backup complete");
            }
        });
    }

    public static void backup() {
        backup(true);
    }

    private static int recurseZip(File[] fileList, ZipOutputStream zos, String parentDir, int currentCount) throws IOException {
        Path baseDir = Paths.get(".").toAbsolutePath().normalize();
        for (File file : fileList) {
            Path relativePath = baseDir.relativize(file.toPath().toAbsolutePath());
            String relativePathString = relativePath.toString().replace('\\', '/');

            if(!exclusionMatchers.isEmpty()) {
                if (exclusionMatchers.stream().anyMatch(matcher -> matcher.matches(relativePath))) {
                    continue;
                }
            }

            if (file.isDirectory()) {
                currentCount = recurseZip(Objects.requireNonNull(file.listFiles()), zos, parentDir + file.getName() + "/", currentCount);
                continue;
            }

            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(parentDir + file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
            fis.close();

            currentCount++;
            if(currentCount % 50 == 0) {
                STEMCraft.info("Processed " + currentCount + " files...");
            }
        }

        return currentCount;
    }

    /**
     * Parse string into a time
     * @param timeStr The string to parse
     * @return A local time or null
     */
    private static LocalTime parseTime(String timeStr) {
        // Try parsing 24-hour format
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            // If 24-hour format fails, try 12-hour format
            try {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("h:mma"));
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}
