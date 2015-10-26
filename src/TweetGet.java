import java.util.Date;

import tweetBasic.Tweet;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.alchemyapi.api.*;

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
	
	static AlchemyAPI alchemyObj;
	
	static{
		try {
			alchemyObj = AlchemyAPI.GetInstanceFromFile("api_key.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
    	
    	ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
           .setOAuthConsumerKey("ZuBFtIjHubaHmaomVCN6HNRI5")
           .setOAuthConsumerSecret("Ll0LZgKPly3QvIxIYMhtAxwxeUkleHc9Xya1Q5zAPxaga2wIpD")
           .setOAuthAccessToken("3095412628-4kLyHeZWV3p4Swmqx0d2lGSfJbtNqPbl0VPuMta")
           .setOAuthAccessTokenSecret("bKdTWWUVrtg1WtTog65t2XscxdvNbHszxDQLHBpZkutIG");
         
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
                    
                    // Extract sentiment for a text string.
                    Document doc;
                    String category = "";
                    String sentiment = "";
    				try {
    					String text = status.getText();
    					doc = alchemyObj.TextGetTextSentiment(text);
    					sentiment = getSentiment(doc);
    					doc = alchemyObj.TextGetCategory(text);
    					category = getCategory(doc);
    				} catch (XPathExpressionException e) {
    				} catch (IOException e) {
    				} catch (SAXException e) {
    				} catch (ParserConfigurationException e) {
    				}


					// Log tweet details. User - Date - (Lat, Long) - Content - Sentiment - Category.
                    System.out.println(username
                    		+ " on " + createdAt.toString()
                    		+ " at (" + Math.round(geoLat) + ", " + Math.round(geoLng) + ")\n"
                    		+ content);
                    if (sentiment.length() != 0 && category.length() != 0) {
                    	System.out.println("sentiment: " + sentiment + "; " + "category: " + category);
                    } else {
                    	System.out.println("no sentiment; no category");
                    }

                    System.out.println("-------------------------------------------");
                    
                    // Save tweet to DynamoDB.
                    Tweet t = new Tweet(strId, username, content, userLocation, geoLat, geoLng, createdAt);
                    t.setSentiment(sentiment);
                    t.setCategory(category);
                    t.saveTweetToDynamoDB();
                }
                
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
//                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
                System.out.println("Got stall warning:" + warning);
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
        twitterStream.addListener(listener);
        twitterStream.sample();
    }
    
    private static String getSentiment(Document doc) {
    	Element parent = (Element) doc.getElementsByTagName("docSentiment").item(0);
    	NodeList childList = parent.getElementsByTagName("type");
    	String sentiment = childList.item(0).getTextContent();
    	return sentiment;
    }
    
    private static String getCategory(Document doc) {
    	String category = doc.getElementsByTagName("category").item(0).getTextContent();
    	return category;
    }
}