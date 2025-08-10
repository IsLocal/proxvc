package fiveavian.proxvc.api;

import fiveavian.proxvc.ProxVCClient;
import fiveavian.proxvc.ProxVCServer;
import fiveavian.proxvc.vc.StreamingAudioSource;

import java.nio.ByteBuffer;

public interface ProxvcPlugin {
    void onClientStart(ProxVCClient client);

    void onSetupSourceALContext(int alSource, StreamingAudioSource source);
    void onSourceQueueAudio(StreamingAudioSource source, ByteBuffer samples);

    void onSourceClose(StreamingAudioSource source);
    void onProxvcStop(ProxVCClient client);

    String getName();
    String getVersion();
    String getAuthor();
    String getDescription();



}
