package tweetBasic;


import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.util.Tables;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;


public class AWSResourceSetup {

	  	public static final String S3_BUCKET_NAME = "video-broadcast-mw2907";
	    public static final String DYNAMODB_TABLE_NAME = "tweet-mw2907";

	    public static final String MAIN_TOPIC_PATH = "topics/";
	    public static final String FOLLOWING_PATH = "follows/";
	    
	    public static final String MAIN_TOPIC     = "topic";
	    public static final String FOLLOWING = "following";

	    /*
	     * The SDK provides several easy to use credentials providers.
	     * Here we're loading our AWS security credentials from a properties
	     * file on our classpath.
	     */
	    public static final AWSCredentialsProvider CREDENTIALS_PROVIDER =
	            new ClasspathPropertiesFileCredentialsProvider();

	    /*
	     * This controls the AWS region used for created resources. You can easily
	     * deploy applications in any or all of the AWS regions around the world,
	     * allowing you to provide a lower latency and better experience for your
	     * customers.
	     */
	    public static final Region REGION = Region.getRegion(Regions.US_EAST_1);

	    /*
	     * We construct our clients to access AWS here, so that we can share them
	     * easily throughout our application.
	     */
	    public static final AmazonS3Client S3 = new AmazonS3Client(CREDENTIALS_PROVIDER);
	    public static final AmazonDynamoDBClient DYNAMODB = new AmazonDynamoDBClient(CREDENTIALS_PROVIDER);
	    public static final DynamoDBMapper DYNAMODB_MAPPER = new DynamoDBMapper(DYNAMODB, CREDENTIALS_PROVIDER);


	    static {
	        /*
	         * Set any other client options that you need here. For example, if you
	         * connect to the internet through a proxy, then call setConfiguration
	         * and pass in a ClientConfiguration object with your proxy settings.
	         *
	         * Here we set our region, so that we can keep our data located in the
	         * same region.
	         */
	        DYNAMODB.setRegion(REGION);
	  
	    }


	    public static void main(String[] args) {
	        
	        if ( !S3.doesBucketExist(S3_BUCKET_NAME) ) {
	            S3.createBucket(new CreateBucketRequest(S3_BUCKET_NAME));
	        }
	        System.out.println("Using Amazon S3 Bucket: " + S3_BUCKET_NAME);


	        if ( !Tables.doesTableExist(DYNAMODB, DYNAMODB_TABLE_NAME) ) {
	            System.out.println("Creating new AWS DynamoDB Table...");
	            DYNAMODB.createTable(new CreateTableRequest()
	                    .withTableName(DYNAMODB_TABLE_NAME)
	                    .withKeySchema(new KeySchemaElement("id", KeyType.HASH))
	                    .withAttributeDefinitions(new AttributeDefinition("id", ScalarAttributeType.S))
	                    .withProvisionedThroughput(new ProvisionedThroughput(50l, 50l)));
	        }
	        Tables.waitForTableToBecomeActive(DYNAMODB, DYNAMODB_TABLE_NAME);
	        System.out.println("Using AWS DynamoDB Table: " + DYNAMODB_TABLE_NAME);
	    }
}
