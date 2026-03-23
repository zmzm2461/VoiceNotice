package com.example.voicenotice.domain.audio;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Chunk 조립 서비스
@Service
public class ChunkAssembleService {

    private final Path baseDir;
    private final ConcurrentHashMap<String, UploadState> states = new ConcurrentHashMap<>();

    public ChunkAssembleService() throws Exception {
        this.baseDir = Path.of("storage", "chunks");
        Files.createDirectories(baseDir);
    }

    public UploadState getState(String uploadId) {
        return states.get(uploadId);
    }

    public Path getTempPath(String uploadId) {
        return baseDir.resolve(uploadId + ".wav.part");
    }

    public Path getFinalPath(String uploadId) {
        return baseDir.resolve(uploadId + ".wav");
    }

    public AppendResult append(
            String uploadId,
            String deviceUid,
            int chunkIndex,
            int totalChunks,
            byte[] chunkBytes
    ) throws Exception {

        UploadState state = states.computeIfAbsent(uploadId, id -> new UploadState(deviceUid, totalChunks));

        // 이미 조립 완료된 uploadId면 중복 업로드로 보고 DUPLICATE 처리
        if (state.assembled) {
            return new AppendResult("DUPLICATE", state.nextChunkIndex);
        }

        // 1) 이미 받은 chunk 재전송(중복) → 그냥 OK로 처리
        if (chunkIndex < state.nextChunkIndex) {
            // 서버는 다음으로 필요한 인덱스를 알려줌
            return new AppendResult("DUPLICATE", state.nextChunkIndex);
        }

        // 2) 아직 안 받은 미래 chunk(순서 꼬임) → OUT_OF_ORDER
        if (chunkIndex > state.nextChunkIndex) {
            return new AppendResult("OUT_OF_ORDER", state.nextChunkIndex);
        }

        // 3) chunkIndex == nextChunkIndex → 정상 append
        Path temp = getTempPath(uploadId);

        try (OutputStream os = Files.newOutputStream(temp, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            os.write(chunkBytes);
        }

        state.nextChunkIndex++;
        state.lastUpdatedEpochMs = Instant.now().toEpochMilli();

        // 마지막 chunk까지 다 받았으면 final로 rename
        if (state.nextChunkIndex >= totalChunks) {
            Path finalWav = getFinalPath(uploadId);
            Files.move(temp, finalWav, StandardCopyOption.REPLACE_EXISTING);
            state.assembled = true;

            return new AppendResult("ASSEMBLED", state.nextChunkIndex);
        }

        return new AppendResult("RECEIVED", state.nextChunkIndex);
    }

    // 메모리 상태
    public static class UploadState {
        public final String deviceUid;
        public final int totalChunks;
        public volatile int nextChunkIndex = 0;
        public volatile boolean assembled = false;
        public volatile long lastUpdatedEpochMs = Instant.now().toEpochMilli();

        public UploadState(String deviceUid, int totalChunks) {
            this.deviceUid = deviceUid;
            this.totalChunks = totalChunks;
        }
    }

    public static class AppendResult {
        public final String status;
        public final int nextChunkIndex;

        public AppendResult(String status, int nextChunkIndex) {
            this.status = status;
            this.nextChunkIndex = nextChunkIndex;
        }
    }
}
