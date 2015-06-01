# JGroups native S3 ping

Native means, it uses the AWS SDK and does not implement the HTTP protocol on its own. The benefit is a more stable
connection as well as usage of IAM server profiles and AWS standardized credential distribution.

# Artifact
```xml
<dependency>
    <groupId>de.zalando</groupId>
    <artifactId>jgroups-native-s3-ping</artifactId>
    <version>0.1.0</version>
</dependency>
```
# Configuration

Like the original `S3_PING`, this library implement a JGroups `Discovery` protocol which replaces protocols like
`UNICAST` or `TCPPING`.

```xml
<de.zalando.jgroups.NATIVE_S3_PING
        regionName="eu-west-1"
        bucketName="jgroups-s3-test"
        bucketPrefix="jgroups"/>
```

## Possible Configurations

* **regionName**: like "eu-west-1", "us-east-1", etc.
* **bucketName**: the S3 bucket to store the files in
* **bucketPrefix** (optional): if you don't want the plugin to pollute your S3 bucket, you can configure a prefix like
  "jgroups/"
* **endpoint** (optional): you can override the S3 endpoint if yuo know what you are doing

## Example Configuration

```xml
<!--
Based on tcp.xml but with new S3_PING.
-->
<config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:org:jgroups"
        xsi:schemaLocation="urn:org:jgroups http://www.jgroups.org/schema/jgroups.xsd">
    <TCP bind_port="7800"
         recv_buf_size="${tcp.recv_buf_size:5M}"
         send_buf_size="${tcp.send_buf_size:5M}"
         max_bundle_size="64K"
         max_bundle_timeout="30"
         use_send_queues="true"
         sock_conn_timeout="300"
         timer_type="new3"
         timer.min_threads="4"
         timer.max_threads="10"
         timer.keep_alive_time="3000"
         timer.queue_max_size="500"
         thread_pool.enabled="true"
         thread_pool.min_threads="2"
         thread_pool.max_threads="8"
         thread_pool.keep_alive_time="5000"
         thread_pool.queue_enabled="true"
         thread_pool.queue_max_size="10000"
         thread_pool.rejection_policy="discard"
         oob_thread_pool.enabled="true"
         oob_thread_pool.min_threads="1"
         oob_thread_pool.max_threads="8"
         oob_thread_pool.keep_alive_time="5000"
         oob_thread_pool.queue_enabled="false"
         oob_thread_pool.queue_max_size="100"
         oob_thread_pool.rejection_policy="discard"/>

    <de.zalando.jgroups.NATIVE_S3_PING
            regionName="eu-west-1"
            bucketName="jgroups-s3-test"
            bucketPrefix="jgroups"/>

    <MERGE3 min_interval="10000"
            max_interval="30000"/>

    <FD_SOCK/>
    <FD timeout="3000" max_tries="3"/>
    <VERIFY_SUSPECT timeout="1500"/>
    <BARRIER/>

    <pbcast.NAKACK2 use_mcast_xmit="false"
                    discard_delivered_msgs="true"/>

    <UNICAST3/>

    <pbcast.STABLE stability_delay="1000" desired_avg_gossip="50000"
                   max_bytes="4M"/>
    <pbcast.GMS print_local_addr="true" join_timeout="2000"
                view_bundling="true"/>
    <MFC max_credits="2M"
         min_threshold="0.4"/>
    <FRAG2 frag_size="60K"/>
    <!--RSVP resend_interval="2000" timeout="10000"/-->
    <pbcast.STATE_TRANSFER/>
</config>
```

# License

    Copyright 2015 Zalando SE

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
