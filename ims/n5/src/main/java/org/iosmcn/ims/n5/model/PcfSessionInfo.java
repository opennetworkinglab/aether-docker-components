package org.iosmcn.ims.n5.model;

public class PcfSessionInfo {

    private String audioSessionLocation;
    private String videoSessionLocation;

    public String getAudioSessionLocation() {
        return audioSessionLocation;
    }

    public void setAudioSessionLocation(String audioSessionLocation) {
        this.audioSessionLocation = audioSessionLocation;
    }

    public String getVideoSessionLocation() {
        return videoSessionLocation;
    }

    public void setVideoSessionLocation(String videoSessionLocation) {
        this.videoSessionLocation = videoSessionLocation;
    }

    public boolean hasAnySession() {
        return audioSessionLocation != null || videoSessionLocation != null;
    }
}
