package de.zalando.jgroups;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.jgroups.Address;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.FILE_PING;
import org.jgroups.protocols.PingData;
import org.jgroups.util.Responses;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Created after the original S3_PING from Bela Ban.
 *
 * This implementation uses the AWS SDK in order to be more solid and to benefit from the built-in security features
 * like getting credentials via IAM instance profiles instead of handling this in the application.
 *
 * @author Tobias Sarnowski
 */
public class NATIVE_S3_PING extends FILE_PING {
    private static final short JGROUPS_PROTOCOL_DEFAULT_MAGIC_NUMBER = 789;

    private static final int SERIALIZATION_BUFFER_SIZE = 4096;
    private static final String SERIALIZED_CONTENT_TYPE = "text/plain";

    @Property(description = "The S3 endpoint to use (optional).", exposeAsManagedAttribute = false)
    protected String endpoint;

    @Property(description = "The S3 region to use.", exposeAsManagedAttribute = false)
    protected String regionName;

    @Property(description = "The S3 bucket to use.", exposeAsManagedAttribute = false)
    protected String bucketName;

    @Property(description = "The S3 bucket prefix to use (optional e.g. 'jgroups/').", exposeAsManagedAttribute = false)
    protected String bucketPrefix;

    private AmazonS3 s3;

    @Override
    public void init() throws Exception {
        super.init();

        if (bucketPrefix == null || bucketPrefix.equals("/")) {
            bucketPrefix = "";
        } else if (!bucketPrefix.endsWith("/") && !bucketPrefix.isEmpty()) {
            bucketPrefix = bucketPrefix + "/";
        }

        s3 = new AmazonS3Client();

        if (endpoint != null) {
            s3.setEndpoint(endpoint);
            log.info("set Amazon S3 endpoint to %s", endpoint);
        }

        final Region region = Region.getRegion(Regions.fromName(regionName));
        s3.setRegion(region);

        log.info("using Amazon S3 ping in region %s with bucket '%s' and prefix '%s'", region, bucketName, bucketPrefix);
    }

    @Override
    protected void createRootDir() {
        // ignore, bucket has to exist
    }

    private String getClusterPrefix(final String clusterName) {
        return bucketPrefix + clusterName + "/";
    }

    @Override
    protected void readAll(final List<Address> members, final String clustername, final Responses responses) {
        if (clustername == null) {
            return;
        }

        final String clusterPrefix = getClusterPrefix(clustername);

        try {
            final ObjectListing objectListing = s3.listObjects(
                    new ListObjectsRequest()
                            .withBucketName(bucketName)
                            .withPrefix(clusterPrefix));

            // TODO batching not supported; can result in wrong lists if bucket has too many entries

            for (final S3ObjectSummary summary : objectListing.getObjectSummaries()) {
                final S3Object object = s3.getObject(new GetObjectRequest(summary.getBucketName(), summary.getKey()));

                final List<PingData> data = read(object.getObjectContent());
                // TODO currently always returns null
                if (data == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("fetched update for member list in Amazon S3 is empty [%s]", clusterPrefix);
                    }
                    return;
                }
                for (final PingData pingData : data) {
                    if (members == null || members.contains(pingData.getAddress())) {
                        responses.addResponse(pingData, pingData.isCoord());
                    }
                    if (local_addr != null && !local_addr.equals(pingData.getAddress())) {
                        addDiscoveryResponseToCaches(pingData.getAddress(), pingData.getLogicalName(),
                                pingData.getPhysicalAddr());
                    }

                    if (log.isTraceEnabled()) {
                        log.trace("processed entry in Amazon S3 [%s -> %s]", summary.getKey(), pingData);
                    }
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("fetched update for member list in Amazon S3 [%s]", clusterPrefix);
            }

        } catch (final Exception e) {
            log.error(String.format("failed getting member list from Amazon S3 [%s]", clusterPrefix), e);
        }
    }

    @Override
    protected void write(final List<PingData> list, final String clustername) {
        final String filename = addressToFilename(local_addr);
        final String key = getClusterPrefix(clustername) + filename;

        try {
            final ByteArrayOutputStream outStream = new ByteArrayOutputStream(SERIALIZATION_BUFFER_SIZE);
            write(list, outStream);

            final byte[] data = outStream.toByteArray();

            final ByteArrayInputStream inStream = new ByteArrayInputStream(data);
            final ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(SERIALIZED_CONTENT_TYPE);
            objectMetadata.setContentLength(data.length);

            s3.putObject(new PutObjectRequest(bucketName, key, inStream, objectMetadata));

            if (log.isDebugEnabled()) {
                log.debug("wrote member list to Amazon S3 [%s -> %s]", key, list);
            }

        } catch (final Exception e) {
            log.error(String.format("failed to update member list in Amazon S3 [%s]", key), e);
        }
    }

    @Override
    protected void remove(final String clustername, final Address addr) {
        if (clustername == null || addr == null) {
            return;
        }

        final String filename = addressToFilename(local_addr);
        final String key = getClusterPrefix(clustername) + filename;

        try {
            s3.deleteObject(new DeleteObjectRequest(bucketName, key));

            if (log.isDebugEnabled()) {
                log.debug("deleted member from member list list in Amazon S3 [%s]", key);
            }

        } catch (final Exception e) {
            log.error(String.format("failed to remove member from member list in Amazon S3 [%s]", key), e);
        }
    }

    public static void registerProtocolWithJGroups() {
        registerProtocolWithJGroups(JGROUPS_PROTOCOL_DEFAULT_MAGIC_NUMBER);
    }

    public static void registerProtocolWithJGroups(short magicNumber) {
        ClassConfigurator.addProtocol(magicNumber, NATIVE_S3_PING.class);
    }
}
