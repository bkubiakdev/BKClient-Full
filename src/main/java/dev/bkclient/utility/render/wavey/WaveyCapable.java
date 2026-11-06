package dev.bkclient.utility.render.wavey;

public interface WaveyCapable {
    WaveySimulation bkclient$getWaveySimulation();
    void bkclient$setWaveySimulation(WaveySimulation sim);
    boolean bkclient$isWaveyDirty();
    void bkclient$setWaveyDirty(boolean dirty);
}
