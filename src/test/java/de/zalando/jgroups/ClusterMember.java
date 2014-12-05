package de.zalando.jgroups;

import org.jgroups.JChannel;

public final class ClusterMember {
    private final String clusterName;

    private JChannel channel;

    public ClusterMember(final String clusterName) {
        this.clusterName = clusterName;
    }

    public JChannel getChannel() {
        if (channel == null) throw new IllegalStateException("cluster member not started");

        return channel;
    }

    public void start() {
        if (channel != null) throw new IllegalStateException("cluster member already started: " + channel);

        try {
            channel = new JChannel(this.getClass().getResource("/jgroups-native-s3.xml"));
            channel.connect(clusterName);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void stop() {
        if (channel == null) throw new IllegalStateException("cluster member not started");

        channel.close();
        channel = null;
    }
}
