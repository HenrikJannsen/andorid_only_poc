package bisq.app;


import java.util.concurrent.CompletableFuture;

import bisq.common.platform.MemoryReportService;

public class AndroidMemoryReport implements MemoryReportService {
    public AndroidMemoryReport() {
    }

    @Override
    public void logReport() {
    }

    @Override
    public long getUsedMemoryInBytes() {
        return 0;
    }

    @Override
    public long getUsedMemoryInMB() {
        return 0;
    }

    @Override
    public long getFreeMemoryInMB() {
        return 0;
    }

    @Override
    public long getTotalMemoryInMB() {
        return 0;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return null;
    }
}
