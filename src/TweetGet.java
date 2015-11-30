import static tweetBasic.AWSResourceSetup.DYNAMODB_MAPPER;
import static tweetBasic.AWSResourceSetup.SQS_QUEUE_NAME;
import static tweetBasic.AWSResourceSetup.SQS;

import java.util.Date;

import tweetBasic.Tweet;
import twitter4j.FilterQuery;
import twitter4j.Location;
import twitter4j.ResponseList;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.alchemyapi.api.*;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

/**
 * <p>This is a code example of Twitter4J Streaming API - sample method support.<br>
 * Usage: java twitter4j.examples.PrintSampleStream<br>
 * </p>
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public final class TweetGet {
	
    /**
     * Main entry of this application.
     *
     * @param args
     * @throws IOException 
     * @throws FileNotFoundException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws XPathExpressionException 
     */
    public static void main(String[] args) throws TwitterException, FileNotFoundException, IOException, XPathExpressionException, SAXException, ParserConfigurationException {
    	
		// String[] keywords = getTrends("New York");
    	
    	ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
           .setOAuthConsumerKey("xB127kA8Wn91LeZLJNHn7DQLD")
           .setOAuthConsumerSecret("XEAEaQBWlDirhoT8noQmbbwDhbBjvjzQcEJIO80eZXIWj6nTKn")
           .setOAuthAccessToken("605341080-dSZi4aYnavhiewli3oxRow2aLMpUdv58cqiM5Wmh")
           .setOAuthAccessTokenSecret("OIDy5WNOgzcvl1NnbJIneZWTSos3v9CfOeR0NMok8eLCt");
         
        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
        StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(Status status) {
                
                if (status.getGeoLocation() != null) {

                	long id=status.getId();
                    String strId=String.valueOf(id);
                    String username=status.getUser().getScreenName();
                    String content=status.getText();
                    String userLocation=status.getUser().getLocation();
                    double geoLat = 0;
                    double geoLng = 0;
                    Date createdAt=status.getCreatedAt();
                    
                    if(status.getGeoLocation() != null){
                    	geoLat=status.getGeoLocation().getLatitude();
                    	geoLng=status.getGeoLocation().getLongitude();
                    }
                    
					// Log tweet details. User - Date - (Lat, Long) - Content - Sentiment - Category.
                    System.out.println("[TweetGet] " + username
                    		+ " on " + createdAt.toString()
                    		+ " at (" + Math.round(geoLat) + ", " + Math.round(geoLng) + ")\n"
                    		+ content);

                    System.out.println("-------------------------------------------");
                    
                    // Save tweet to DynamoDB.
                    if (status.getGeoLocation() != null){
                    	Tweet t = new Tweet(strId, username, content, userLocation, geoLat, geoLng, createdAt);
                         t.saveTweetToDynamoDB();
                         
                         // Move all the work of alchemy to workers: after saving the basic
                         // content, send a message to SQS
                         
                         
                         /*
                          * Use Amazon SQS to send a message to the queue our worker processes
                          * are monitoring.
                          *
                          * Another option in the SDK for sending messages to a queue is the
                          * AmazonSQSBufferedAsyncClient. This client will buffer messages
                          * locally so that they are batched together in groups when they're
                          * sent. This means more efficient network communication with SQS
                          * because less individual requests with single messages are being sent.
                          * For high throughput applications this can not only help with
                          * throughput, but can also decrease your SQS costs because of the
                          * reduction in the amount of API calls. The tradeoff is that individual
                          * messages can be slightly delayed while a full batch is created on the
                          * client-side.
                          */
                         String queueUrl = SQS.getQueueUrl(new GetQueueUrlRequest(SQS_QUEUE_NAME)).getQueueUrl();
                         System.out.println("[TweetGet] send to sqs queue url: "+queueUrl);
                         SQS.sendMessage(new SendMessageRequest(queueUrl, t.getId()));
                         

                    }
                    
                }
                
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
               //System.out.println("[TweetGet] Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("[TweetGet] Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("[TweetGet] Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
                System.out.println("[TweetGet] Got stall warning:" + warning);
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
        twitterStream.addListener(listener);
        
        // FilterQuery qry = new FilterQuery();
        // qry.track(keywords);
        // twitterStream.filter(qry);
        
        twitterStream.sample();
    }
    
  
    private static Integer getTrendLocationId(String locationName) {

	    int idTrendLocation = 0;
	
	    try {
	
	    	ConfigurationBuilder cb = new ConfigurationBuilder();
	        cb.setDebugEnabled(true)
	           .setOAuthConsumerKey("xB127kA8Wn91LeZLJNHn7DQLD")
	           .setOAuthConsumerSecret("XEAEaQBWlDirhoT8noQmbbwDhbBjvjzQcEJIO80eZXIWj6nTKn")
	           .setOAuthAccessToken("605341080-dSZi4aYnavhiewli3oxRow2aLMpUdv58cqiM5Wmh")
	           .setOAuthAccessTokenSecret("OIDy5WNOgzcvl1NnbJIneZWTSos3v9CfOeR0NMok8eLCt");
	        
	        TwitterFactory tf = new TwitterFactory(cb.build());
	        Twitter twitter = tf.getInstance();
	
	        ResponseList<Location> locations;
	        locations = twitter.getAvailableTrends();
	        
	
	        for (Location location : locations) {
		        if (location.getName().toLowerCase().equals(locationName.toLowerCase())) {
		            idTrendLocation = location.getWoeid();
		            break;
		        }
	        }
	
	        if (idTrendLocation > 0) {
	        	return idTrendLocation;
	        }
	
	        return null;
	
	    } catch (TwitterException te) {
	        te.printStackTrace();
	        System.out.println("Failed to get trends: " + te.getMessage());
	        return null;
	    }

    }
    
    private static String[] getTrends(String trendLocation) {
	    try {
	    	
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true)
			   .setOAuthConsumerKey("xB127kA8Wn91LeZLJNHn7DQLD")
			   .setOAuthConsumerSecret("XEAEaQBWlDirhoT8noQmbbwDhbBjvjzQcEJIO80eZXIWj6nTKn")
			   .setOAuthAccessToken("605341080-dSZi4aYnavhiewli3oxRow2aLMpUdv58cqiM5Wmh")
			   .setOAuthAccessTokenSecret("OIDy5WNOgzcvl1NnbJIneZWTSos3v9CfOeR0NMok8eLCt");
	
			TwitterFactory tf = new TwitterFactory(cb.build());
	        Twitter twitter = tf.getInstance();
	
	        ResponseList<Location> locations;
	        locations = twitter.getAvailableTrends();
	
	        Integer idTrendLocation = getTrendLocationId(trendLocation);
	
	        if (idTrendLocation == null) {
	        	System.out.println("Trend Location Not Found");
	        	System.exit(0);
	        }
	
	        Trends trends = twitter.getPlaceTrends(idTrendLocation);
	        int numTrends = trends.getTrends().length;
	        String[] keywords = new String[numTrends];
	        
	        for (int i = 0; i < numTrends; i++) {
	        	String trend = trends.getTrends()[i].getName();
	        	keywords[i] = trend;
	        	System.out.println(trend);
	        }
	        	        
	        return keywords;
	
	    } catch (TwitterException te) {
	        te.printStackTrace();
	        System.out.println("Failed to get trends: " + te.getMessage());
	        System.exit(-1);
	        return null;
	    }
	    
    }
    
}