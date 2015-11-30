import twitter4j.Location;
import twitter4j.ResponseList;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public final class TrendsGet {

    public static void main(String[] args) {

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
	
	        Integer idTrendLocation = getTrendLocationId("New York");
	
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
	        	        	        
	        System.exit(0);
	
	    } catch (TwitterException te) {
	        te.printStackTrace();
	        System.out.println("Failed to get trends: " + te.getMessage());
	        System.exit(-1);
	    }
    }

	// Influenced by http://stackoverflow.com/questions/10431321/using-twitter4j-daily-trends
    
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

