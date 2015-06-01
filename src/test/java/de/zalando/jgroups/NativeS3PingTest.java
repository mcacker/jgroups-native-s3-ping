package de.zalando.jgroups;

import org.jgroups.JChannel;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NativeS3PingTest {
    private static final int MEMBER_COUNT = 2;

    @Test
    public void findMembersTest() {

        final List<ClusterMember> members = new ArrayList<ClusterMember>();
        for (int n = 0; n < MEMBER_COUNT; n++) {
            members.add(new ClusterMember("findMembersTest-" + Instant.now().getEpochSecond()));
        }

        for (final ClusterMember member: members) {
            member.start();
        }

        final JChannel channel = members.get(members.size() - 1).getChannel();

        Assert.assertEquals("member count", members.size(), channel.getView().getMembers().size());

        for (final ClusterMember member: members) {
            member.stop();
        }
    }
}
