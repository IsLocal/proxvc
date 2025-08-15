package fiveavian.proxvc.api;

import fiveavian.proxvc.ProxVCClient;
import fiveavian.proxvc.vc.StreamingAudioSource;

import java.nio.ByteBuffer;

public class ExamplePlugin implements ProxVCPlugin {
    @Override
    public void registerClientEvents(ClientEvents clientEvents) {

    }

    @Override
    public void onClientStart(ProxVCClient client) {

    }

    @Override
    public void onSetupSourceALContext(int alSource, StreamingAudioSource source) {

    }

    @Override
    public void onSourceQueueSamples(StreamingAudioSource source, ByteBuffer samples) {

    }

    @Override
    public void onSourceClose(StreamingAudioSource source) {

    }

    @Override
    public void onProxvcStop(ProxVCClient client) {

    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public String getAuthor() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }
}
